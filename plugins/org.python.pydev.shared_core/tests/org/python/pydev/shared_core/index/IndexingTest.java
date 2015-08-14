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

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.shared_core.index.IndexApi.DocumentInfo;
import org.python.pydev.shared_core.index.IndexApi.IDocumentsVisitor;
import org.python.pydev.shared_core.partitioner.CustomRuleBasedPartitionScanner;
import org.python.pydev.shared_core.structure.OrderedMap;

import junit.framework.TestCase;

public class IndexingTest extends TestCase {
    private IndexApi indexApi;
    private IFields mapper = new IFields() {

        @Override
        public String getTokenFieldName(IToken nextToken) {
            String data = (String) nextToken.getData();
            if (IDocument.DEFAULT_CONTENT_TYPE.equals(data) || data == null) {
                return IFields.PYTHON;
            }
            throw new AssertionError("Unexpected: " + data);
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Create it in-memory
        indexApi = new IndexApi(new RAMDirectory(), true);
        indexApi.registerTokenizer(IFields.PYTHON, CodeAnalyzer.createPythonStreamComponents());
        TokenStreamComponents stringOrComment = CodeAnalyzer.createStringsOrCommentsStreamComponents();
        indexApi.registerTokenizer(IFields.STRING, stringOrComment);
        indexApi.registerTokenizer(IFields.COMMENT, stringOrComment);
    }

    @Override
    public void tearDown() throws Exception {
        indexApi.dispose();
    }

    public void testSimpleIndexing() throws Exception {
        indexApi.index(new Path("a.py"), 0L, createScanner("aaaaaaaa"), mapper);
        indexApi.index(new Path("b.py"), 0L, createScanner("bbbbbbb"), mapper);
        indexApi.index(new Path("c.py"), 0L, createScanner("another"), mapper);

        SearchResult result = indexApi.searchRegexp("a", IFields.PYTHON, true);
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("aaaaaaaa", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("a.*", IFields.PYTHON, true);
        assertEquals(2, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("b.*", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        indexApi.setMaxMatches(1);
        result = indexApi.searchRegexp("a.*", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());
    }

    private ITokenScanner createScanner(String string) {
        CustomRuleBasedPartitionScanner scanner = new CustomRuleBasedPartitionScanner();
        scanner.setRange(new Document(string), 0, string.length());
        return scanner;
    }

    public void testCaseIndexing() throws Exception {
        indexApi.index(new Path("a.py"), 0L, createScanner("aAaAaAaA"), mapper);
        indexApi.index(new Path("b.py"), 0L, createScanner("bBbBbBb"), mapper);
        indexApi.index(new Path("c.py"), 0L, createScanner("nother other Another"), mapper);

        SearchResult result = indexApi.searchRegexp("a", IFields.PYTHON, true);
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("aaaaaaaa", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("a.*", IFields.PYTHON, true);
        assertEquals(2, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("b.*", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("a", IFields.PYTHON, true);
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("othe", IFields.PYTHON, true);
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.searchRegexp("other", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        indexApi.setMaxMatches(1);
        result = indexApi.searchRegexp("a.*", IFields.PYTHON, true);
        assertEquals(1, result.getNumberOfDocumentMatches());
    }

    public void testKeepingSynched() throws Exception {
        indexApi.index(new Path("a.py"), 0L, "aAaAaAaA");
        indexApi.index(new Path("b.py"), 1L, "bBbBbBb");
        indexApi.index(new Path("c.py"), 2L, "nother other Another");
        indexApi.commit();
        final Map<Integer, String> found = new HashMap<>();
        IDocumentsVisitor visitor = new IDocumentsVisitor() {

            @Override
            public void visit(DocumentInfo documentInfo) {
                found.put(documentInfo.getDocId(), documentInfo.get(IFields.FILEPATH));
            }
        };
        indexApi.visitAllDocs(visitor, IFields.FILEPATH);
        assertEquals(3, found.size());

        HashMap<String, Collection<String>> map = new HashMap<>();
        map.put(IFields.MODIFIED_TIME, Arrays.asList("1", "0"));
        indexApi.removeDocs(map);

        found.clear();

        indexApi.visitAllDocs(visitor, IFields.FILEPATH);
        assertEquals(1, found.size());
    }

    public void testExactMatch() throws Exception {
        indexApi.index(new Path("a.py"), 0L, "aAaAaAaA");
        indexApi.index(new Path("b.py"), 1L, "bBbBbBb");
        indexApi.index(new Path("c.py"), 2L, "nother other Another");

        SearchResult result = indexApi.searchExact("aaaaaaaa", IFields.GENERAL_CONTENTS, true);
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.searchExact("aaaaaaaa", IFields.PYTHON, true);
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.searchExact("a.*", IFields.GENERAL_CONTENTS, true);
        assertEquals(0, result.getNumberOfDocumentMatches());
    }

    public void testWildCards() throws Exception {
        indexApi.index(new Path("a.py"), 0L, "aabbcc");

        SearchResult result = indexApi.searchWildcard(new HashSet<>(Arrays.asList("a*bc*")), IFields.GENERAL_CONTENTS,
                true, null, null);
        assertEquals(1, result.getNumberOfDocumentMatches());
    }

    public void testWildCards2a() throws Exception {
        indexApi.index(new Path("a.py"), 0L, "aabbcc");

        // No match because it has no * in the end
        SearchResult result = indexApi.searchWildcard(new HashSet<>(Arrays.asList("a*bc")), IFields.GENERAL_CONTENTS,
                true, null, null);
        assertEquals(0, result.getNumberOfDocumentMatches());
    }

    public void testWildCards2() throws Exception {
        indexApi.index(new Path("a.py"), 0L, "ab");

        SearchResult result = indexApi.searchWildcard(new HashSet<>(Arrays.asList("*ab*")), IFields.GENERAL_CONTENTS,
                true, null, null);
        assertEquals(1, result.getNumberOfDocumentMatches());
    }

    public void testSearchModuleKey() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(IFields.FILENAME, "my.mod");
        Reader reader = new StringReader("ab");
        indexApi.index(map, reader, IFields.GENERAL_CONTENTS);

        map = new HashMap<>();
        map.put(IFields.FILENAME, "my.mod2");
        reader = new StringReader("ab");
        indexApi.index(map, reader, IFields.GENERAL_CONTENTS);

        IDocumentsVisitor visitor = new IDocumentsVisitor() {

            @Override
            public void visit(DocumentInfo documentInfo) {
            }
        };
        OrderedMap<String, Set<String>> fieldNameToValues = new OrderedMap<>();
        fieldNameToValues.put(IFields.GENERAL_CONTENTS, new HashSet<>(Arrays.asList("*ab*")));
        fieldNameToValues.put(IFields.FILENAME, new HashSet<>(Arrays.asList("my.mod")));

        SearchResult result = indexApi.searchWildcard(fieldNameToValues, true, visitor, null, IFields.FILENAME);
        assertEquals(1, result.getNumberOfDocumentMatches());

        fieldNameToValues.put(IFields.FILENAME, new HashSet<>(Arrays.asList("my.mod*")));
        result = indexApi.searchWildcard(fieldNameToValues, true, visitor, null, IFields.FILENAME);
        assertEquals(2, result.getNumberOfDocumentMatches());
    }
}
