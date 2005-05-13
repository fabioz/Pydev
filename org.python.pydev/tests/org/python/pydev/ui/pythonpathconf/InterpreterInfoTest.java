/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterInfoTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterpreterInfoTest.class);
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
     * 
     */
    public void testInfo() {
        List l = new ArrayList();
        InterpreterInfo info = new InterpreterInfo("test", l);
        InterpreterInfo info2 = new InterpreterInfo("test", l);
        InterpreterInfo info3 = new InterpreterInfo("test3", l);
        List l4 = new ArrayList();
        l4.add("l4");
        InterpreterInfo info4 = new InterpreterInfo("test", l4);
        
        assertEquals(info, info2);
        assertFalse(info.equals(info3));
        assertFalse(info.equals(info4));
        
        assertEquals(info, InterpreterInfo.fromString(info.toString()));
        
    }
}
