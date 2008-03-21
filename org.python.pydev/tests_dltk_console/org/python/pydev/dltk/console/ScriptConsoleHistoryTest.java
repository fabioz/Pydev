package org.python.pydev.dltk.console;

import junit.framework.TestCase;

public class ScriptConsoleHistoryTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testScriptConsole() throws Exception {
        ScriptConsoleHistory c = new ScriptConsoleHistory();
        assertFalse(c.prev());
        assertFalse(c.next());
        
        c.update("test");
        
        assertEquals("test", c.get());
        assertFalse(c.prev());
        assertEquals("test", c.get());
        assertFalse(c.next());
        assertEquals("test", c.get());
        
        c.commit();
        assertEquals("", c.get());
        assertTrue(c.prev());
        assertEquals("test", c.get());
        assertFalse(c.prev());
        assertEquals("test", c.get());
        assertTrue(c.next());
        assertEquals("", c.get());
        assertTrue(c.prev());
        assertEquals("test", c.get());
        
        c.update("kkk");
        c.commit();

        assertEquals("test\nkkk\n" , c.getAsDoc().get());
        assertFalse(c.next());
        assertEquals("", c.get());
        assertTrue(c.prev());
        assertEquals("kkk", c.get());
        assertTrue(c.prev());
        assertEquals("test", c.get());
        
        c.update("");
        c.commit();
        assertEquals("test\nkkk\n\n" , c.getAsDoc().get());
        assertTrue(c.prev());
        assertEquals("kkk", c.get());
    }
}
