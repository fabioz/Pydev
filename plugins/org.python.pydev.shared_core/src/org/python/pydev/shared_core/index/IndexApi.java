/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.partitioner.IContentsScanner;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_core.utils.Timer;

public class IndexApi {

    public static final boolean DEBUG = false;

    private final Directory indexDir;
    private IndexWriter writer;
    private SearcherManager searchManager;
    private SearcherFactory searcherFactory;
    private int maxMatches = Integer.MAX_VALUE;
    private CodeAnalyzer analyzer;
    private final Object lock = new Object();

    public IndexApi(Directory indexDir, boolean applyAllDeletes) throws IOException {
        this.indexDir = indexDir;
        init(applyAllDeletes);
    }

    /**
     * @return an object which external users can use to synchronize on this lock. Note that
     * the methods in the API aren't synchronized (so, if more than one thread can use it in
     * the use-case, this lock should be used for synchronization).
     */
    public Object getLock() {
        return lock;
    }

    public IndexApi(File indexDir, boolean applyAllDeletes) throws IOException {
        this(FSDirectory.open(indexDir.toPath()), applyAllDeletes);
    }

    public void init(boolean applyAllDeletes) throws IOException {
        this.analyzer = new CodeAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setCommitOnClose(true);
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        try {
            writer = new IndexWriter(this.indexDir, config);
        } catch (IOException e) {
            config.setOpenMode(OpenMode.CREATE);
            writer = new IndexWriter(this.indexDir, config);
        }

        searcherFactory = new SearcherFactory();
        searchManager = new SearcherManager(writer, applyAllDeletes, searcherFactory);
    }

    public void registerTokenizer(String fieldName, TokenStreamComponents tokenStream) {
        this.analyzer.registerTokenizer(fieldName, tokenStream);
    }

    public void commit() throws IOException {
        if (this.writer != null) {
            this.writer.commit();
        }
    }

    public void dispose() {
        if (this.writer != null) {
            try {
                this.writer.commit();
            } catch (IOException e) {
                Log.log(e);
            }
            try {
                this.writer.close();
            } catch (Exception e) {
                Log.log(e);
            }
            this.writer = null;
        }

        if (this.searchManager != null) {
            try {
                this.searchManager.close();
            } catch (Exception e) {
                Log.log(e);
            }
            this.searchManager = null;
        }
    }

    private Document createDocument(Map<String, String> fieldsToIndex) {
        Document doc = new Document();

        Set<Entry<String, String>> entrySet = fieldsToIndex.entrySet();
        for (Entry<String, String> entry : entrySet) {
            doc.add(new StringField(entry.getKey(), entry.getValue(), Field.Store.YES));
        }

        return doc;
    }

    private Document createDocument(IPath filepath, long modifiedTime, Map<String, String> additionalStringFields) {
        Document doc = new Document();

        doc.add(new StringField(IFields.FILEPATH, filepath.toPortableString(), Field.Store.YES)); // StringField is not analyzed
        doc.add(new StringField(IFields.MODIFIED_TIME, String.valueOf(modifiedTime), Field.Store.YES));

        String lastSegment = filepath.removeFileExtension().lastSegment();
        if (lastSegment == null) {
            lastSegment = "";
        }
        doc.add(new StringField(IFields.FILENAME, lastSegment, Field.Store.YES)); // StringField is not analyzed
        String fileExtension = filepath.getFileExtension();
        if (fileExtension == null) {
            fileExtension = "";
        }

        if (additionalStringFields != null) {
            Set<Entry<String, String>> entrySet = additionalStringFields.entrySet();
            for (Entry<String, String> entry : entrySet) {
                doc.add(new StringField(entry.getKey(), entry.getValue(), Field.Store.YES));
            }
        }

        doc.add(new StringField(IFields.EXTENSION, fileExtension, Field.Store.YES)); // StringField is not analyzed
        return doc;
    }

