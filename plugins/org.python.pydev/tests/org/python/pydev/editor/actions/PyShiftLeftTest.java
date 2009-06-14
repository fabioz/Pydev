package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.TestIndentPrefs;

public class PyShiftLeftTest extends TestCase{


    public void testShiftLeft1() throws Exception {
        Document doc = new Document(
                "    def a(aa):\n" +
                "        pass\n" +
                "    \n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        Tuple<Integer, Integer> newSel = new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));
        assertEquals(new Tuple<Integer, Integer>(0, doc.getLength()), newSel);
        
        String expected = "def a(aa):\n" +
                          "    pass\n" +
                          "\n";
        assertEquals(expected, doc.get());
    }
    
    
    public void testShiftLeft2() throws Exception {
        Document doc = new Document(
                "   def a(aa):\n" +
                "        pass\n" +
                "    \n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        Tuple<Integer, Integer> newSel = new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));
        assertEquals(new Tuple<Integer, Integer>(0, doc.getLength()), newSel);
        
        String expected = "def a(aa):\n" +
                          "     pass\n" +
                          " \n";
        assertEquals(expected, doc.get());
    }
    
    public void testShiftLeft3() throws Exception {
        Document doc = new Document(
                "   def a(aa):\n" +
                "        pass\n" +
                "    bb\n");
        PySelection ps = new PySelection(doc, 0, 3, doc.getLength()-2-3);
        Tuple<Integer, Integer> newSel = new PyShiftLeft().perform(ps, new TestIndentPrefs(true, 4));
        assertEquals(new Tuple<Integer, Integer>(0, doc.getLength()-2), newSel);
        
        String expected = "def a(aa):\n" +
                          "     pass\n" +
                          " bb\n";
        assertEquals(expected, doc.get());
    }
    
    
}
