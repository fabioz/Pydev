package org.python.pydev.parser;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.parser.ParseException;
import org.python.parser.SimpleNode;
import org.python.parser.Token;
import org.python.pydev.core.IPythonNature;

import junit.framework.TestCase;

public class PyParserTestBase extends TestCase {
    protected PyParser parser;

    protected void setUp() throws Exception {
        PyParser.ACCEPT_NULL_EDITOR = true;
        PyParser.ENABLE_TRACING = true;
        PyParser.TRY_REPARSE = false;
        ParseException.verboseExceptions = true;
        parser = new PyParser();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        PyParser.ACCEPT_NULL_EDITOR = false;
        PyParser.ENABLE_TRACING = false;
        PyParser.TRY_REPARSE = true;
        ParseException.verboseExceptions = false;
        super.tearDown();
    }

	/**
	 * @param s
	 * @return 
	 */
	protected static SimpleNode parseLegalDocStr(String s, Object ... additionalErrInfo) {
	    Document doc = new Document(s);
	    return parseLegalDoc(doc, additionalErrInfo, new PyParser());
	}

	protected SimpleNode parseLegalDoc(IDocument doc) {
        return parseLegalDoc(doc);
    }
    
	protected SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
	    return parseLegalDoc(doc, additionalErrInfo, parser);
    }
	protected void parseILegalDoc(IDocument doc) {
	    parser.setDocument(doc, false);
	    Object[] objects = parser.reparseDocument((IPythonNature)null);
	    Object err = objects[1];
	    if(err == null){
	        fail("Expected a ParseException and the doc was successfully parsed.");
        }
	    if(!(err instanceof ParseException)){
	        fail("Expected a ParseException and received:"+err.getClass());
	    }
    }

    /**
	 * @param additionalErrInfo can be used to add additional errors to the fail message if the doc is not parseable
	 * @param parser the parser to be used to do the parsing.
	 */
	protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, PyParser parser) {
	    parser.setDocument(doc, false);
	    Object[] objects = parser.reparseDocument((IPythonNature)null);
	    Object err = objects[1];
	    if(err != null){
	        String s = "";
	        for (int i = 0; i < additionalErrInfo.length; i++) {
	            s += additionalErrInfo[i];
	        }
	        if (err instanceof ParseException) {
	            ParseException parseErr = (ParseException) err;
	            
	            Token token = parseErr.currentToken;
	            if(token != null){
	                fail("Expected no error, received: "+err+" "+s+" line:"+token.beginLine+ " col:"+token.beginColumn);
	            }
	        }
	         
	        fail("Expected no error, received: "+err+" "+s);
	    }
	    assertNotNull(objects[0]);
	    return (SimpleNode) objects[0];
	}

    public void testEmpty() {
    }
}