    public void index(Path filepath, long modifiedTime, String general) throws IOException {
        this.index(filepath, modifiedTime, general, null);
    }

    public void index(Path filepath, long modifiedTime, String general, Map<String, String> additionalStringFields)
            throws IOException {
        this.index(filepath, modifiedTime, general, IFields.GENERAL_CONTENTS, additionalStringFields);
    }

    public void index(Path filepath, long modifiedTime, String general, String fieldName,
            Map<String, String> additionalStringFields) throws IOException {
        if (this.writer == null) {
            return;
        }
        Document doc = createDocument(filepath, modifiedTime, additionalStringFields);

        //Note: TextField should be analyzed/normalized in Analyzer.createComponents(String)
        doc.add(new TextField(fieldName, general, Field.Store.NO));

        this.writer.addDocument(doc);
    }

    public void index(Map<String, String> fieldsToIndex, Reader reader, String fieldName) throws IOException {
        if (this.writer == null) {
            return;
        }
        Document doc = createDocument(fieldsToIndex);

        //Note: TextField should be analyzed/normalized in Analyzer.createComponents(String)
        doc.add(new TextField(fieldName, reader));

        this.writer.addDocument(doc);
    }

    public void index(IPath filepath, long modifiedTime, Reader reader, String fieldName) throws IOException {
        if (this.writer == null) {
            return;
        }
        Document doc = createDocument(filepath, modifiedTime, null);

        //Note: TextField should be analyzed/normalized in Analyzer.createComponents(String)
        doc.add(new TextField(fieldName, reader));

        this.writer.addDocument(doc);
    }

    /**
     * We index based on what we want to search later on!
     *
     * We have to index giving the path for the file (workspace-relative path).
     *
     * The project is not expected to be passed because the idea is having one index
     * for each project.
     *
     * The scanner and the mapper work together: the scanner generates the tokens
     * and the mapper maps the token from the scanner to the mapping used for indexing.
     */
    public void index(Path filepath, long modifiedTime, ITokenScanner tokenScanner, IFields mapper)
            throws IOException {
        if (this.writer == null) {
            return;
        }
        IContentsScanner contentsScanner = (IContentsScanner) tokenScanner;
        Document doc = createDocument(filepath, modifiedTime, null);

        FastStringBuffer buf = new FastStringBuffer();
        IToken nextToken = tokenScanner.nextToken();
        while (!nextToken.isEOF()) {
            if (!nextToken.isUndefined() && !nextToken.isWhitespace()) {
                int offset = tokenScanner.getTokenOffset();
                int length = tokenScanner.getTokenLength();
                contentsScanner.getContents(offset, length, buf.clear());
                String fieldName = mapper.getTokenFieldName(nextToken);
                if (fieldName != null) {
                    //Note: TextField should be analyzed/normalized in Analyzer.createComponents(String)
                    doc.add(new TextField(fieldName, buf.toString(), Field.Store.NO));
                }
            }
            nextToken = tokenScanner.nextToken();
        }

        this.writer.addDocument(doc);
    }

    public SearchResult searchExact(String string, String fieldName, boolean applyAllDeletes) throws IOException {
        return searchExact(string, fieldName, applyAllDeletes, null);
    }

    public SearchResult searchExact(String string, String fieldName, boolean applyAllDeletes, IDocumentsVisitor visitor,
            String... fieldsToLoad)
                    throws IOException {
        Query query = new TermQuery(new Term(fieldName, string));
        return search(query, applyAllDeletes, visitor, fieldsToLoad);
    }

    public SearchResult searchWildcard(Set<String> string, String fieldName, boolean applyAllDeletes,
            IDocumentsVisitor visitor, Map<String, String> translateFields, String... fieldsToLoad)
                    throws IOException {
        OrderedMap<String, Set<String>> fieldNameToValues = new OrderedMap<>();
        fieldNameToValues.put(fieldName, string);
        return searchWildcard(fieldNameToValues, applyAllDeletes, visitor, translateFields, fieldsToLoad);
    }

