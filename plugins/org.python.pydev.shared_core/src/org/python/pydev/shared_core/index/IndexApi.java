package org.python.pydev.shared_core.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.partitioner.IContentsScanner;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class IndexApi {

    private final Directory indexDir;
    private IndexWriter writer;
    private SearcherManager searchManager;
    private SearcherFactory searcherFactory;
    private int maxMatches = 501;
    private CodeAnalyzer analyzer;

    public IndexApi(Directory indexDir) throws IOException {
        this.indexDir = indexDir;
        init();
    }

    public IndexApi(File indexDir) throws IOException {
        this(FSDirectory.open(indexDir.toPath()));
    }

    public void init() throws IOException {
        this.analyzer = new CodeAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setCommitOnClose(true);
        writer = new IndexWriter(this.indexDir, config);

        searcherFactory = new SearcherFactory();
        boolean applyAllDeletes = true;
        searchManager = new SearcherManager(writer, applyAllDeletes, searcherFactory);
    }

    public void registerTokenizer(String fieldName, TokenStreamComponents tokenStream) {
        this.analyzer.registerTokenizer(fieldName, tokenStream);
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
    public void index(Path filepath, long modifiedTime, ITokenScanner tokenScanner, ITokenMapper mapper)
            throws IOException {
        if (this.writer == null) {
            return;
        }
        IContentsScanner contentsScanner = (IContentsScanner) tokenScanner;
        Document doc = new Document();

        doc.add(new StringField(ITokenMapper.FILEPATH, filepath.toPortableString(), Field.Store.YES)); // StringField is not analyzed
        doc.add(new NumericDocValuesField(ITokenMapper.MODIFIED_TIME, modifiedTime));

        String lastSegment = filepath.removeFileExtension().lastSegment();
        if (lastSegment == null) {
            lastSegment = "";
        }
        doc.add(new StringField(ITokenMapper.FILENAME, lastSegment, Field.Store.YES)); // StringField is not analyzed
        String fileExtension = filepath.getFileExtension();
        if (fileExtension != null) {
            doc.add(new StringField(ITokenMapper.EXTENSION, fileExtension, Field.Store.YES)); // StringField is not analyzed
        }

        FastStringBuffer buf = new FastStringBuffer();
        IToken nextToken = tokenScanner.nextToken();
        while (!nextToken.isEOF()) {
            if (!nextToken.isUndefined() && !nextToken.isWhitespace()) {
                int offset = tokenScanner.getTokenOffset();
                int length = tokenScanner.getTokenLength();
                contentsScanner.getContents(offset, length, buf.clear());
                String tokenMapping = mapper.getTokenMapping(nextToken);
                if (tokenMapping != null) {
                    //Note: TextField should be analyzed/normalized in Analyzer.createComponents(String)
                    doc.add(new TextField(tokenMapping, buf.toString(), Field.Store.YES));
                }
            }
            nextToken = tokenScanner.nextToken();
        }

        this.writer.addDocument(doc);
    }

    public SearchResult search(String string) throws IOException {
        try {
            this.writer.commit();
        } catch (Exception e) {
            Log.log(e);
        }
        SearchResult result = new SearchResult();
        try (IndexReader reader = DirectoryReader.open(writer, true);) {
            IndexSearcher searcher = searcherFactory.newSearcher(reader);

            Query query = new RegexpQuery(new Term(ITokenMapper.PYTHON, string));
            TopDocs search = searcher.search(query, maxMatches);
            ScoreDoc[] scoreDocs = search.scoreDocs;
            result.add(scoreDocs, reader);
        }
        return result;
    }

    public void setMaxMatches(int maxMatches) {
        this.maxMatches = maxMatches;
    }

    public int getMaxMatches() {
        return maxMatches;
    }

}
