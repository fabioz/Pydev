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

    public void testInfo2() {
        List<String> l1 = new ArrayList<String>();
        l1.add("c:\\bin\\python24\\lib\\lib-tk");
        List<String> l2 = new ArrayList<String>();
        List<String> l3 = new ArrayList<String>();
        l3.add("__builtin__");
        l3.add("__main__");
        l3.add("_bisect");
        InterpreterInfo info8 = new InterpreterInfo("2.4", "C:\\bin\\Python24\\python.exe", l1, l2, l3);
        
        //without the version
        String s = "EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| c:\\bin\\python24OUT_PATH\n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        
        //with the version 2.4
        s = "Version2.4EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| c:\\bin\\python24OUT_PATH\n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        
        //with the version 2.5
        s = "Version2.5EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| c:\\bin\\python24OUT_PATH\n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals("Version2.5Executable:C:\\bin\\Python24\\python.exe|c:\\bin\\python24\\lib\\lib-tk@$|__builtin__|__main__|_bisect", 
                InterpreterInfo.fromString(s, false).toString());
    }
    
    /**
     * 
     */
    public void testInfo() {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("2.4","test", l);
        InterpreterInfo info2 = new InterpreterInfo("2.4","test", l);
        InterpreterInfo info3 = new InterpreterInfo("2.4","test3", l);
        List<String> l4 = new ArrayList<String>();
        l4.add("l4");
        InterpreterInfo info4 = new InterpreterInfo("2.4","test", l4);
        
        List<String> dlls = new ArrayList<String>();
        dlls.add("dll1");
        InterpreterInfo info5 = new InterpreterInfo("2.4","test", l4, dlls);
        
        List<String> forced = new ArrayList<String>();
        forced.add("forced1");
        InterpreterInfo info6 = new InterpreterInfo("2.4","test", l4, dlls, forced);
        
        InterpreterInfo info7 = new InterpreterInfo("2.4","test", new ArrayList(), new ArrayList(), forced);
        
        assertEquals(info, info2);
        assertFalse(info.equals(info3));
        assertFalse(info.equals(info4));
        assertTrue(info4.equals(info5)); //dlls do not make a difference anymore
        assertFalse(info4.equals(info6));
        assertFalse(info5.equals(info6));
        assertEquals(info6, info6);
        
        String toString1 = info.toString();
        assertEquals(info, InterpreterInfo.fromString(toString1));
        
        String toString4 = info4.toString();
        assertEquals(info4, InterpreterInfo.fromString(toString4));
        
        String toString5 = info5.toString();
        assertEquals(info5, InterpreterInfo.fromString(toString5));
        
        String toString6 = info6.toString();
        assertEquals(info6, InterpreterInfo.fromString(toString6));
        
        String toString7 = info7.toString();
        assertEquals(info7, InterpreterInfo.fromString(toString7));
        
        List<String> l1 = new ArrayList<String>();
        l1.add("c:\\bin\\python24\\lib\\lib-tk");
        l1.add("c:\\bin\\python24");
        List<String> l2 = new ArrayList<String>();
        List<String> l3 = new ArrayList<String>();
        l3.add("__builtin__");
        l3.add("__main__");
        l3.add("_bisect");
        InterpreterInfo info8 = new InterpreterInfo("2.4","C:\\bin\\Python24\\python.exe", l1, l2, l3);
        
        String s = "EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tk\n| c:\\bin\\python24\n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s));
        
    }
}
