/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.ast.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.CompletionCache;
import org.python.pydev.ast.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.PythonShell;
import org.python.pydev.ast.codecompletion.shell.PythonShellTest;
import org.python.pydev.core.BaseModuleRequest;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.concurrency.RunnableAsJobsPoolThread;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.proposals.OverrideMethodCompletionProposal;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;

import junit.framework.AssertionFailedError;

public class PythonCompletionWithBuiltinsTest extends CodeCompletionTestsBase {

    protected static boolean isInTestFindDefinition = false;

    public static void main(String[] args) {
        try {
            PythonCompletionWithBuiltinsTest builtins = new PythonCompletionWithBuiltinsTest();
            builtins.setUp();
            builtins.testAssignToFuncReturnCompletion();
            builtins.tearDown();

            junit.textui.TestRunner.run(PythonCompletionWithBuiltinsTest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected PythonNature createNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
            }

            @Override
            public int getGrammarVersion() {
                return IPythonNature.LATEST_GRAMMAR_PY3_VERSION;
            }

            @Override
            public String resolveModule(File file) throws MisconfigurationException {
                if (isInTestFindDefinition) {
                    return null;
                }
                return super.resolveModule(file);
            }
        };
    }

    private static PythonShell shell;

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ADD_MX_TO_FORCED_BUILTINS = false;
        if (shell == null && TestDependent.PYTHON_NUMPY_PACKAGES != null) {
            try {
                FileUtils.copyFile(TestDependent.PYTHON_NUMPY_PACKAGES +
                        "numpy/core/umath.pyd",
                        TestDependent.TEST_PYSRC_TESTING_LOC
                                +
                                "extendable/bootstrap_dll/umath.pyd");
            } catch (RuntimeException e) {
                //Ignore: it's being already used by some process (which means it's probably already correct anyways).
            }
        }

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        String additionalPaths = "";
        for (String s : new String[] { TestDependent.PYTHON2_WXPYTHON_PACKAGES, TestDependent.PYTHON2_MX_PACKAGES,
                TestDependent.PYTHON_NUMPY_PACKAGES, TestDependent.PYTHON2_OPENGL_PACKAGES,
                TestDependent.PYTHON_DJANGO_PACKAGES

        }) {
            if (s != null) {
                additionalPaths += ("|" + s);
            }
        }
        this.restorePythonPath(TestDependent.getCompletePythonLib(true, isPython3Test()) + "|" + additionalPaths,
                false);

        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if (shell == null) {
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), null);
        super.tearDown();
    }

    public void testRecursion() throws FileNotFoundException, Exception, CompletionRecursionException {
        String file = TestDependent.TEST_PYSRC_TESTING_LOC +
                "testrec3/rec.py";
        String strDoc = "RuntimeError.";
        File f = new File(file);
        try {
            ICodeCompletionASTManager astManager = nature.getAstManager();
            ICompletionState state = CompletionStateFactory.getEmptyCompletionState("RuntimeError", nature,
                    new CompletionCache());
            IModule module = AbstractASTManager.createModule(f, new Document(FileUtils.getFileContents(f)), nature);
            astManager.getCompletionsForModule(module, state, true, true);
        } catch (CompletionRecursionException e) {
            //that's ok... we're asking for it here...
        }
        requestCompl(f, strDoc, strDoc.length(), -1, new String[] { "args", "with_traceback(self, tb)" });
    }

    public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String s;

        s = "from datetime import datetime\n" +
                "datetime.";

        //for some reason, this is failing only when the module is specified...
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC +
                "tests/pysrc/simpledatetimeimport.py");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        requestCompl(file, s, s.length(), -1, new String[] { "today()", "now()", "utcnow()" });

        s = "from datetime import datetime, date, MINYEAR,";
        requestCompl(s, s.length(), -1, new String[] { "date", "datetime", "MINYEAR", "MAXYEAR", "timedelta" });

        s = "from datetime.datetime import ";
        requestCompl(s, s.length(), -1, new String[] { "today", "now", "utcnow" });

        // Problem here is that we do not evaluate correctly if
        // met( ddd,
        //      fff,
        //      ccc )
        //so, for now the test just checks that we do not get in any sort of
        //look...
        s = "" +

                "class bla(object):pass\n" +
                "\n" +
                "def newFunc(): \n"
                +
                "    callSomething( bla.__get#complete here... stack error \n" +
                "                  keepGoing) \n";

        //If we improve the parser to get the error above, uncomment line below to check it...
        requestCompl(s, s.indexOf('#'), 1, new String[] { "__getattribute__()" });

        //check for builtins..1
        s = "" +
                "\n" +
                "";
        requestCompl(s, s.length(), -1, new String[] { "RuntimeError" });

        //check for builtins..2
        s = "" +
                "from testlib import *\n" +
                "\n" +
                "";
        requestCompl(s, s.length(), -1, new String[] { "RuntimeError" });

        //check for builtins..3 (builtins should not be available because it is an import request for completions)
        requestCompl("from testlib.unittest import  ", new String[] { "__file__", "__name__", "__init__", "__dict__",
                "__path__", "anothertest", "AnotherTest", "GUITest", "guitestcase", "main", "relative", "t",
                "TestCase", "testcase", "TestCaseAlias", });

    }

    public void testBuiltinsInNamespace0() throws BadLocationException, IOException, Exception {
        String s = "_";
        requestCompl(s, s.length(), -1, new String[] { "__builtins__" });
    }

    public void testBuiltinsInNamespace1() throws BadLocationException, IOException, Exception {
        String s = "__builtins__.";
        requestCompl(s, s.length(), -1, new String[] { "RuntimeError" });
    }

    public void testBuiltinsInNamespace2() throws BadLocationException, IOException, Exception {
        String s = "__builtins__.RuntimeError.";
        requestCompl(s, s.length(), 2, new String[] { "args", "with_traceback(self, tb)" });
    }

    public void testBuiltinsInNamespace2Underline() throws BadLocationException, IOException, Exception {
        String s = "__builtins__.RuntimeError._";
        requestCompl(s, s.length(), -1, new String[] { "__doc__", "__init__(self)", "__str__(self)" });
    }

    public void testPreferForcedBuiltin() throws BadLocationException, IOException, Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON2_MX_PACKAGES != null) {
            String s = "" +
                    "from mx import DateTime\n" +
                    "DateTime.";
            requestCompl(s, s.length(), -1, new String[] { "now()" });
        }
    }

    public void testNumpy() throws BadLocationException, IOException, Exception {
        if (TestDependent.PYTHON_NUMPY_PACKAGES != null) {
            String s = "" +
                    "from numpy import less\n" +
                    "less.";
            requestCompl(s, s.length(), -1,
                    new String[] { "types", "ntypes", "nout", "nargs", "nin" });
        }
    }

    public void testDeepNested6() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.f.";
        requestCompl(s, s.length(), -1, new String[] { "curdir" });
    }

    public void testDeepNested10() throws Exception {
        String s;
        s = "" +
                "from extendable.nested3 import hub2\n" +
                "hub2.c.a.";
        requestCompl(s, s.length(), -1, new String[] { "fun()" });
    }

    public void testRelativeOnSameProj() throws Exception {
        String s;
        s = "" +
                "import prefersrc\n" +
                "prefersrc.";
        AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "foo";
        try {
            requestCompl(s, s.length(), -1, new String[] { "OkGotHere" }, nature2);
        } finally {
            AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";
        }
    }

    public void testDeepNested7() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.f.curdir.";
        requestCompl(s, s.length(), -1, new String[] { "upper()" });
    }

    public void testDeepNested8() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.C1.f.sep."; //changed: was altsep (may be None in linux).
        requestCompl(s, s.length(), -1, new String[] { "upper()" });
    }

    public void testDeepNested9() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.C1.f.inexistant.";
        requestCompl(s, s.length(), -1, new String[] {});
    }

    public void testDictAssign() throws Exception {
        String s;
        s = "" +
                "a = {}\n" +
                "a.";
        requestCompl(s, s.length(), -1, new String[] { "keys()" });
    }

    public void testPreferSrc() throws BadLocationException, IOException, Exception {
        String s = "" +
                "import prefersrc\n" +
                "prefersrc.";
        requestCompl(s, s.length(), -1, new String[] { "PreferSrc" });
    }

    public void testPreferCompiledOnBootstrap() throws BadLocationException, IOException, Exception {
        // This fails because of platform dependent setUp of umath
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_NUMPY_PACKAGES != null) {
            String s = "" +
                    "from extendable.bootstrap_dll import umath\n" +
                    "umath.";
            IModule module = nature.getAstManager().getModule("extendable.bootstrap_dll.umath", nature, true,
                    new BaseModuleRequest(true));
            assertTrue("Expected CompiledModule. Found: " + module.getClass(), module instanceof CompiledModule);
            //NOTE: The test can fail if numpy is not available (umath.pyd depends on numpy)
            requestCompl(s, s.length(), -1, new String[] { "less" });
        }
    }

    public void testPreferCompiledOnBootstrap2() throws BadLocationException, IOException, Exception {
        // This fails because of platform dependent setUp of umath
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_NUMPY_PACKAGES != null) {
            String s = "" +
                    "from extendable.bootstrap_dll.umath import ";
            IModule module = nature.getAstManager().getModule("extendable.bootstrap_dll.umath", nature, true,
                    new BaseModuleRequest(true));
            assertTrue(module instanceof CompiledModule);
            //NOTE: The test can fail if numpy is not available (umath.pyd depends on numpy)
            requestCompl(s, s.length(), -1, new String[] { "less" });
        }
    }

    public void testWxPython1() throws BadLocationException, IOException, Exception {
        if (TestDependent.PYTHON2_WXPYTHON_PACKAGES != null) { //we can only test what we have
            String s = "" +
                    "from wxPython.wx import *\n" +
                    "import wx\n" +
                    "class HelloWorld(wx.App):\n"
                    +
                    "   def OnInit(self):\n" +
                    "       frame = wx.Frame(None,-1,\"hello world\")\n"
                    +
                    "       frame.Show(True)\n" +
                    "       self.SetTopWindow(frame)\n"
                    +
                    "       b=wx.Button(frame,-1,\"Button\")\n" +
                    "       return True\n" +
                    "app = HelloWorld(0)\n"
                    +
                    "app.MainLoop()\n" +
                    "app.";
            requestCompl(s, s.length(), -1, new String[] { "MainLoop()" });
        }
    }

    public void testCompleteImportBuiltinReference2() throws BadLocationException, IOException, Exception {
        String s;
        if (TestDependent.PYTHON2_WXPYTHON_PACKAGES != null) { //we can only test what we have
            s = "" +
                    "from wx import ";
            requestCompl(s, s.length(), -1, new String[] { "glcanvas" });
        }
    }

    public void testGlu() throws IOException, Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON2_OPENGL_PACKAGES != null) {
            final String s = "from OpenGL import ";
            requestCompl(s, s.length(), -1, new String[] { "GLU", "GLUT" });
        }
    }

    public void testGlu2() throws IOException, Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON2_OPENGL_PACKAGES != null) {
            final String s = "from OpenGL.GL import ";
            requestCompl(s, s.length(), -1, new String[] { "glPushMatrix" });
        }
    }

    public void testCompleteImportBuiltinReference() throws BadLocationException, IOException, Exception {
        String s;

        if (TestDependent.PYTHON2_WXPYTHON_PACKAGES != null) { //we can only test what we have
            s = "" +
                    "from wxPython.wx import wxButton\n" +
                    "                \n" +
                    "wxButton.";
            requestCompl(s, s.length(), -1, new String[] { "Close()" });

            s = "" +
                    "import wxPython\n" +
                    "                \n" +
                    "wxPython.";
            requestCompl(s, s.length(), -1, new String[] { "wx" });
        }

        s = "" +
                "import os\n" +
                "                \n" +
                "os.";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC +
                "tests/pysrc/simpleosimport.py");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        requestCompl(file, s, s.length(), -1, new String[] { "path" });

        s = "" +
                "import os\n" +
                "                \n" +
                "os.";
        requestCompl(s, s.length(), -1, new String[] { "path" });

        //check for builtins with reference..3
        s = "" +
                "from testlib.unittest import anothertest\n" +
                "anothertest.";
        requestCompl(s, s.length(), 2, new String[] { "AnotherTest", "t" });

        s = "" +
                "from testlib.unittest import anothertest\n" +
                "anothertest._";
        requestCompl(s, s.length(), 3, new String[] { "__file__", "__dict__", "__name__" });

    }

    public void testInstanceCompletion() throws Exception {
        String s = "class A:\n" +
                "    def __init__(self):\n" +
                "        self.list1 = []\n"
                +
                "if __name__ == '__main__':\n" +
                "    a = A()\n" +
                "    a.list1.";

        requestCompl(s, -1, new String[] { "pop(index)", "remove(value)" });
    }

    public void test__all__() throws Exception {
        String s = "from extendable.all_check import *\n" +
                "";

        //should keep the variables from the __builtins__ in this module
        ICompletionProposalHandle[] codeCompletionProposals = requestCompl(s, -1,
                new String[] { "ThisGoes", "RuntimeError" });
        assertNotContains("ThisDoesnt", codeCompletionProposals);
    }

    public void test__all__3() throws Exception {
        String s = "from extendable.all_check3 import *\n" +
                "";

        //should keep the variables from the __builtins__ in this module
        ICompletionProposalHandle[] codeCompletionProposals = requestCompl(s, -1,
                new String[] { "ThisGoes", "RuntimeError" });
        assertNotContains("ThisDoesnt", codeCompletionProposals);

    }

    public void testSortParamsCorrect() throws Exception {
        //should keep the variables from the __builtins__ in this module
        requestCompl("[].sort", 1, new String[] { "sort()" });
    }

    public void testFindDefinition() throws Exception {
        isInTestFindDefinition = true;
        try {
            CompiledModule mod = new CompiledModule("os", nature.getAstManager().getModulesManager(), nature);
            Definition[] findDefinition = mod.findDefinition(
                    CompletionStateFactory.getEmptyCompletionState("walk", nature, new CompletionCache()), -1, -1,
                    nature);
            assertEquals(1, findDefinition.length);
            assertEquals("os", findDefinition[0].module.getName());
        } finally {
            isInTestFindDefinition = false;
        }
    }

    public void testDjango() throws Exception {
        if (TestDependent.PYTHON_DJANGO_PACKAGES != null) {
            String s = "from django.db import models\n" +
                    "\n" +
                    "class HelperForPydevCompletion(models.Model):\n"
                    +
                    "    helper = models.IntegerField()\n" +
                    "\n" +
                    "HelperForPydevCompletion.";

            requestCompl(s, -1, new String[] { "objects" });
        }
    }

    public void testDjango2() throws Exception {
        if (TestDependent.PYTHON_DJANGO_PACKAGES != null) {
            assertTrue(new File(TestDependent.PYTHON_DJANGO_PACKAGES).exists());
            String s = "from django.db import models\n" +
                    "\n" +
                    "class HelperForPydevCompletion(models.Model):\n"
                    +
                    "    helper = models.IntegerField()\n" +
                    "\n" +
                    "HelperForPydevCompletion.objects.";

            requestCompl(s, -1, new String[] { "get()" });
        }
    }

    public void testDjango3() throws Exception {
        if (TestDependent.PYTHON_DJANGO_PACKAGES != null) {
            String s = "from django.contrib.auth.models import User\n" +
                    "User.";

            requestCompl(s, -1, new String[] { "DoesNotExist" });
        }
    }

    public void testKeywordCompletions() throws Exception {
        String s = "assert isinstance(lo";

        ICompletionProposalHandle[] requestCompl = requestCompl(s, -1, new String[] {});
        for (ICompletionProposalHandle iCompletionProposal : requestCompl) {
            if (iCompletionProposal.getDisplayString().equals("locals={}=")) {
                fail("A locals={}= should not be found.");
            }
        }
    }

    public void testOverrideCompletions() throws Exception {
        String s;
        s = "" +
                "class Bar(object):\n" +
                "    def __ha";//bring override completions!
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), -1,
                new String[] { "__hash__ (Override method in object)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Bar(object):\n" +
                "    def __hash__(self)->int:\n" +
                "        return object.__hash__(self)", doc.get());
    }

    public void testOverrideCompletionsNoReturn() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def method(self)->None:\n" +
                "        ...\n" +
                "class Bar(Foo):\n" +
                "    def method";//bring override completions!
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), -1,
                new String[] { "method (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    def method(self)->None:\n" +
                "        ...\n" +
                "class Bar(Foo):\n" +
                "    def method(self)->None:\n" +
                "        Foo.method(self)", doc.get());
    }

    public void testBuiltinKnownReturns() throws Exception {
        String s = "a = open()\n" +
                "a.";

        //open returns a file object.
        requestCompl(s, -1, new String[] { "close()", "flush()", "write(s)" });
    }

    public void testBuiltinKnownReturns1() throws Exception {
        String s = "a = ''.split()\n" + //returns list
                "a.";

        ICompletionProposalHandle[] comps = requestCompl(s, -1, new String[] { "append(object)", "reverse()" });
        ICompletionProposalHandle appendComp = Arrays.stream(comps).filter((comp) -> {
            return comp.getDisplayString().startsWith("append(");
        }).findFirst().get();
        String additionalProposalInfo = appendComp.getAdditionalProposalInfo();
        assertTrue(additionalProposalInfo.indexOf("object to the end of the list") != -1);
    }

    public void testBuiltinCached() throws Exception {
        IModule module = nature.getAstManager().getModule("_bisect", nature, true, new BaseModuleRequest(false));
        assertTrue("Expected CompiledModule. Found: " + module, module instanceof CompiledModule);
        ISystemModulesManager systemModulesManager = nature.getAstManager().getModulesManager()
                .getSystemModulesManager();
        RunnableAsJobsPoolThread.getSingleton().waitToFinishCurrent();
        File file = systemModulesManager.getCompiledModuleCacheFile(module.getName());
        assertTrue(file.exists());

        module = nature.getAstManager().getModule("_bisect.foo", nature, true, new BaseModuleRequest(false));
        assertNull(module);

        module = nature.getAstManager().getModule("os", nature, true, new BaseModuleRequest(false));
        assertTrue("Expected CompiledModule. Found: " + module, module instanceof CompiledModule);

        module = nature.getAstManager().getModule("os.path", nature, true, new BaseModuleRequest(false));
        assertTrue("Expected CompiledModule. Found: " + module, module instanceof CompiledModule);
    }

    public void testAssignToFuncCompletion() throws Exception {
        String s = "" +
                "def aFunction(a, b, c):\n" +
                "    return tuple(a, b, c)\n" +
                "\n" +
                "tup1 = aFunction\n"
                +
                "tup";

        requestCompl(s, -1, new String[] { "tup1(a, b, c)" });
    }

    public void testAssignToFuncReturnCompletion() throws Exception {
        String s = "" +
                "def aFunction(a, b, c):\n" +
                "    return tuple(a, b, c)\n" +
                "\n"
                +
                "tup1 = aFunction(1, 2, 3)\n" +
                "tup";

        requestCompl(s, -1, new String[] { "tup1" });
    }

    public void testCodeCompletionForCompoundObjectsDocstring() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "def foo(a):\n"
                + "    ':type a: list of F'\n"
                + "    a."
                + "";
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), -1, new String[] { "append(object)" });
        assertTrue(comps.length > 10); //list completions
        ICompletionProposalHandle appendCompletion = Arrays.stream(comps).filter((comp) -> {
            return comp.getDisplayString().startsWith("append(");
        }).findFirst().get();
        String info = appendCompletion.getAdditionalProposalInfo();
        if (!info.contains("def append(self")) {
            throw new AssertionFailedError("Did not find 'def append(self' in: " + info);
        }
        if (!info.contains("Append object to the end of the list.")) {
            throw new AssertionFailedError("Did not find 'Append object to the end of the list.' in: " + info);
        }
    }

    public void testCodeCompletionForCompoundObjectsBuiltin() throws Exception {
        String s;
        s = ""
                + "x = ''\n"
                + "for a in x.splitlines()\n"
                + "    a."
                + "";
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), -1, new String[] { "title()", "upper()" });
        assertTrue(comps.length > 10); //str completions
    }

    public void testCodeCompletionForCompoundObjectsBuiltin2() throws Exception {
        String s;
        s = ""
                + "d = {i: str(i) for i in xrange(10)}\n"
                + "d."
                + "";
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), -1, new String[] { "items()" });
        assertTrue(comps.length > 10); //dict completions
    }

    public void testEnum() throws Exception {
        String s;
        s = "" +
                "from enum import Enum\n" +
                "\n" +
                "class Color(Enum):\n" +
                "    Black = '#000000'\n" +
                "    White = '#ffffff'\n" +
                "Color.Black.";
        requestCompl(s, s.length(), -1, new String[] { "name", "value" });
    }

    public void testTypingCloseDocs() throws Exception {
        String s;
        s = "" +
                "from typing import Generator\n" +
                "\n" +
                "generator = Generator()\n" +
                "generator.close";
        ICompletionProposalHandle[] comps = requestCompl(s, s.length(), 1, new String[] { "close()" });
        String info = comps[0].getAdditionalProposalInfo();
        if (StringUtils.count(info, "def close(self)->None") != 1) {
            throw new AssertionFailedError("Expected 1 occurrence of: 'def close(self)->None' in " + info);
        }

    }
}
