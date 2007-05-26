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

/**
 * @author Fabio Zadrozny
 */
public class PyAutoIndentStrategyTest extends TestCase {

    private PyAutoIndentStrategy strategy;
	private String doc;
	private DocCmd docCmd;
	private String expected;

    public static void main(String[] args) {
        try {
            PyAutoIndentStrategyTest s = new PyAutoIndentStrategyTest("testt");
            s.setUp();
            s.testTryExceptDedent();
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
    
    
    public void testNewLineWithNewWithConstruct() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "class C:\n" +
        "    with a:" +
        "";
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n        ", docCmd.text); 
    }

    public void testTab() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = 
            "        args = [ '-1', '-2',\n"+ 
            "                ";
        DocCmd docCmd = new DocCmd(str.length(), 0, "\t");
        strategy.customizeDocumentCommand(new Document(str), docCmd);
        assertEquals("    ", docCmd.text);
    }
    
    public void testTab2() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = 
            "class Foo:\n"+
            "    def m"+
        "";
        DocCmd docCmd = new DocCmd(str.length()-"    def m".length(), 4, "\t");
        Document document = new Document(str);
        strategy.customizeDocumentCommand(document, docCmd);
        assertEquals("    ", docCmd.text); 
        assertEquals(str, document.get()); //as we already have a selection, nothing should be deleted
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
    
    

    public void testNewLineAfterReturn() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    			"def m1(self):\n" +
    			"    return 'foo'\n" +
    			"#ffo" +
    			"" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"#ffo".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testIgnoreComment() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"titleEnd = ('[#')" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testIgnoreComment2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"titleEnd = ('''\n" +
    	"            [#''')" + //should wrap to the start
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testNewLineAfterOpeningParWithOtherContents() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def m1(  self,";
    	//        |<-- should indent here in this case, and not on the parenthesis
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n         ", docCmd.text); 
    }
    
