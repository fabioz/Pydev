/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.docutils.PySelection;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PySelectionTest extends TestCase {

    private PySelection ps;
    private Document doc;
    private String docContents;

    public static void main(String[] args) {
        try {
            PySelectionTest test = new PySelectionTest();
            test.setUp();
            test.testGetLineWithoutComments();
            test.tearDown();
            
            junit.textui.TestRunner.run(PySelectionTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        docContents = "" +
		"TestLine1\n"+
		"TestLine2#comm2\n"+
		"TestLine3#comm3\n"+
		"TestLine4#comm4\n";
        doc = new Document(docContents);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws BadLocationException
     * 
     */
    public void testGeneral() throws BadLocationException {
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        assertEquals("TestLine1",ps.getCursorLineContents());
        assertEquals("",ps.getLineContentsToCursor());
        ps.selectCompleteLine();
        
        assertEquals("TestLine1",ps.getCursorLineContents());
        assertEquals("TestLine1",ps.getLine(0));
        assertEquals("TestLine2#comm2",ps.getLine(1));
        
        ps.deleteLine(0);
        assertEquals("TestLine2#comm2",ps.getLine(0));
        ps.addLine("TestLine1", 0);
        
    }
    
    public void testImportLine() {
        String strDoc = "" +
        "#coding                   \n"+
        "''' this should be ignored\n"+
        "from xxx import yyy       \n"+
        "import www'''             \n"+
        "#we want the import to appear in this line\n"+
        "Class C:                  \n"+
        "    pass                  \n"+
        "import kkk                \n"+
        "\n"+
        "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(4, selection.getLineAvailableForImport());
    }

    public void testImportLine2() {
        String strDoc = "" +
        "#coding                   \n"+
        "#we want the import to appear after this line\n"+
        "Class C:                  \n"+
        "    pass                  \n"+
        "import kkk                \n"+
        "\n"+
        "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(2, selection.getLineAvailableForImport());
    }
    
    public void testImportLine3() {
        String strDoc = "" +
        "#coding                   \n"+
        "#we want the import to appear after this line\n"+
        "Class C:                  \n"+
        "    pass                  \n"+
        "import kkk                \n"+
        "                          \n"+
        "''' this should be ignored\n"+
        "from xxx import yyy       \n"+
        "import www'''             \n"+
        "\n"+
        "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(2, selection.getLineAvailableForImport());
    }

    
    
    public void testImportLine4() {
    	String strDoc = "" +
		"class SomeClass( object ):\n"+
		"    '''This is the data that should be set...\n"+
		"    '''\n"+
		"\n"+
		"\n";
    	Document document = new Document(strDoc);
    	PySelection selection = new PySelection(document);
    	assertEquals(0, selection.getLineAvailableForImport());
    }
    
    public void testImportLine5() {
    	String strDoc = "" +
    	"'''This is the data that should be set...\n"+
    	"'''\n"+
    	"\n"+
    	"\n";
    	Document document = new Document(strDoc);
    	PySelection selection = new PySelection(document);
    	assertEquals(2, selection.getLineAvailableForImport());
    }
    
    
    public void testSelectAll() {
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        ps.selectAll(true);
        assertEquals(docContents, ps.getCursorLineContents()+"\n");
        assertEquals(docContents, ps.getSelectedText());
        
        ps = new PySelection(doc, new TextSelection(doc, 0,9)); //first line selected
        ps.selectAll(true); //changes
        assertEquals(docContents, ps.getCursorLineContents()+"\n");
        assertEquals(docContents, ps.getSelectedText());
        
        ps = new PySelection(doc, new TextSelection(doc, 0,9)); //first line selected
        ps.selectAll(false); //nothing changes
        assertEquals(ps.getLine(0), ps.getCursorLineContents());
        assertEquals(ps.getLine(0), ps.getSelectedText());
    }
    
    public void testFullRep() throws Exception {
        String s = "v=aa.bb.cc()";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 2,2));
        assertEquals("aa.bb.cc", ps.getFullRepAfterSelection());

        s = "v=aa.bb.cc";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 2,2));
        assertEquals("aa.bb.cc", ps.getFullRepAfterSelection());
        
        
    }
    
    public void testReplaceToSelection() throws Exception {
        String s = "vvvvppppaaaa";
        doc = new Document(s);
        ps = new PySelection(doc, 4);
        ps.replaceLineContentsToSelection("xxxx");
        assertEquals("xxxxppppaaaa", ps.getDoc().get());
    }
    
    
    public void testGetInsideParentesis() throws Exception {
        String s = "def m1(self, a, b)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        List<String> insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(2, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));
        
        
        s = "def m1(self, a, b=None)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        insideParentesisToks = ps.getInsideParentesisToks(true).o1;
        assertEquals(3, insideParentesisToks.size());
        assertEquals("self", insideParentesisToks.get(0));
        assertEquals("a", insideParentesisToks.get(1));
        assertEquals("b", insideParentesisToks.get(2));
        

        s = "def m1(self, a, b=None)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(2, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));
        
        s = "def m1(self, a, (b,c) )";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(3, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));
        assertEquals("c", insideParentesisToks.get(2));
        
        s = "def m1(self, a, b, \nc,\nd )";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(4, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));
        assertEquals("c", insideParentesisToks.get(2));
        assertEquals("d", insideParentesisToks.get(3));
        
        
    }
    
    public void testRemoveEndingComments() throws Exception {
        String s = "class Foo:pass\n" +
                "#comm1\n" +
                "#comm2\n" +
                "print 'no comm'\n" +
                "#comm3\n" +
                "#comm4";
        doc = new Document(s);
        StringBuffer buffer = PySelection.removeEndingComments(doc);
        
        assertEquals("\n#comm3\n" +
                "#comm4", buffer.toString());
        assertEquals("class Foo:pass\n" +
                "#comm1\n" +
                "#comm2\n" +
                "print 'no comm'\n", doc.get());
    }
    public void testRemoveEndingComments2() throws Exception {
        String s = "class C: \n" +
        "    pass\n" +
        "#end\n" +
        "";
        doc = new Document(s);
        StringBuffer buffer = PySelection.removeEndingComments(doc);
        
        assertEquals("\n#end\n" , buffer.toString());
        assertEquals("class C: \n" +
                "    pass\n" 
                , doc.get());
    }
    public void testGetLastIf() throws Exception {
        String s = 
            "if False:\n" +
            "    print foo";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("if False:", ps.getPreviousLineThatAcceptsElse());

        s = 
        "while False:\n" +
        "    print foo";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals(null, ps.getPreviousLineThatAcceptsElse());
        
    }
    
    public void testGetLineWithoutComments() {
        String s = 
            "a = 'ethuenoteuho#ueoth'";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("a = ", ps.getLineWithoutCommentsOrLiterals());
    }
}
