/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.CharArrayReader;
import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathHelperTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            PythonPathHelperTest test = new PythonPathHelperTest();
            test.setUp();
            test.testGetEncoding6();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PythonPathHelperTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String qual = "";
    public String token = "";
    public int line;
    public int col;
    public String sDoc = "";

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    public void testResolvePath() {
        PythonPathHelper helper = new PythonPathHelper();
        String path = TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.TEST_PYSRC_LOC;
        helper.setPythonPath(path);

        IProject project = null;

        assertEquals("unittest", helper.resolveModule(TestDependent.PYTHON_LIB + "unittest.py", project));
        assertEquals("compiler.ast", helper.resolveModule(TestDependent.PYTHON_LIB + "compiler/ast.py", project));

        assertEquals("email", helper.resolveModule(TestDependent.PYTHON_LIB + "email", project));
        assertSame(null, helper.resolveModule(TestDependent.PYTHON_LIB + "curses/invalid", true, project));
        assertSame(null, helper.resolveModule(TestDependent.PYTHON_LIB + "invalid", true, project));

        assertEquals("testlib", helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib", project));
        assertEquals("testlib.__init__",
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib/__init__.py", project));
        assertEquals("testlib.unittest",
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib/unittest", project));
        assertEquals("testlib.unittest.__init__",
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/__init__.py", project));
        assertEquals("testlib.unittest.testcase",
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/testcase.py", project));
        assertEquals(null,
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/invalid.py", true, project));

        assertEquals(
                null,
                helper.resolveModule(TestDependent.TEST_PYSRC_LOC + "extendable/invalid.folder/invalidfile.py", project));
    }

    public void testGetModulesFoundStructure() {
        PythonPathHelper helper = new PythonPathHelper();
        String path = TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.TEST_PYSRC_LOC;
        helper.setPythonPath(path);
        ModulesFoundStructure modulesFoundStructure = helper.getModulesFoundStructure(null, null);
        Map<File, String> regularModules = modulesFoundStructure.regularModules;
        Collection<String> moduleNames = regularModules.values();

        assertFalse(moduleNames.contains("testlib"));
        assertTrue(moduleNames.contains("testlib.__init__"));
        assertFalse(moduleNames.contains("testlib.unittest"));
        assertTrue(moduleNames.contains("testlib.unittest.__init__"));
        assertTrue(moduleNames.contains("testlib.unittest.testcase"));
        assertTrue(moduleNames.contains("testlib.unittest.relative.testrelative"));
    }

    public void testModuleCompletion() {
        token = "unittest";
        line = 3;
        col = 9;

        sDoc = "" + "from testlib import unittest \n" + "                            \n"
                + "unittest.                   \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        assertEquals(13, comps.length);

        ASTManagerTest.assertIsIn("__name__", comps);
        ASTManagerTest.assertIsIn("__file__", comps);
        ASTManagerTest.assertIsIn("__path__", comps);
        ASTManagerTest.assertIsIn("__dict__", comps);
        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
        ASTManagerTest.assertIsIn("anothertest", comps);
        ASTManagerTest.assertIsIn("guitestcase", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
    }

    private IToken[] getComps(Document doc, ICompletionState state) {
        try {
            return nature.getAstManager().getCompletionsForToken(doc, state);
        } catch (CompletionRecursionException e) {
            throw new RuntimeException(e);
        }
    }

    public void testRecursionModuleCompletion() throws CompletionRecursionException {
        token = "";
        line = 2;
        col = 0;

        sDoc = "" + "from testrec.imp1 import * \n" + "                           \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        ICodeCompletionASTManager a = nature.getAstManager();
        comps = a.getCompletionsForToken(doc, state);
        assertFalse(comps.length == 0);

    }

    public void testRecursion2() throws CompletionRecursionException {
        token = "i";
        line = 3;
        col = 2;

        sDoc = "" + "from testrec.imp3 import MethodReturn1 \n" + "i = MethodReturn1()                    \n" + "i.";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        ICodeCompletionASTManager a = nature.getAstManager();
        comps = a.getCompletionsForToken(doc, state);
        assertEquals(0, comps.length);

    }

    public void testClassHierarchyCompletion() {

        token = "TestCase";
        line = 3;
        col = 9;

        sDoc = "" + "from testlib.unittest.testcase import TestCase \n"
                + "                                              \n"
                + "TestCase.                                     \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
    }

    public void testClassHierarchyCompletion2() {

        token = "GUITest";
        line = 3;
        col = 8;

        sDoc = "" + "from testlib.unittest import GUITest  \n" + "                                      \n"
                + "GUITest.                              \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        ASTManagerTest.assertIsIn("SetWidget", comps);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        assertTrue(comps.length > 5);
    }

    public void testClassHierarchyCompletion3() {

        token = "AnotherTest";
        line = 3;
        col = 12;

        sDoc = "" + "from testlib.unittest import AnotherTest  \n" + "                                          \n"
                + "AnotherTest.                              \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        ASTManagerTest.assertIsIn("another", comps);
    }

    public void testImportAs() {
        token = "t";
        line = 3;
        col = 2;

        sDoc = "" + "from testlib import unittest as t \n" + "                                  \n"
                + "t.                                \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        assertEquals(13, comps.length);

        ASTManagerTest.assertIsIn("__name__", comps);
        ASTManagerTest.assertIsIn("__file__", comps);
        ASTManagerTest.assertIsIn("__path__", comps);
        ASTManagerTest.assertIsIn("__dict__", comps);
        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
        ASTManagerTest.assertIsIn("anothertest", comps);
        ASTManagerTest.assertIsIn("guitestcase", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
    }

    public void testImportAs2() {
        token = "t";
        line = 3;
        col = 2;

        sDoc = "" + "from testlib.unittest import AnotherTest as t \n"
                + "                                              \n"
                + "t.                                            \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        ASTManagerTest.assertIsIn("another", comps);

    }

    public void testRelativeImport() {
        token = "Derived";
        line = 3;
        col = 8;

        sDoc = "" + "from testlib.unittest.relative.testrelative import Derived \n"
                + "                                                            \n"
                + "Derived.                                                    \n";

        IToken[] comps = null;
        Document doc = new Document(sDoc);
        ICompletionState state = new CompletionState(line, col, token, nature, "");
        comps = getComps(doc, state);
        ASTManagerTest.assertIsIn("test1", comps);
        ASTManagerTest.assertIsIn("test2", comps);
        assertEquals(2, comps.length);

    }

    public void testGetEncoding2() {
        String s = "" + "#test.py\n" + "# handles encoding and decoding of xmlBlaster socket protocol \n" + "\n" + "\n"
                + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals(null, encoding);
    }

    public void testGetEncoding3() {
        //silent it in the tests
        FileUtils.LOG_ENCODING_ERROR = false;
        try {
            String s = "" + "#coding: foo_1\n" + //not valid encoding... will show in log but will not throw error
                    "# handles encoding and decoding of xmlBlaster socket protocol \n" + "\n" + "\n" + "";
            CharArrayReader reader = new CharArrayReader(s.toCharArray());
            String encoding = FileUtils.getPythonFileEncoding(reader, null);
            assertEquals(null, encoding);
        } finally {
            FileUtils.LOG_ENCODING_ERROR = true;
        }
    }

    public void testGetEncoding4() {
        String s = "" + "#coding: utf-8\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("utf-8", encoding);
    }

    public void testGetEncoding5() {
        String s = "" + "#-*- coding: utf-8; -*-\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("utf-8", encoding);
    }

    public void testGetEncoding6() {
        String s = "" + "#coding: utf-8;\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("utf-8", encoding);
    }

    public void testGetEncoding7() {
        String s = "" + "#coding: utf8;\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("utf8", encoding);
    }

    public void testGetEncoding8() {
        String s = "" + "#coding: iso-latin-1-unix;\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("latin1", encoding);
    }

    public void testGetEncoding9() {
        String s = "" + "#coding: latin-1\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("latin1", encoding);
    }

    public void testGetEncoding10() {
        String s = "" + "#coding: latin1\n" + "\n" + "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        String encoding = FileUtils.getPythonFileEncoding(reader, null);
        assertEquals("latin1", encoding);
    }

    public void testGetEncoding() {
        String loc = TestDependent.TEST_PYSRC_LOC + "testenc/encutf8.py";
        String encoding = FileUtils.getPythonFileEncoding(new File(loc));
        assertEquals("UTF-8", encoding);
    }

    public void testValidInitFile() throws Exception {
        assertTrue(PythonPathHelper.isValidInitFile("a/__init__.py"));
        assertTrue(PythonPathHelper.isValidInitFile("a/__init__/a/__init__.py"));

        assertFalse(PythonPathHelper.isValidInitFile("a/__init__.bar.py"));
        assertFalse(PythonPathHelper.isValidInitFile("a/__init__..py"));
        assertFalse(PythonPathHelper.isValidInitFile("a/__init__/a/.py"));
        assertFalse(PythonPathHelper.isValidInitFile("a/__init__/a/__init__ .py"));

        assertFalse(PythonPathHelper.isValidInitFile("a/__init__/a/m__init__.py"));
    }
}