    /**
     * Search where we return if any of the given strings appear.
     *
     * Accepts wildcard in queries
     */
    public SearchResult searchWildcard(OrderedMap<String, Set<String>> fieldNameToValues, boolean applyAllDeletes,
            IDocumentsVisitor visitor, Map<String, String> translateFields, String... fieldsToLoad)
                    throws IOException {
        BooleanQuery booleanQuery = new BooleanQuery();
        Set<Entry<String, Set<String>>> entrySet = fieldNameToValues.entrySet();
        for (Entry<String, Set<String>> entry : entrySet) {
            BooleanQuery fieldQuery = new BooleanQuery();
            String fieldName = entry.getKey();
            if (translateFields != null) {
                String newFieldName = translateFields.get(fieldName);
                if (newFieldName != null) {
                    fieldName = newFieldName;
                }
            }
            boolean allNegate = true;
            for (String s : entry.getValue()) {
                if (s.length() == 0) {
                    throw new RuntimeException("Unable to create term for searching empty string.");
                }
                boolean negate = false;
                if (s.startsWith("!")) {
                    // Negation if dealing with paths
                    if (IFields.FIELDS_NEGATED_WITH_EXCLAMATION.contains(fieldName)) {
                        s = s.substring(1);
                        negate = true;
                    }
                }
                if (s.length() == 0) {
                    // Only a single '!' for the negate.
                    continue;
                }
                if (s.indexOf('*') != -1 || s.indexOf('?') != -1) {
                    if (StringUtils.containsOnlyWildCards(s)) {
                        throw new RuntimeException("Unable to create term for searching only wildcards: " + s);
                    }
                    fieldQuery.add(new WildcardQuery(new Term(fieldName, s)),
                            negate ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.SHOULD);

                } else {
                    fieldQuery.add(new TermQuery(new Term(fieldName, s)),
                            negate ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.SHOULD);
                }
                if (!negate) {
                    allNegate = false;
                }
            }

            if (fieldQuery.getClauses().length != 0) {
                if (allNegate) {
                    // If all are negations, we actually have to add one which would
                    // match all to remove the negations.
                    fieldQuery.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
                }
                booleanQuery.add(fieldQuery, BooleanClause.Occur.MUST);
            }
        }

        if (DEBUG) {
            System.out.println("Searching: " + booleanQuery);
        }
        return search(booleanQuery, applyAllDeletes, visitor, fieldsToLoad);
    }

    public SearchResult searchRegexp(String string, String fieldName, boolean applyAllDeletes) throws IOException {
        return searchRegexp(string, fieldName, applyAllDeletes, null);
    }

    public SearchResult searchRegexp(String string, String fieldName,
            boolean applyAllDeletes, IDocumentsVisitor visitor, String... fieldsToLoad) throws IOException {
        Query query = new RegexpQuery(new Term(fieldName, string));
        return search(query, applyAllDeletes, visitor, fieldsToLoad);
    }

    public static class DocumentInfo {

        private Document document;
        private int documentId;

        public DocumentInfo(Document document, int doc) {
            this.document = document;
            this.documentId = doc;
        }

        public String get(String field) {
            return this.document.get(field);
        }

        public int getDocId() {
            return this.documentId;
        }

    }

    public static interface IDocumentsVisitor {

        void visit(DocumentInfo documentInfo);

    }

    /**
     * @param fields the fields to be loaded.
     */
    public void visitAllDocs(IDocumentsVisitor visitor, String... fields) throws IOException {
        boolean applyAllDeletes = true;
        try (IndexReader reader = DirectoryReader.open(writer, applyAllDeletes);) {

            IndexSearcher searcher = searcherFactory.newSearcher(reader, null);
            Query query = new MatchAllDocsQuery();
            TopDocs docs = searcher.search(query, Integer.MAX_VALUE);
            ScoreDoc[] scoreDocs = docs.scoreDocs;
            int length = scoreDocs.length;
            for (int i = 0; i < length; i++) {
                ScoreDoc scoreDoc = scoreDocs[i];
                DocumentStoredFieldVisitor fieldVisitor = new DocumentStoredFieldVisitor(fields);
                reader.document(scoreDoc.doc, fieldVisitor);
                Document document = fieldVisitor.getDocument();
                visitor.visit(new DocumentInfo(document, scoreDoc.doc));
            }
        }
    }

