/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterInfoTest extends TestCase {

    public static void main(String[] args) {

        InterpreterInfoTest test = new InterpreterInfoTest();
        try {
            test.setUp();
            test.testInterpreterInfoOutputWithEncoding();
            test.tearDown();
            junit.textui.TestRunner.run(InterpreterInfoTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
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
        assertEquals(
                "Version2.5Executable:C:\\bin\\Python24\\python.exe|c:\\bin\\python24\\lib\\lib-tk@$|__builtin__|__main__|_bisect",
                InterpreterInfo.fromString(s, false).toStringOld());

        assertEquals(
                "Version2.5Executable:C:\\bin\\Python24\\python.exe|c:\\bin\\python24\\lib\\lib-tk@$|__builtin__|__main__|_bisect",
                InterpreterInfo.fromString(s, false).toStringOld());

        s = "Name:MyInterpreter:EndName:Version2.4EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tkINS_PATH\n| \n@\n$\n| __builtin__| __main__\n| _bisect\n";
        info8.setName("MyInterpreter");
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        assertTrue(info8.toStringOld().startsWith("Name:MyInterpreter:EndName:"));
        check(info8);
    }

    /**
     * 
     */
    public void testInfo() {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("2.4", "test", l);
        InterpreterInfo info2 = new InterpreterInfo("2.4", "test", l);
        InterpreterInfo info3 = new InterpreterInfo("2.4", "test3", l);
        List<String> l4 = new ArrayList<String>();
        l4.add("l4");
        InterpreterInfo info4 = new InterpreterInfo("2.4", "test", l4);

        List<String> dlls = new ArrayList<String>();
        dlls.add("dll1");
        InterpreterInfo info5 = new InterpreterInfo("2.4", "test", l4, dlls);

        List<String> forced = new ArrayList<String>();
        forced.add("forced1");
        InterpreterInfo info6 = new InterpreterInfo("2.4", "test", l4, dlls, forced);

        InterpreterInfo info7 = new InterpreterInfo("2.4", "test", new ArrayList<String>(), new ArrayList<String>(),
                forced);
        info7.addPredefinedCompletionsPath("c:\\temp");

        assertEquals(info, info2);
        assertFalse(info.equals(info3));
        assertFalse(info.equals(info4));
        assertTrue(info4.equals(info5)); //dlls do not make a difference anymore
        assertFalse(info4.equals(info6));
        assertFalse(info5.equals(info6));
        assertEquals(info6, info6);

        check(info);
        check(info2);
        check(info3);
        check(info4);
        check(info5);
        check(info6);
        check(info7);

        List<String> l1 = new ArrayList<String>();
        l1.add("c:\\bin\\python24\\lib\\lib-tk");
        l1.add("c:\\bin\\python24");
        List<String> l2 = new ArrayList<String>();
        List<String> l3 = new ArrayList<String>();
        l3.add("__builtin__");
        l3.add("__main__");
        l3.add("_bisect");
        InterpreterInfo info8 = new InterpreterInfo("2.4", "C:\\bin\\Python24\\python.exe", l1, l2, l3);

        String s = "EXECUTABLE:C:\\bin\\Python24\\python.exe|| c:\\bin\\python24\\lib\\lib-tk\n| c:\\bin\\python24\n@\n$\n| __builtin__| __main__\n| _bisect\n";
        assertEquals(info8, InterpreterInfo.fromString(s, false));
        check(info8);

    }

    public void testSeparatorChars() {
        List<String> l1 = new ArrayList<String>();
        l1.add("c:\\bin\\pyth&on24\\lib\\lib-tk");
        l1.add("c:\\bin\\pyth&on24");
        List<String> l2 = new ArrayList<String>();
        List<String> l3 = new ArrayList<String>();
        l3.add("__builtin__");
        l3.add("__main__");
        l3.add("_bisect");
        InterpreterInfo info = new InterpreterInfo("2.4", "C:\\bin\\Python24\\python.exe", l1, l2, l3);
        String string = info.toString();
        assertEquals(info, InterpreterInfo.fromString(string, false));
    }

    /**
     * @param info
     */
    private void check(final InterpreterInfo info) {
        String toStringOld = info.toStringOld();
        InterpreterInfo obtained = InterpreterInfo.fromString(toStringOld, false);
        assertEquals(info, obtained);
        assertEquals(obtained, info);

        String toString1 = info.toString();
        //        System.out.println("\n\n");
        //        System.out.println(toString1);
        //        System.out.println("\n\n");
        obtained = InterpreterInfo.fromString(toString1, false);

        //        System.out.println("\n\n");
        //        System.out.println(obtained);
        //        System.out.println("\n\n");
        assertEquals(info, obtained);
        assertEquals(obtained, info);
    }

    /**
     * Compare whether the environment in envA is the same as envB
     * @param envA
     * @param envB
     */
    private void compareEnvironments(String[] envA, String[] envB) {
        assertEquals(new HashSet<String>(Arrays.asList(envA)), new HashSet<String>(Arrays.asList(envB)));
    }

    public void testInfo3() throws Exception {
        InterpreterInfo info = new InterpreterInfo("2.5", "c:\\bin\\python.exe", new ArrayList<String>());

        info.setEnvVariables(new String[] { "PATH=c:\\bin;d:\\bin", "LIBPATH=k:\\foo" });

        Properties stringSubstitutionOriginal = new Properties();
        stringSubstitutionOriginal.setProperty("my_prop", "prop_val");
        info.setStringSubstitutionVariables(stringSubstitutionOriginal);

        String string = info.toStringOld();
        InterpreterInfo newInfo = InterpreterInfo.fromString(string, false);
        assertEquals(info.getStringSubstitutionVariables(), newInfo.getStringSubstitutionVariables());
        assertEquals(info, newInfo);
        assertEquals(newInfo, info);
        compareEnvironments(info.getEnvVariables(), newInfo.getEnvVariables());
        newInfo.setEnvVariables(null);
        newInfo.setStringSubstitutionVariables(null);
        assertFalse(info.equals(newInfo));
        assertFalse(newInfo.equals(info));

        assertEquals(newInfo, InterpreterInfo.fromString(newInfo.toStringOld(), false));
        check(info);
    }

    public void testInfo4() throws Exception {
        InterpreterInfo info = new InterpreterInfo("2.5", "c:\\bin\\python.exe", new ArrayList<String>());
        String[] original1 = new String[] { "LIBPATH=k:\\foo", "PATH=c:\\bin;d:\\bin" };
        info.setEnvVariables(original1);

        compareEnvironments(info.updateEnv(null), original1);

        compareEnvironments(info.updateEnv(new String[0]), original1);

        String[] original2 = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin2" };
        String[] expected2 = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin" };
        compareEnvironments(info.updateEnv(original2), expected2);
        check(info);
    }

    public void testVariableExpansion() throws Exception {
        InterpreterInfo info = new InterpreterInfo("2.5", "c:\\bin\\python.exe", new ArrayList<String>());
        MockStringVariableManager manager = new MockStringVariableManager();
        manager.addMockVariable("var1", "value1");
        info.stringVariableManagerForTests = manager;
        String[] variableInInfo = new String[] { "LIBPATH=k:\\foo", "PATH=c:\\bin;d:\\bin", "ENV1=${var1}" };
        info.setEnvVariables(variableInInfo);

        // echeck expected output when no environment is being added to
        String[] expected1 = new String[] { "LIBPATH=k:\\foo", "PATH=c:\\bin;d:\\bin", "ENV1=value1" };
        compareEnvironments(info.updateEnv(null), expected1);
        compareEnvironments(info.updateEnv(new String[0]), expected1);

        // check expected output when there is an input environment
        String[] inputEnv = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin2",
                "ENV1=some_other_value" };
        String[] expected2 = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin", "ENV1=value1" };
        compareEnvironments(info.updateEnv(inputEnv), expected2);

        // make sure that variables in the input (system) environment are not expanded
        String[] original3 = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin2", "ENV2=${var1}" };
        String[] expected3 = new String[] { "LIBPATH=k:\\foo", "boo=boo", "PATH=c:\\bin;d:\\bin", "ENV2=${var1}",
                "ENV1=value1" };
        compareEnvironments(info.updateEnv(original3), expected3);
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

        check(info);
    }

    public void testInterpreterInfoOutputWithEncoding() throws Exception {
        //To generate output:

        //cd W:/pydev/plugins/org.python.pydev/tests/org/python/pydev/ui/pythonpathconf
        //"d:\instaçao âo\Python27\python.exe"  W:\pydev\plugins\org.python.pydev\pysrc\interpreterInfo.py > InterpreterInfoOutput.txt

        String contents = (String) FileUtils.getFileContentsCustom(new File(TestDependent.TEST_PYDEV_PLUGIN_LOC
                + "tests/org/python/pydev/ui/pythonpathconf/InterpreterInfoOutput.txt"), "utf-8", String.class);
        InterpreterInfo i1 = InterpreterInfo.fromString(contents, false);
        InterpreterInfo i2 = i1.makeCopy();
        assertEquals(i1, i2);
    }

    public void testInterpreterInfoOutputWithGarbageBeforeAfterXML() throws Exception {
        //To generate output:

        //cd W:/pydev/plugins/org.python.pydev/tests/org/python/pydev/ui/pythonpathconf
        //"d:\instaçao âo\Python27\python.exe"  W:\pydev\plugins\org.python.pydev\pysrc\interpreterInfo.py > InterpreterInfoOutput.txt

        String contents = (String) FileUtils.getFileContentsCustom(new File(TestDependent.TEST_PYDEV_PLUGIN_LOC
                + "tests/org/python/pydev/ui/pythonpathconf/InterpreterInfoOutput.txt"), "utf-8", String.class);
        InterpreterInfo i1 = InterpreterInfo
                .fromString("Some random string before" + contents + " random after", false);
        InterpreterInfo i2 = InterpreterInfo.fromString(contents, false);
        assertEquals(i1, i2);
    }

}
