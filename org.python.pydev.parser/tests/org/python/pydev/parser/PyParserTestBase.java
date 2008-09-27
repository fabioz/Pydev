package org.python.pydev.parser;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;

public class PyParserTestBase extends TestCase {
    protected static PyParser parser;
    private static int defaultVersion;
    private static IGrammarVersionProvider versionProvider = new IGrammarVersionProvider(){

        public int getGrammarVersion() {
            return defaultVersion;
        }};

    /**
     * @param defaultVersion the defaultVersion to set
     */
    protected static void setDefaultVersion(int defaultVersion) {
        PyParserTestBase.defaultVersion = defaultVersion;
    }

    /**
     * @return the defaultVersion
     */
    protected static int getDefaultVersion() {
        return defaultVersion;
    }

    protected void setUp() throws Exception {
        PyParser.ACCEPT_NULL_INPUT_EDITOR = true;
        PyParser.ENABLE_TRACING = true;
        PyParser.TRY_REPARSE = false;
        ParseException.verboseExceptions = true;
        parser = new PyParser(versionProvider);
        setDefaultVersion(IPythonNature.LATEST_GRAMMAR_VERSION);
        super.setUp();
    }

    protected void tearDown() throws Exception {
        PyParser.ACCEPT_NULL_INPUT_EDITOR = false;
        PyParser.ENABLE_TRACING = false;
        PyParser.TRY_REPARSE = true;
        super.tearDown();
    }

    /**
     * @param s
     * @return 
     */
    protected static SimpleNode parseLegalDocStr(String s, Object ... additionalErrInfo) {
        Document doc = new Document(s);
        //by default always use the last version for parsing
        return parseLegalDoc(doc, additionalErrInfo, parser);
    }

    protected SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
        return parseLegalDoc(doc, additionalErrInfo, parser);
    }
    protected ParseException parseILegalDoc(IDocument doc) {
        parser.setDocument(doc, false, null);
        Tuple<SimpleNode, Throwable> objects = parser.reparseDocument();
        Object err = objects.o2;
        if(err == null){
            fail("Expected a ParseException and the doc was successfully parsed.");
        }
        if(!(err instanceof ParseException)){
            fail("Expected a ParseException and received:"+err.getClass());
        }
        return (ParseException) err;
    }

    /**
     * @param additionalErrInfo can be used to add additional errors to the fail message if the doc is not parseable
     * @param parser the parser to be used to do the parsing.
     */
    protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, PyParser parser) {
        parser.setDocument(doc, false, null);
        Tuple<SimpleNode, Throwable> objects = parser.reparseDocument();
        Object err = objects.o2;
        if(err != null){
            String s = "";
            for (int i = 0; i < additionalErrInfo.length; i++) {
                s += additionalErrInfo[i];
            }
            if (err instanceof ParseException) {
                ParseException parseErr = (ParseException) err;
                parseErr.printStackTrace();
                
                Token token = parseErr.currentToken;
                if(token != null){
                    fail("Expected no error, received: "+parseErr.getMessage()+" "+s+" line:"+token.beginLine+ " col:"+token.beginColumn);
                }
            }
             
            fail("Expected no error, received: "+err+" "+s);
        }
        assertNotNull(objects.o1);
        return objects.o1;
    }

    public void testEmpty() {
    }
}
