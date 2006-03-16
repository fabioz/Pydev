/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.autoedit.AbstractIndentPrefs;
import org.python.pydev.editor.autoedit.DocCmd;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.ui.BundleInfoStub;

/**
 * @author Fabio Zadrozny
 */
public class PyAutoIndentStrategyTest extends TestCase {

    private PyAutoIndentStrategy strategy;

    public static void main(String[] args) {
        try {
            PyAutoIndentStrategyTest s = new PyAutoIndentStrategyTest("testt");
            s.setUp();
            s.testIndentLevel();
            s.tearDown();
            junit.textui.TestRunner.run(PyAutoIndentStrategyTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        JythonPlugin.setBundleInfo(new BundleInfoStub());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for PyAutoIndentStrategyTest.
     * @param arg0
     */
    public PyAutoIndentStrategyTest(String arg0) {
        super(arg0);
        strategy = new PyAutoIndentStrategy();
    }
    
    public void testSpaces() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        DocCmd docCmd = new DocCmd(0, 0, "\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("    ", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\t\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("        ", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\tabc");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("    abc", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\tabc\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("    abc    ", docCmd.text);
        
        docCmd = new DocCmd(0, 0, " ");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals(" ", docCmd.text);
    }
    
    public void testTabs() {
        strategy.setIndentPrefs(new TestIndentPrefs(false, 4));
        DocCmd docCmd = new DocCmd(0, 0, "\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("\t", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\t\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("\t\t", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\tabc");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("\tabc", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "\tabc\t");
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("\tabc\t", docCmd.text);
        
        docCmd = new DocCmd(0, 0, "    abc"); //paste
        strategy.customizeDocumentCommand(new Document(""), docCmd);
        assertEquals("\tabc", docCmd.text);
    }

    public void testCommentsIndent() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));

        String doc = "class c: #some comment";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
                          "    ";
        assertEquals(expected, docCmd.text);

        //test not indent more
        doc = "    # comment:";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
        "    ";
        assertEquals(expected, docCmd.text);
        
        //test indent more
        doc = "    if False:";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
        "        ";
        assertEquals(expected, docCmd.text);
    }
    
    public void testIndentLevel() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        
        String doc = "" +
                "def m1(): #some comment\n" +
                "    print foo(a,\n" +
                "              b)";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n    ";
        assertEquals(expected, docCmd.text);
    }
    
    public void testIndentLevel2() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        
        String doc = "" +
        "def m1(): #some comment\n" +
        "    def metfoo(a,\n" +
        "               b):";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n        ";
        assertEquals(expected, docCmd.text);
    }
    
    public void testDedent() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));

        String doc = "def m1(): #some comment\n" +
                     "    return 10";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n";
        assertEquals(expected, docCmd.text);
        
        //test ending with
        doc = "def m1(): #some comment\n" +
              "    return";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n";
        assertEquals(expected, docCmd.text);
        
        //test not dedenting
        doc = "def m1(): #some comment\n" +
        "    returnIs10 = 10";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n"+
        "    ";
        assertEquals(expected, docCmd.text);
        
    }
    
    public void testIndentSpaces() {
        //test after class xxx:\n
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = "class c:";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        		          "    ";
        assertEquals(expected, docCmd.text);
        
        //test regular
        doc = "    a = 2";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "    ";
        assertEquals(expected, docCmd.text);

        //test after [ a,\n
        doc = "m = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "     ";
        assertEquals(expected, docCmd.text);
    }        
    
    public void testAfterClosePar1() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = "m = [a,";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        "     ";
        assertEquals(expected, docCmd.text);
        
    }
    
    public void testAfterClosePar2() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = "m = [a,\n" +
                     "     b,";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        "     ";
        assertEquals(expected, docCmd.text);
        
    }
    public void testAfterClosePar() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = "m = [a, (#comment";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        "         ";
        assertEquals(expected, docCmd.text);
        
//        doc = "m = [a, otherCall(), ]";
//        docCmd = new DocCmd(doc.length()-1, 0, "\n"); //right before the last ']'
//        strategy.customizeDocumentCommand(new Document(doc), docCmd);
//        expected = "\n" +
//        "      ";
//        assertEquals(expected, docCmd.text);
    }
    
    public void testIndent2() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = "m = [a, otherCall(), ";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
