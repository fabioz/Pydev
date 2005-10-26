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
        ParseException.verboseExceptions = true;
        parser = new PyParser();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        PyParser.ACCEPT_NULL_EDITOR = false;
        PyParser.ENABLE_TRACING = false;
        ParseException.verboseExceptions = false;
        super.tearDown();
    }

	/**
	 * @param s
	 * @return 
	 */
	protected SimpleNode parseLegalDocStr(String s, Object ... additionalErrInfo) {
	    Document doc = new Document(s);
	    return parseLegalDoc(doc, additionalErrInfo);
	}

	/**
	 * @param additionalErrInfo 
	 * @param parser
	 */
	protected SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
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

}