    public SearchResult search(Query query, boolean applyAllDeletes, IDocumentsVisitor visitor, String... fields)
            throws IOException {
        try {
            this.writer.commit();
        } catch (Exception e) {
            Log.log(e);
        }
        try (IndexReader reader = DirectoryReader.open(writer, applyAllDeletes);) {
            IndexSearcher searcher = searcherFactory.newSearcher(reader, null);

            TopDocs search = searcher.search(query, maxMatches);
            ScoreDoc[] scoreDocs = search.scoreDocs;

            if (visitor != null) {
                int length = scoreDocs.length;
                for (int i = 0; i < length; i++) {
                    ScoreDoc scoreDoc = scoreDocs[i];
                    DocumentStoredFieldVisitor fieldVisitor = new DocumentStoredFieldVisitor(fields);
                    reader.document(scoreDoc.doc, fieldVisitor);
                    Document document = fieldVisitor.getDocument();
                    visitor.visit(new DocumentInfo(document, scoreDoc.doc));
                }
            }

            return new SearchResult(scoreDocs);
        }
    }

    public void removeDocs(Map<String, Collection<String>> fieldToValuesToRemove) throws IOException {
        int total = 0;
        Set<Entry<String, Collection<String>>> entrySet = fieldToValuesToRemove.entrySet();
        for (Entry<String, Collection<String>> entry : entrySet) {
            total += entry.getValue().size();
        }
        if (total == 0) {
            return;
        }
        ArrayList<Term> lst = new ArrayList<>(total);
        for (Entry<String, Collection<String>> entry : entrySet) {
            String fieldName = entry.getKey();
            for (String string : entry.getValue()) {
                lst.add(new Term(fieldName, string));
            }
        }

        Term[] queries = lst.toArray(new Term[0]);
        this.writer.deleteDocuments(queries);
    }

    public void setMaxMatches(int maxMatches) {
        this.maxMatches = maxMatches;
    }

    public int getMaxMatches() {
        return maxMatches;
    }

    public static void main(String[] args) throws IOException {
        File f = new File("x:\\index");
        final IndexApi indexApi = new IndexApi(f, true);

        ICallback<Object, java.nio.file.Path> onFile = new ICallback<Object, java.nio.file.Path>() {

            @Override
            public Object call(java.nio.file.Path path) {
                String string = path.toString();
                if (string.endsWith(".py")) {
                    try (SeekableByteChannel sbc = Files.newByteChannel(path);
                            InputStream in = Channels.newInputStream(sbc)) {
                        Reader reader = new BufferedReader(new InputStreamReader(in));
                        IPath path2 = Path.fromOSString(string);
                        indexApi.index(path2, FileUtils.lastModified(path.toFile()),
                                reader, IFields.GENERAL_CONTENTS);
                    } catch (Exception e) {
                        Log.log("Error parsing: " + path, e);
                    }
                }

                return null;
            }
        };
        Timer timer = new Timer();
        //        FileUtils.visitDirectory(new File("x:\\etk"), true, onFile);
        // indexApi.commit();
        indexApi.setMaxMatches(Integer.MAX_VALUE);
        SearchResult searchResult = indexApi.searchRegexp(".*", IFields.GENERAL_CONTENTS, true);

        System.out.println("Matched: " + searchResult.getNumberOfDocumentMatches());
        timer.printDiff("Total time");
        //        indexApi.dispose();
        //        indexApi.index(filepath, modifiedTime, general);
    }

}