//        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        "      ";
//        assertEquals(expected, docCmd.text);
//
//        doc = "m = [a, otherCall(), ]";
//        docCmd = new DocCmd(doc.length()-1, 0, "\n"); //right before the last ']'
//        strategy.customizeDocumentCommand(new Document(doc), docCmd);
//        expected = "\n" +
//        "      ";
//        assertEquals(expected, docCmd.text);
        
        doc = "def m2(self):\n"+
              "    m1(a, b(), )";
        docCmd = new DocCmd(doc.length()-1, 0, "\n"); //right before the last ')'
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
              "       ";
        assertEquals(expected, docCmd.text);
        
    }
    
    public void testIndent3() {
        
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String doc = ""+
        "properties.create(a = newClass(),"; 
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n"+
        "                  ";
        assertEquals(expected, docCmd.text);
        
    }
    public void testIndent3a() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String doc = ""+
		"properties.create(a = newClass(),\n" +
		"                  b = newClass(),"; //don't indent after the '(' in this line, but to the default one
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n"+
    	"                  ";
    	assertEquals(expected, docCmd.text);
    }
    
    public void testIndent4() { //even if it does not end with ',' we should indent in parenthesis
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String doc = ""+
    	"properties.create(a = newClass(),\n" +
    	"                  b = newClass("; //don't indent after the '(' in this line, but to the default one
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n"+
    	"                               ";
    	assertEquals(expected, docCmd.text);
    }
    
    public void testDedent5() { 
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String doc = ""+
    	"properties.create(a = newClass(),\n" +
    	"                  b = newClass(\n" +
    	"                               )"; //go to the last indentation
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n"+
    	"                  ";
    	assertEquals(expected, docCmd.text);
    }
    
    public void testNoSmartIndent() {
    	
    	TestIndentPrefs prefs = new TestIndentPrefs(false, 4, true);
    	prefs.smartIndentAfterPar = false;
		strategy.setIndentPrefs(prefs);

		String doc = null;
        DocCmd docCmd = null;
        String expected = null;

	    //test after [ a,\n
        doc = "m = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,  ";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t";
        assertEquals(expected, docCmd.text);

    }

    public void testIndentTabs() {
        //test after class xxx:\n
        strategy.setIndentPrefs(new TestIndentPrefs(false, 4));
        String doc = "class c:";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "\n" +
        		          "\t";
        assertEquals(expected, docCmd.text);
        
        //test after class xxx:  \n
        strategy.setIndentPrefs(new TestIndentPrefs(false, 4));
        doc = "class c:  ";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
		           "\t";
        assertEquals(expected, docCmd.text);
        
        //test regular
        doc = "\ta = 2";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t";
        assertEquals(expected, docCmd.text);

        //test after [ a,\n
        doc = "m = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t\t";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,  ";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t\t";
        assertEquals(expected, docCmd.text);
    }        

    public void testAutoClose() {
    	strategy.setIndentPrefs(new TestIndentPrefs(false, 4, true));
        String doc = "class c(object): ";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "[");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "[]";
        assertEquals(expected, docCmd.text);

    }

    public void testAutoSelf() {
    	TestIndentPrefs testIndentPrefs = new TestIndentPrefs(false, 4, true);
    	testIndentPrefs.autoAddSelf = false;
		strategy.setIndentPrefs(testIndentPrefs);
    	String doc = null;
    	DocCmd docCmd = null;
    	String expected = null;
    	
    	doc = "class c:\n" +
    	"    def met";
    	docCmd = new DocCmd(doc.length(), 0, "(");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	expected = "():";
    	assertEquals(expected, docCmd.text);
    	
    }
    
    /**
     * Tests automatically adding/replacing brackets, colons, and parentheses.
     * @see PyAutoIndentStrategy
     */
    public void testAutoPar() {
        strategy.setIndentPrefs(new TestIndentPrefs(false, 4, true));
        String doc = "class c";
        DocCmd docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = "():";
        assertEquals(expected, docCmd.text);
        
        doc = "class c:\n" +
    		  "    def met";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "(self):";
        assertEquals(expected, docCmd.text);
        
        //same as above, but with tabs
        doc = "class c:\n" +
        "\tdef met";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "(self):";
        assertEquals(expected, docCmd.text);
        
        doc = "class c(object): #";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "("; //in comment
        assertEquals(expected, docCmd.text);
        
        doc = "def a";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "():";
        assertEquals(expected, docCmd.text);
        
        doc = "a";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "()";
        assertEquals(expected, docCmd.text);
        
        doc = "a()";
        docCmd = new DocCmd(doc.length()-1, 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "(";
        assertEquals(expected, docCmd.text);
		
		// test very simple ':' detection
		doc = "def something():";
		docCmd = new DocCmd(doc.length() - 1, 0, ":");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(15, docCmd.offset);

		// test inputting ':' when you already have a ':', like at the end of a function declaraction
		doc = "class c:\n" +
				"    def __init__(self):";
		docCmd = new DocCmd(doc.length() - 1, 0, ":");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(32, docCmd.caretOffset);
		
		// test inputting ':' at the end of a document
		doc = "class c:\n" +
				"    def __init__(self)";
		docCmd = new DocCmd(doc.length(), 0, ":");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = ":";
		assertEquals(expected, docCmd.text);
		assertEquals(31, docCmd.offset);
		
		// test same as above, but with a comment
		doc = "class c:\n" +
				"    def __init__(self): # comment";
		docCmd = new DocCmd(doc.length() - 11, 0, ":");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(32, docCmd.caretOffset);
		
		// test inputting ')' at the end of a document
		doc = "class c:\n" +
				"    def __init__(self)";
		docCmd = new DocCmd(doc.length(), 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = ")";
		assertEquals(expected, docCmd.text);
		assertEquals(0, docCmd.caretOffset);
		
		// test inputting ')' at the end of a document when it should replace a ')'
		doc = "class c:\n" +
				"    def __init__(self)";
		docCmd = new DocCmd(doc.length() - 1, 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(31, docCmd.caretOffset);
		
		// test inputting ')' in the middle of the document
		doc = "def __init__(self):\n" + 
			  "   pass";
		docCmd = new DocCmd(17, 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(18, docCmd.caretOffset);
		
		// check very simple braces insertion
		doc = "()";
		docCmd = new DocCmd(1, 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(2, docCmd.caretOffset);
		
		// check simple braces insertion not at end of document
		doc = "() ";
		docCmd = new DocCmd(1, 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(2, docCmd.caretOffset);
		
		// check insertion that should happen even being just before a ')'
		doc = "(() ";
		docCmd = new DocCmd(2, 0, ")");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = ")";
		assertEquals(expected, docCmd.text);
		assertEquals(0, docCmd.caretOffset);
		
		// check same stuff for brackets
		// check simple braces insertion not at end of document
		doc = "[] ";
		docCmd = new DocCmd(1, 0, "]");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "";
		assertEquals(expected, docCmd.text);
		assertEquals(2, docCmd.caretOffset);
		
		// two different kinds of braces next to each other
		doc = "([)";
		docCmd = new DocCmd(2, 0, "]");
		strategy.customizeDocumentCommand(new Document(doc), docCmd);
		expected = "]";
		assertEquals(expected, docCmd.text);
		assertEquals(0, docCmd.caretOffset);
    }
    
    public void testElse() {
        //first part of test - simple case
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        String strDoc = "if foo:\n" +
                     "    print a\n" +
                     "    else";
        int initialOffset = strDoc.length();
        DocCmd docCmd = new DocCmd(initialOffset, 0, ":");
        Document doc = new Document(strDoc);
        strategy.customizeDocumentCommand(doc, docCmd);
        String expected = ":";
        assertEquals(docCmd.offset, initialOffset-4);
        assertEquals(expected, docCmd.text);
        assertEquals(
                "if foo:\n" +
                "    print a\n" +
                "else",
                doc.get());
        
        //second part of test - should not dedent
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        strDoc = 
        "if foo:\n" +
        "    if somethingElse:" +
        "        print a\n" +
        "    else";
        initialOffset = strDoc.length();
        docCmd = new DocCmd(initialOffset, 0, ":");
        doc = new Document(strDoc);
        strategy.customizeDocumentCommand(doc, docCmd);
        expected = ":";
        assertEquals(expected, docCmd.text);
        assertEquals(docCmd.offset, initialOffset);
        assertEquals(
                "if foo:\n" +
                "    if somethingElse:" +
                "        print a\n" +
                "    else",
                doc.get());
        
    }
    
    public void testElif() {
    	//first part of test - simple case
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
    	String strDoc = "if foo:\n" +
    	"    print a\n" +
    	"    elif";
    	int initialOffset = strDoc.length();
    	DocCmd docCmd = new DocCmd(initialOffset, 0, " ");
    	Document doc = new Document(strDoc);
    	strategy.customizeDocumentCommand(doc, docCmd);
    	String expected = " ";
    	assertEquals(docCmd.offset, initialOffset-4);
    	assertEquals(expected, docCmd.text);
    	assertEquals(
    			"if foo:\n" +
    			"    print a\n" +
    			"elif",
    			doc.get());
    	
    	//second part of test - should not dedent
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
    	strDoc = 
    		"if foo:\n" +
    		"    if somethingElse:" +
    		"        print a\n" +
    		"    elif";
    	initialOffset = strDoc.length();
    	docCmd = new DocCmd(initialOffset, 0, " ");
    	doc = new Document(strDoc);
    	strategy.customizeDocumentCommand(doc, docCmd);
    	expected = " ";
    	assertEquals(expected, docCmd.text);
    	assertEquals(docCmd.offset, initialOffset);
    	assertEquals(
    			"if foo:\n" +
    			"    if somethingElse:" +
    			"        print a\n" +
    			"    elif",
    			doc.get());
    	
    }
    
    
    public void testElseInFor() {
        //first part of test - simple case
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        String strDoc = 
        "for i in []:\n" +
        "    msg=\"success at %s\" % i\n" +
        "    else" +
        "";
        int initialOffset = strDoc.length();
        DocCmd docCmd = new DocCmd(initialOffset, 0, ":");
        Document doc = new Document(strDoc);
        strategy.customizeDocumentCommand(doc, docCmd);
        String expected = ":";
        assertEquals(docCmd.offset, initialOffset-4);
        assertEquals(expected, docCmd.text);
        assertEquals(
                "for i in []:\n" +
                "    msg=\"success at %s\" % i\n" +
                "else" +
                "",
                doc.get());
    }
    
    public void testElseInTry() {
        //first part of test - simple case
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        String strDoc = 
            "try:\n" +
            "    print a\n" +
            "except:\n" +
            "    pass\n" +
            "    else";
        int initialOffset = strDoc.length();
        DocCmd docCmd = new DocCmd(initialOffset, 0, ":");
        Document doc = new Document(strDoc);
        strategy.customizeDocumentCommand(doc, docCmd);
        String expected = ":";
        assertEquals(docCmd.offset, initialOffset-4);
        assertEquals(expected, docCmd.text);
        assertEquals(
                "try:\n" +
                "    print a\n" +
                "except:\n" +
                "    pass\n" +
                "else",
                doc.get());
    }
    
    public void testElifWithPar() {
    	//first part of test - simple case
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
    	String strDoc = "if foo:\n" +
    	"    print a\n" +
    	"    elif";
    	int initialOffset = strDoc.length();
    	DocCmd docCmd = new DocCmd(initialOffset, 0, "(");
    	Document doc = new Document(strDoc);
    	strategy.customizeDocumentCommand(doc, docCmd);
    	String expected = "()";
    	assertEquals(docCmd.offset, initialOffset-4);
    	assertEquals(expected, docCmd.text);
    	assertEquals(
    			"if foo:\n" +
    			"    print a\n" +
    			"elif",
    			doc.get());
    	
    	//second part of test - should not dedent
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
    	strDoc = 
    		"if foo:\n" +
    		"    if somethingElse:" +
    		"        print a\n" +
    		"    elif";
    	initialOffset = strDoc.length();
    	docCmd = new DocCmd(initialOffset, 0, "(");
    	doc = new Document(strDoc);
    	strategy.customizeDocumentCommand(doc, docCmd);
    	expected = "()";
    	assertEquals(expected, docCmd.text);
    	assertEquals(docCmd.offset, initialOffset);
    	assertEquals(
    			"if foo:\n" +
    			"    if somethingElse:" +
    			"        print a\n" +
    			"    elif",
    			doc.get());
    	
    }
    public void testAutoImportStr() {
        strategy.setIndentPrefs(new TestIndentPrefs(false, 4, true));
        String doc = "from xxx";
        DocCmd docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        String expected = " import ";
        assertEquals(expected, docCmd.text);
        
        doc = "from xxx import";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "no from xxx";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "From xxx";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "from this space";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "from";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);

        doc = "from xxx import yyy";
        docCmd = new DocCmd(8, 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "from xxx #import yyy";
        docCmd = new DocCmd(8, 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " import ";
        assertEquals(expected, docCmd.text);
        
    }

    private final class TestIndentPrefs extends AbstractIndentPrefs {
        
        private boolean useSpaces;
        private int tabWidth;
        boolean autoPar = true;
        boolean autoColon = true;
        boolean autoBraces = true;
        boolean autoWriteImport = true;
        boolean smartIndentAfterPar = true;
        boolean autoAddSelf = true;
        private boolean autoElse;

        public TestIndentPrefs(boolean useSpaces, int tabWidth){
            this.useSpaces = useSpaces;
            this.tabWidth = tabWidth;
        }

        public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar){
            this(useSpaces,tabWidth, autoPar, true);
        }

        public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar, boolean autoElse){
            this(useSpaces,tabWidth);
            this.autoPar = autoPar;
            this.autoElse = autoElse;
        }
        
        public boolean getUseSpaces() {
            return useSpaces;
        }

        public int getTabWidth() {
            return tabWidth;
        }

        public boolean getAutoParentesis() {
            return autoPar;
        }

		public boolean getAutoColon() {
			return autoColon;
		}

		public boolean getAutoBraces()
		{
			return autoBraces;
		}

        public boolean getAutoWriteImport() {
            return autoWriteImport;
        }

		public boolean getSmartIndentPar() {
			return smartIndentAfterPar;
		}

		public boolean getAutoAddSelf() {
			return autoAddSelf;
		}

        public boolean getAutoDedentElse() {
            return autoElse;
        }

    }

}
