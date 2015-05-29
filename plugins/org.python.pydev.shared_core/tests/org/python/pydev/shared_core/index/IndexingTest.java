package org.python.pydev.shared_core.index;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.shared_core.partitioner.CustomRuleBasedPartitionScanner;

import junit.framework.TestCase;

public class IndexingTest extends TestCase {
    private IndexApi indexApi;
    private ITokenMapper mapper = new ITokenMapper() {

        @Override
        public String getTokenMapping(IToken nextToken) {
            String data = (String) nextToken.getData();
            if (IDocument.DEFAULT_CONTENT_TYPE.equals(data) || data == null) {
                return ITokenMapper.PYTHON;
            }
            throw new AssertionError("Unexpected: " + data);
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Create it in-memory
        indexApi = new IndexApi(new RAMDirectory());
        indexApi.registerTokenizer(ITokenMapper.PYTHON, CodeAnalyzer.createPythonStreamComponents());
        TokenStreamComponents stringOrComment = CodeAnalyzer.createStringsOrCommentsStreamComponents();
        indexApi.registerTokenizer(ITokenMapper.STRING, stringOrComment);
        indexApi.registerTokenizer(ITokenMapper.COMMENT, stringOrComment);
    }

    @Override
    public void tearDown() throws Exception {
        indexApi.dispose();
    }

    public void testSimpleIndexing() throws Exception {
        indexApi.index(new Path("a.py"), 0L, createScanner("aaaaaaaa"), mapper);
        indexApi.index(new Path("b.py"), 0L, createScanner("bbbbbbb"), mapper);
        indexApi.index(new Path("c.py"), 0L, createScanner("another"), mapper);

        SearchResult result = indexApi.search("a");
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.search("aaaaaaaa");
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.search("a.*");
        assertEquals(2, result.getNumberOfDocumentMatches());

        result = indexApi.search("b.*");
        assertEquals(1, result.getNumberOfDocumentMatches());

        indexApi.setMaxMatches(1);
        result = indexApi.search("a.*");
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

        SearchResult result = indexApi.search("a");
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.search("aaaaaaaa");
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.search("a.*");
        assertEquals(2, result.getNumberOfDocumentMatches());

        result = indexApi.search("b.*");
        assertEquals(1, result.getNumberOfDocumentMatches());

        result = indexApi.search("a");
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.search("othe");
        assertEquals(0, result.getNumberOfDocumentMatches());

        result = indexApi.search("other");
        assertEquals(1, result.getNumberOfDocumentMatches());

        indexApi.setMaxMatches(1);
        result = indexApi.search("a.*");
        assertEquals(1, result.getNumberOfDocumentMatches());
    }
}
