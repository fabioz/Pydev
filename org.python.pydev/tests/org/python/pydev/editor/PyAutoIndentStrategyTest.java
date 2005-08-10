/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.python.pydev.editor.autoedit.AbstractIndentPrefs;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyAutoIndentStrategyTest extends TestCase {

    private PyAutoIndentStrategy strategy;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyAutoIndentStrategyTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
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
                   "      ";
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
                   "\t  ";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t\t  ";
        assertEquals(expected, docCmd.text);

        //test after \t[ a,\n
        doc = "\tm = [a,  ";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "\t\t  ";
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
        
        doc = "class c(object): #";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "()";
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
    }

    private final class TestIndentPrefs extends AbstractIndentPrefs {
        
        private boolean useSpaces;
        private int tabWidth;
        boolean autoPar = true;
        boolean autoColon = true;
        boolean autoBraces = true;
        boolean autoWriteImport = true;

        public TestIndentPrefs(boolean useSpaces, int tabWidth){
            this.useSpaces = useSpaces;
            this.tabWidth = tabWidth;
        }

        public TestIndentPrefs(boolean useSpaces, int tabWidth, boolean autoPar){
            this(useSpaces,tabWidth);
            this.autoPar = autoPar;
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

    }

    public static class DocCmd extends DocumentCommand{
        public DocCmd(int offset, int length, String text){
            this.offset = offset;
            this.length = length;
            this.text   = text;
        }
    }

}
