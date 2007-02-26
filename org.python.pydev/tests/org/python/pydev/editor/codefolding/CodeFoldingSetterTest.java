package org.python.pydev.editor.codefolding;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

public class CodeFoldingSetterTest extends TestCase {

    public static boolean DEBUG = false;

    public static void main(String[] args) {

        try {
            DEBUG = true;
            CodeFoldingSetterTest test = new CodeFoldingSetterTest();
            test.setUp();
            test.testMarks();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(CodeFoldingSetterTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }
    
    
    public void testMarks2() throws Exception {
        Document doc = new Document("" +
                "class TestCase(unittest.TestCase):\n" +
                "    \n" +
                "    def setUp(self):\n" +
                "        unittest.TestCase.setUp(self)\n" +
                "        return 1\n" +
                "\n" +
                "\n");
        
        List<FoldingEntry> marks = getMarks(doc);
        
        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 0, 5, null),it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 2, 5, null),it.next());
        assertTrue(it.hasNext()==false);
        
    }


    private List<FoldingEntry> getMarks(Document doc) {
        Tuple<SimpleNode, Throwable> r = PyParser.reparseDocument(new PyParser.ParserInfo(doc, false, IPythonNature.LATEST_GRAMMAR_VERSION));
        List<FoldingEntry> marks = CodeFoldingSetter.getMarks(doc, r.o1);
        if(DEBUG){
            for (FoldingEntry entry : marks) {
                System.out.println(entry);
            }
        }
        return marks;
    }
    
    public void testMarks() throws Exception {
        Document doc = new Document("" +
                "import foo\n" +
                "from foo import (x,\n" +
                "                 y,\n" +
                "                 )\n" +
                "import foo\n" +
                "#comment1\n" +
                "#comment2\n" +
                "#comment3\n" +
                "def m1():\n" +
                "    '''start\n" +
                "    end'''" +
                "");
        
        List<FoldingEntry> marks = getMarks(doc);
        
        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_IMPORT, 0, 5, null),it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_COMMENT, 5, 8, null),it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 8, 11, null),it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STR, 9, 11, null),it.next());
        assertTrue(it.hasNext()==false);
    }
}