    public void testNewLineAfterReturn2() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "def m1(self):\n" +
        "    return ('foo',";
                     
                         
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n            ", docCmd.text); 
        
    }
    
    
    public void testMaintainIndent() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "def moo():\n" +
        "    if not 1:\n" +
        "        print 'foo'\n" +
        "    print 'bla'"+
        "";
        
        
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength()-"print 'bla'".length(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n    ", docCmd.text); 
        
    }
    
    public void testMaintainIndent2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def moo():\n" +
    	"    if not 1:\n" +
    	"        print 'foo'\n" +
    	"    print 'bla'"+
    	"";
    	
    	
    	final Document doc = new Document(str);
    	int offset = doc.getLength()-"  print 'bla'".length();
		DocCmd docCmd = new DocCmd(offset, 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n  ", docCmd.text); 
    	assertEquals(offset+2, docCmd.caretOffset); 
    	
    }
    
    
    public void testDontChangeCursorOffset() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def moo():\n" +
    	"    if not 1:\n" +
    	"        print    'foo'" +
    	"";
    	
    	
    	final Document doc = new Document(str);
    	int offset = doc.getLength()-"    'foo'".length();
    	DocCmd docCmd = new DocCmd(offset, 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n        ", docCmd.text); 
    	assertEquals(0, docCmd.caretOffset); //don't change it 
    	
    }
    
    
    public void testTabIndentToLevel() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"properties.create( \n" +
    	"                  a,\n" +
    	"        \n" +
    	"\n" + //cursor is here
    	"                  b,\n" +
    	")" +
    	"";
    	
    	
    	final Document doc = new Document(str);
    	int offset = doc.getLength()-"\n                  b,\n)".length();
    	DocCmd docCmd = new DocCmd(offset, 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("                  ", docCmd.text); 
    	
    }
    
    
    
    public void testTabIndentToLevel2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class ContaminantFont( Barrier, ModelBase ):\n" +
    	"    '''\n" +
    	"    This class contains information to edit a contaminant.\n" +
    	"    '''\n" +
    	"    properties.create( \n" +
    	"                          \n" +
    	"                          #defines where is the source (in the water or in the soil)\n" +
    	"                          sourceLocation = SOURCE_LOCATION_WATER,\n" +
    	"                          \n" +
    	"" + //we're here (indent to the first level)
    	"";
    	
    	
    	final Document doc = new Document(str);
    	int offset = doc.getLength();
    	DocCmd docCmd = new DocCmd(offset, 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("    ", docCmd.text); 
    	
    }
    
    
    
    public void testTabIndentToLevel3() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class ContaminantFont( Barrier, ModelBase ):\n" +
    	"    '''\n" +
    	"    This class contains information to edit a contaminant.\n" +
    	"    '''\n" +
    	"    properties.create( \n" +
    	"                          \n" +
    	"                          #defines where is the source (in the water or in the soil)\n" +
    	"                          sourceLocation = SOURCE_LOCATION_WATER,\n" +
    	"                          \n" +
    	"    " + //now that we're already in the first level, indent to the current level
    	"";
    	
    	
    	final Document doc = new Document(str);
    	int offset = doc.getLength();
    	DocCmd docCmd = new DocCmd(offset, 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("                      ", docCmd.text); 
    	
    }
    
    
    public void testNoAutoIndentClosingPar() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"newTuple = (\n" +
    	"              what(),\n" + //the next line should be indented to this one, and not to the start of the indent
    	"            )\n" +
    	"";
    	
    	
    	final Document doc = new Document(str);
    	String s = 
    		"\n"+
    		"            )\n";
    	DocCmd docCmd = new DocCmd(doc.getLength()-s.length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n              ", docCmd.text); 
    	
    }
    
    public void testNoAutoIndentClosingPar2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"newTuple = (\n" +
    	"              what(),\n" + 
    	"\n" + //pressing tab in the start of this line will bring us to the 'what()' level.
    	"            )\n" +
    	"";
    	
    	
    	final Document doc = new Document(str);
    	String s = 
    		"\n"+
    		"            )\n";
    	DocCmd docCmd = new DocCmd(doc.getLength()-s.length(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("              ", docCmd.text); 
    	
    }
    
    public void testNewLineAfterLineWithComment() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"string1 = '01234546789[#]'" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testNewLine10() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def M1(a):\n" +
    	"    doFoo(a,b(),\n" +
    	"          '',b)" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    	
    }
    
    public void testNewLine11() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def fun():\n" +
    	"    if True:\n" +
    	"        passif False: 'foo'" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"if False: 'foo'".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    	
    }
    
    
    public void testNewLine12() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"if False:print 'done'" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"print 'done'".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    }
    

    public void testNewLine3() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "for a in b:    " +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-4, 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    	
    	String expected = "for a in b:    ";
    	assertEquals(expected, doc.get());
    }
    
    public void testNewLine6() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    			"for v in w:\n" +
    			"    pass\n" + //dedent on pass
    			"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    }
    
    public void testNewLine6a() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def getSpilledComps( *dummy ):\n" +
    	"    return [self.component4]" + //dedent here
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    }
    
    public void testNewLine7() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "class C:\n" +
        "    a = 30\n" +
        "print C.a\n" +
        "\n" +
        "";
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n", docCmd.text); 
    }
    
    public void testNewLine8() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "class C:\n" +
        "    pass\n" +
        "    a = 30\n" +
        "    " +
        "";
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n    ", docCmd.text); 
    }
    
    public void testIndent() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"while False:\n" +
    	"    if foo:" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"if foo:".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    }
    
    public void testIndentAfterRet() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class Foo:\n" +
    	"    def m1():\n" +
    	"        for a in b:\n" +
    	"            if a = 20:\n" +
    	"                print 'foo'\n" +
    	"        return 30\n" +
    	"    " +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n    ", docCmd.text); 
    }
    
    public void testIndentAfterRet2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class Foo:\n" +
    	"    def m1():\n" +
    	"        for a in b:\n" +
    	"            if a = 20:\n" +
    	"                print 'foo'\n" +
    	"        return 30\n" +
    	"    \n" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("    ", docCmd.text); 
    }
    
    public void testNewLine9() {
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        String str = "" +
        "class C:\n" +
        "    try:" +
        "";
        final Document doc = new Document(str);
        DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
        strategy.customizeDocumentCommand(doc, docCmd);
        assertEquals("\n        ", docCmd.text); 
    }
    
    public void testNewLine4() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    			"def a():\n" +
    			"    print a" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"    print a".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	String expected = "" +
    	"def a():\n" +
    	"    print a" +
    	"";
    	assertEquals(expected, doc.get()); 
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testNewLine5() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"def a():\n" +
    	"    " +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"    ".length(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	String expected = "" +
    	"def a():\n" +
    	"    " +
    	"";
    	assertEquals(expected, doc.get()); 
    	assertEquals("\n", docCmd.text); 
    }
    
    public void testNewLine() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "createintervention() #create " +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    public void testNewLine2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "err)" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n", docCmd.text); 
    	
    }
    
    
    public void testTabInComment() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "#comment" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("    ", docCmd.text); // a single tab should go to the correct indent
    	
    }
    
    public void testIndentingWithTab() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "class C:\n" +
    			     "    def m1(self):\n" +
    			     "";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
		strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("        ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    			"class C:\n" +
    			"    pass\n" +
    			"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("    ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab3() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"        print 1\n" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("        ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testWithoutSmartIndent() {
    	final TestIndentPrefs prefs = new TestIndentPrefs(true, 4);
    	prefs.smartIndentAfterPar = false;
		strategy.setIndentPrefs(prefs);
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\n");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("\n        ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab4() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"        print 'a'\n" +
    	"        " + //now, a 'regular' tab should happen
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("    ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab5() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"        print 'a'\n" +
    	"       " + //now, only 1 space is missing to the correct indent
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals(" ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab6() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"print 'a'" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"print 'a'".length(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("        ", docCmd.text); // a single tab should go to the correct indent
    }
    
    public void testIndentingWithTab7() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"  print 'a'" +
    	"";
    	String expected = "" +
    	"class C:\n" +
    	"    def m1(self):            \n" +
    	"print 'a'" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-"  print 'a'".length(), 0, "\t");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("        ", docCmd.text); // a single tab should go to the correct indent
    	assertEquals(expected, doc.get()); // the spaces after the indent should be removed
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

        doc = "class c: #some comment";
        docCmd = new DocCmd(doc.length(), 0, "\n");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "\n" +
                   "    ";
        assertEquals(expected, docCmd.text);
    }
    
    public void testCommentsIndent2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
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
    
    public void testIndentLevel3() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	
    	String doc = "" +
		"a = (1, \n" +
		"  2,"; //should keep this indent, and not go to the opening bracket indent.
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n  ";
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
    
    public void testAfterCloseParOnlyIndent() {
    	final TestIndentPrefs prefs = new TestIndentPrefs(true, 4);
		strategy.setIndentPrefs(prefs);
		prefs.indentToParLevel = false;
    	String doc = "m = [a,";
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n" +
    	"    ";
    	assertEquals(expected, docCmd.text);
    	
    }
    
    public void testAfterCloseParOnlyIndent2() {
    	final TestIndentPrefs prefs = new TestIndentPrefs(true, 4);
    	strategy.setIndentPrefs(prefs);
    	prefs.indentToParLevel = false;
    	String doc = "" +
    			"class A:\n" +
    			"    def m1(a,";
    	DocCmd docCmd = new DocCmd(doc.length(), 0, "\n");
    	strategy.customizeDocumentCommand(new Document(doc), docCmd);
    	String expected = "\n" +
    	"        ";
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
    
    public void testAutoCls() {
        TestIndentPrefs testIndentPrefs = new TestIndentPrefs(false, 4, true);
        strategy.setIndentPrefs(testIndentPrefs);
        String doc = null;
        DocCmd docCmd = null;
        String expected = null;
        
        doc = 
            "class c:\n" +
            "    @classmethod\n" +
            "    def met";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "(cls):";
        assertEquals(expected, docCmd.text);
        
    }
    
    public void testNoAutoSelf() {
        TestIndentPrefs testIndentPrefs = new TestIndentPrefs(false, 4, true);
        strategy.setIndentPrefs(testIndentPrefs);
        String doc = null;
        DocCmd docCmd = null;
        String expected = null;
        
        doc = 
            "class c:\n" +
            "    def met(self):\n" +
            "        def inner";
        docCmd = new DocCmd(doc.length(), 0, "(");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = "():";
        assertEquals(expected, docCmd.text);
    }
    
    public void testNoAutoSelf2() {
        TestIndentPrefs testIndentPrefs = new TestIndentPrefs(false, 4, true);
        strategy.setIndentPrefs(testIndentPrefs);
        String doc = null;
        DocCmd docCmd = null;
        String expected = null;
        
        doc = 
            "class c:\n" +
            "    @staticmethod\n" +
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
    
    public void testParens() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "isShown() #suite()" +
    	"";
    	final Document doc = new Document(str);
    	DocCmd docCmd = new DocCmd(doc.getLength()-") #suite()".length(), 0, ")");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("", docCmd.text); 
    	assertEquals(9, docCmd.caretOffset);
    	
    }
    
    public void testParens2() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "isShown() #suite()'" +
    	"";
    	final Document doc = new Document(str);
    	int offset = doc.getLength()-") #suite()'".length();
		DocCmd docCmd = new DocCmd(offset, 0, ")");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("", docCmd.text); 
    	assertEquals(offset+1, docCmd.caretOffset);
    	
    }
    
    public void testParens3() {
    	strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
    	String str = "assert_('\\' in txt) a()";
    	final Document doc = new Document(str);
    	int offset = doc.getLength()-")".length();
		DocCmd docCmd = new DocCmd(offset, 0, ")");
    	strategy.customizeDocumentCommand(doc, docCmd);
    	assertEquals("", docCmd.text); 
    	assertEquals(offset+1, docCmd.caretOffset);
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
    
    public void testTryExceptDedent() {
        //first part of test - simple case
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        String strDoc = "try:\n" +
        "    print a\n" +
        "    except";
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
                "except",
                doc.get());
        
        //second part of test - should also dedent
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4, true));
        strDoc = 
            "try:\n" +
            "    if somethingElse:" +
            "        print a\n" +
            "    except";
        initialOffset = strDoc.length();
        docCmd = new DocCmd(initialOffset, 0, ":");
        doc = new Document(strDoc);
        strategy.customizeDocumentCommand(doc, docCmd);
        expected = ":";
        assertEquals(expected, docCmd.text);
        assertEquals(docCmd.offset, initialOffset-4);
        assertEquals(
                "try:\n" +
                "    if somethingElse:" +
                "        print a\n" +
                "except",
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
        
        doc = "from_xxx";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "from importer";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " import ";
        assertEquals(expected, docCmd.text);
        
        doc = "from importer";
        docCmd = new DocCmd(doc.length()-2, 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " ";
        assertEquals(expected, docCmd.text);
        
        doc = "from myimporter";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " import ";
        assertEquals(expected, docCmd.text);
        
        doc = "from myimport";
        docCmd = new DocCmd(doc.length(), 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " import ";
        assertEquals(expected, docCmd.text);
        
        doc = "from xxx #import yyy";
        docCmd = new DocCmd(8, 0, " ");
        strategy.customizeDocumentCommand(new Document(doc), docCmd);
        expected = " import ";
        assertEquals(expected, docCmd.text);
        
    }

    public static final class TestIndentPrefs extends AbstractIndentPrefs {
        
        private boolean useSpaces;
        private int tabWidth;
        boolean autoPar = true;
        boolean autoColon = true;
        boolean autoBraces = true;
        boolean autoWriteImport = true;
        boolean smartIndentAfterPar = true;
        boolean autoAddSelf = true;
        boolean autoElse;
		boolean indentToParLevel = true;

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

		public boolean getIndentToParLevel() {
			return indentToParLevel;
		}

		public void regenerateIndentString() {
			//ignore it
		}

    }

}
