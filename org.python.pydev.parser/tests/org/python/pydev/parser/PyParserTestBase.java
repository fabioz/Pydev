package org.python.pydev.parser;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;

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

	protected SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
	    return parseLegalDoc(doc, additionalErrInfo, parser);
    }
	protected ParseException parseILegalDoc(IDocument doc) {
	    parser.setDocument(doc, false);
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

	protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, PyParser parser) {
        // default implementation: parser grammar with version 2.4
       return parseLegalDoc(doc, additionalErrInfo, parser, IPythonNature.GRAMMAR_PYTHON_VERSION_2_4); 
    }
    
    /**
	 * @param additionalErrInfo can be used to add additional errors to the fail message if the doc is not parseable
	 * @param parser the parser to be used to do the parsing.
	 */
	protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, PyParser parser, int version) {
	    parser.setDocument(doc, false);
        parser.setGrammar(version);
        Tuple<SimpleNode, Throwable> objects = parser.reparseDocument();
	    Object err = objects.o2;
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
	    assertNotNull(objects.o1);
	    return objects.o1;
	}

    public void testEmpty() {
    }
}
