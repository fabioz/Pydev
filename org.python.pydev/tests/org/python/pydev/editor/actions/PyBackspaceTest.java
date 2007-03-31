package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyAutoIndentStrategyTest.TestIndentPrefs;

import junit.framework.TestCase;

public class PyBackspaceTest extends TestCase {
    
    private PyBackspace backspace;

    @Override
    protected void setUp() throws Exception {
        this.backspace = new PyBackspace();
        this.backspace.setIndentPrefs(new TestIndentPrefs(true, 4));
    }
    
    public void testBackspace() throws Exception {
        Document doc = new Document("a = 10");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 1", doc.get());
    }
    
    public void testBackspace2() throws Exception {
        Document doc = new Document("a = 10     ");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace3() throws Exception {
        Document doc = new Document("a =  10");        
        PySelection ps = new PySelection(doc, 0, 5, 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace4() throws Exception {
        Document doc = new Document("a =  10");        
        PySelection ps = new PySelection(doc, 0, 3, 2);
        
        backspace.perform(ps);
        assertEquals("a =10", doc.get());
    }
    
    public void testBackspace5() throws Exception {
        Document doc = new Document("a = 10");        
        PySelection ps = new PySelection(doc, 0, 0, 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace6() throws Exception {
        Document doc = new Document("a = 10\r\n");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace7() throws Exception {
        Document doc = new Document("a = 10\n");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace8() throws Exception {
        Document doc = new Document("a = 10\r");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace9() throws Exception {
        Document doc = new Document("a = 10\r");        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }
    
    public void testBackspace10() throws Exception {
        Document doc = new Document(
                "a = 10\n" +
                "    "
                );        
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);
        
        backspace.perform(ps);
        assertEquals("a = 10\n", doc.get());
    }
}
