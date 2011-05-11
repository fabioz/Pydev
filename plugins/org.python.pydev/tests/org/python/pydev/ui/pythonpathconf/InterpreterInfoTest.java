/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterInfoTest extends TestCase {

    public static void main(String[] args) {
        
        InterpreterInfoTest test = new InterpreterInfoTest();
        try {
            test.setUp();
            test.testInfo3();
            test.tearDown();
            junit.textui.TestRunner.run(InterpreterInfoTest.class);
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
        String s = "EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| \n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        
        //with the version 2.4
        s = "Version2.4EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| \n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        
        //with the version 2.5
        s = "Version2.5EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| \n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals("Version2.5Executable:C:\\bin\\Python24\\python.exe|c:\\bin\\python24\\lib\\lib-tk@$|__builtin__|__main__|_bisect", 
                InterpreterInfo.fromString(s, false).toString());
        
        assertEquals("Version2.5Executable:C:\\bin\\Python24\\python.exe|c:\\bin\\python24\\lib\\lib-tk@$|__builtin__|__main__|_bisect", 
                InterpreterInfo.fromString(s, false).toString());
        
        s = "Name:MyInterpreter:EndName:Version2.4EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| \n@\n$\n| __builtin__| __main__\n| _bisect\n";
        info8.setName("MyInterpreter");
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        assertTrue(info8.toString().startsWith("Name:MyInterpreter:EndName:"));
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
        info7.addPredefinedCompletionsPath("c:\\temp");
        
        assertEquals(info, info2);
        assertFalse(info.equals(info3));
        assertFalse(info.equals(info4));
        assertTrue(info4.equals(info5)); //dlls do not make a difference anymore
        assertFalse(info4.equals(info6));
        assertFalse(info5.equals(info6));
        assertEquals(info6, info6);
        
        String toString1 = info.toString();
        assertEquals(info, InterpreterInfo.fromString(toString1, false));
        
        String toString4 = info4.toString();
        assertEquals(info4, InterpreterInfo.fromString(toString4, false));
        
        String toString5 = info5.toString();
        assertEquals(info5, InterpreterInfo.fromString(toString5, false));
        
        String toString6 = info6.toString();
        assertEquals(info6, InterpreterInfo.fromString(toString6, false));
        
        String toString7 = info7.toString();
        assertEquals(info7, InterpreterInfo.fromString(toString7, false));
        
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
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        
    }
    
    private void compareArray(String []a, String[] b) {
        if(!Arrays.equals(a, b)){
            fail(Arrays.asList(a)+" != "+ Arrays.asList(b));
        }
    }
    
    public void testInfo3() throws Exception {
        InterpreterInfo info = new InterpreterInfo("2.5", "c:\\bin\\python.exe", new ArrayList<String>());
        
        info.setEnvVariables(new String[]{"PATH=c:\\bin;d:\\bin", "LIBPATH=k:\\foo"});
        
        Properties stringSubstitutionOriginal = new Properties();
        stringSubstitutionOriginal.setProperty("my_prop", "prop_val");
        info.setStringSubstitutionVariables(stringSubstitutionOriginal);

        
        String string = info.toString();
        InterpreterInfo newInfo = InterpreterInfo.fromString(string, false);
        assertEquals(info.getStringSubstitutionVariables(), newInfo.getStringSubstitutionVariables());
        assertEquals(info, newInfo);
        assertEquals(newInfo, info);
        compareArray(info.getEnvVariables(), newInfo.getEnvVariables());
        newInfo.setEnvVariables(null);
        newInfo.setStringSubstitutionVariables(null);
        assertFalse(info.equals(newInfo));
        assertFalse(newInfo.equals(info));
        
        assertEquals(newInfo, InterpreterInfo.fromString(newInfo.toString(), false));
    }
    
    public void testInfo4() throws Exception {
        InterpreterInfo info = new InterpreterInfo("2.5", "c:\\bin\\python.exe", new ArrayList<String>());
        String[] original1 = new String[]{"LIBPATH=k:\\foo", "PATH=c:\\bin;d:\\bin"};
        info.setEnvVariables(original1);
        
        compareArray(info.updateEnv(null), original1);
        
        compareArray(info.updateEnv(new String[0]), original1);
        
        String[] original2 = new String[]{"LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin2"};
        String[] expected2 = new String[]{"LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin"};
        assertEquals(new HashSet<String>(Arrays.asList(info.updateEnv(original2))), new HashSet<String>(Arrays.asList(expected2)));
    }
    
    
    public void testInfoIgnoreDjangoForcedBuiltin() throws Exception {
        List<String> l1 = new ArrayList<String>();
        List<String> l2 = new ArrayList<String>();
        List<String> lForcedBuiltins = new ArrayList<String>();
        lForcedBuiltins.add("__builtin__");
        lForcedBuiltins.add("__main__");
        lForcedBuiltins.add("_bisect");
        lForcedBuiltins.add("django");
        lForcedBuiltins.add("django.db");
        InterpreterInfo info = new InterpreterInfo("2.4", "C:\\bin\\Python24\\python.exe", l1, l2, lForcedBuiltins);
        List<String> asList = Arrays.asList(info.getBuiltins());
        assertTrue(!asList.contains("django"));
        assertTrue(!asList.contains("django.db"));
    }
    
    
}
