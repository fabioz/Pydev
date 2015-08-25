/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

/**
 * This tests the 'whole' code completion, passing through all modules.
 *
 * @author Fabio Zadrozny
 */
public class PythonCompletionWithoutBuiltinsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            PythonCompletionWithoutBuiltinsTest test = new PythonCompletionWithoutBuiltinsTest();
            test.setUp();
            test.testGrammar2AbsoluteAndRelativeImports();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionWithoutBuiltinsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void registerThreadGlobalCompletion() {
        Map<String, List<Object>> participants = new HashMap<>();
        participants.put(ExtensionHelper.PYDEV_COMPLETION, Arrays.asList((Object) new IPyDevCompletionParticipant() {

            @Override
            public Collection<Object> getStringGlobalCompletions(CompletionRequest request, ICompletionState state)
                    throws MisconfigurationException {
                return new ArrayList<>();
            }

            @Override
            public Collection<Object> getGlobalCompletions(CompletionRequest request, ICompletionState state)
                    throws MisconfigurationException {
                return new ArrayList<>();
            }

            @Override
            public Collection<IToken> getCompletionsForType(ICompletionState state)
                    throws CompletionRecursionException {
                ArrayList<IToken> ret = new ArrayList<>();
                if (state.getActivationToken().endsWith("Thread")) {
                    ret.add(new CompiledToken("run()", "", "", "", 1));
                }
                return ret;
            }

            @Override
            public Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state,
                    ILocalScope localScope,
                    Collection<IToken> interfaceForLocal) {
                return new ArrayList<>();
            }

            @Override
            public Collection<IToken> getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope,
                    Collection<IToken> interfaceForLocal) {
                return new ArrayList<>();
            }

            @Override
            public Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope,
                    Collection<IToken> interfaceForLocal) {
                return new ArrayList<>();
            }
        }));
        ExtensionHelper.testingParticipants = participants;
    }

    private static final class ParticipantWithBarToken implements IPyDevCompletionParticipant {
        public Collection<Object> getStringGlobalCompletions(CompletionRequest request, ICompletionState state)
                throws MisconfigurationException {
            throw new RuntimeException("Not implemented");
        }

        public Collection<Object> getGlobalCompletions(CompletionRequest request, ICompletionState state)
                throws MisconfigurationException {
            throw new RuntimeException("Not implemented");
        }

        public Collection<IToken> getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope,
                Collection<IToken> interfaceForLocal) {
            throw new RuntimeException("Not implemented");
        }

        public Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state,
                ILocalScope localScope, Collection<IToken> interfaceForLocal) {
            ArrayList<IToken> ret = new ArrayList<IToken>();
            ret.add(new SourceToken(null, "bar", null, null, null, IToken.TYPE_ATTR));
            return ret;
        }

        public Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope,
                Collection<IToken> interfaceForLocal) {
            throw new RuntimeException("Not implemented");
        }

        public Collection<IToken> getCompletionsForType(ICompletionState state) {
            throw new RuntimeException("Not implemented");
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.GetCompletePythonLib(true) +
                "|" + TestDependent.PYTHON_PIL_PACKAGES +
                "|"
                + TestDependent.TEST_PYSRC_LOC +
                "configobj-4.6.0-py2.6.egg", false);

        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            public Object call(CompletionRecursionException e) {
                throw new RuntimeException("Recursion error:" + Log.getExceptionStr(e));
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        PyCodeCompletion.onCompletionRecursionException = null;
        ExtensionHelper.testingParticipants = null;
    }

    public void testCompleteImportCompletion() throws Exception {
        String[] testLibAndSubmodules = new String[] { "testlib",
                //                "testlib.unittest",
                //                "testlib.unittest.anothertest",
                //                "testlib.unittest.guitestcase",
                //                "testlib.unittest.relative",
                //                "testlib.unittest.relative.testrelative",
                //                "testlib.unittest.relative.toimport",
                //                "testlib.unittest.testcase",
        };

        requestCompl("import zipf", new String[] { "zipfile" });
        requestCompl("from testl", testLibAndSubmodules);
        requestCompl("import testl", testLibAndSubmodules);
        requestCompl("from testlib import ", new String[] { "__file__", "__name__", "__init__", "unittest", "__path__",
                "__dict__" });
        requestCompl("from testlib import unittest, __in", new String[] { "__init__" });
        requestCompl("from testlib import unittest,__in", new String[] { "__init__" });
        requestCompl("from testlib import unittest ,__in", new String[] { "__init__" });
        requestCompl("from testlib import unittest , __in", new String[] { "__init__" });
        requestCompl("from testlib import unittest , ", new String[] { "__file__", "__name__", "__init__", "unittest",
                "__path__", "__dict__" });

        requestCompl("from testlib.unittest import  ", getTestLibUnittestTokens());

        requestCompl("from testlib.unittest.testcase.TestCase import  assertImagesNotE",
                new String[] { "assertImagesNotEqual" });
        requestCompl("from testlib.unittest.testcase.TestCase import  assertBM", new String[] { "assertBMPsNotEqual",
                "assertBMPsEqual" });
    }

    public void testFullModulesOnFromImport() throws Exception {
        requestCompl("from ", -1, new String[] { "testlib" });
        //        requestCompl("from ", -1, new String[]{"testlib", "testlib.unittest"}); -- feature removed.
    }

    /**
     * This test checks the code-completion for adaptation and factory methods, provided that the
     * class expected is passed as one of the parameters.
     *
     * This is done in AssignAnalysis
     */
    public void testProtocolsAdaptation() throws Exception {
        String s = "import protocols\n" +
                "class InterfM1(protocols.Interface):\n" +
                "    def m1(self):\n"
                +
                "        pass\n" +
                " \n" +
                "class Bar(object):\n"
                +
                "    protocols.advise(instancesProvide=[InterfM1])\n" +
                "if __name__ == '__main__':\n"
                +
                "    a = protocols.adapt(Bar(), InterfM1)\n" +
                "    a.";

        requestCompl(s, s.length(), -1, new String[] { "m1()" });
    }

    /**
     * Check if some assert for an instance is enough to get the type of some variable. This should
     * be configurable so that the user can do something as assert IsInterfaceDeclared(obj, Class) or
     * AssertImplements(obj, Class), with the assert or not, providing some way for the user to configure that.
     *
     * This is done in ILocalScope#getPossibleClassesForActivationToken
     */
    public void testAssertDeterminesClass() throws Exception {
        String s = "def m1(a):\n" +
                "    import xmllib\n" +
                "    assert isinstance(a, xmllib.XMLParser)\n" +
                "    a.";

        requestCompl(s, s.length(), -1, new String[] { "handle_data(data)" });

    }

    public void testAssertDeterminesClass2() throws Exception {
        String s = "def m1(a):\n" +
                "    import xmllib\n" +
                "    assert isinstance(a.bar, xmllib.XMLParser)\n"
                +
                "    a.bar.";

        requestCompl(s, s.length(), -1, new String[] { "handle_data(data)" });

    }

    public void testAssertDeterminesClass3() throws Exception {
        String s = "class InterfM1:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "" +
                "def m1(a):\n"
                +
                "    assert isinstance(a, InterfM1)\n" +
                "    a.";

        requestCompl(s, s.length(), -1, new String[] { "m1()" });

    }

    public void testAssertDeterminesClass4() throws Exception {
        String s = "class InterfM1:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "class InterfM2:\n"
                +
                "    def m2(self):\n" +
                "        pass\n" +
                "\n" +
                "" +
                "def m1(a):\n"
                +
                "    assert isinstance(a, (InterfM1, InterfM2))\n" +
                "    a.";

        requestCompl(s, s.length(), -1, new String[] { "m1()", "m2()" });

    }

    public void testAssertDeterminesClass5() throws Exception {
        String s = "class InterfM1:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "" +
                "def m1(a):\n"
                +
                "    assert InterfM1.implementedBy(a)\n" +
                "    a.";

        requestCompl(s, s.length(), -1, new String[] { "m1()" });

    }

    public void testAssertDeterminesClass6() throws Exception {
        String s = "class InterfM1:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "" +
                "def m1(a):\n"
                +
                "    assert InterfM1.implementedBy()\n" + //should give no error
                "    a.";

        requestCompl(s, s.length(), -1, new String[] {});

    }

    public void testMultilineImportCompletion() throws Exception {
        String s = "from testlib import (\n";

        requestCompl(s, new String[] { "__file__", "__name__", "__init__", "unittest", "__path__", "__dict__" });

    }

    /**
     * @return
     */
    public String[] getTestLibUnittestTokens() {
        return new String[] { "__file__", "__init__", "__name__", "__dict__", "__path__", "anothertest", "AnotherTest",
                "GUITest", "guitestcase", "main", "relative", "t", "TestCase", "testcase", "TestCaseAlias" };
    }

    public void testSelfReference() throws Exception {
        String s;
        s = "class C:            \n" +
                "    def met1(self): \n" +
                "        pass        \n" +
                "                    \n"
                +
                "class B:            \n" +
                "    def met2(self): \n" +
                "        self.c = C()\n"
                +
                "                    \n" +
                "    def met3(self): \n" +
                "        self.c.";
        requestCompl(s, s.length(), -1, new String[] { "met1()" });
    }

    public void testProj2() throws Exception {
        String s;
        s = "" +
                "import proj2root\n" +
                "print proj2root.";
        requestCompl(s, s.length(), -1, new String[] { "Proj2Root" }, nature2);
    }

    public void testProj2Global() throws Exception {
        String s;
        s = "" +
                "import ";
        requestCompl(s, s.length(), -1, new String[] { "proj2root", "testlib" }, nature2);
    }

    public void testPIL() throws Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_PIL_PACKAGES != null) {
            String s;
            s = "" +
                    "import Image\n" +
                    "Image." +
                    "";
            requestCompl(s, s.length(), -1, new String[] { "RASTERIZE" });
        }
    }

    public void testClassAttrs() throws Exception {
        String s;
        s = "" +
                "class A:\n" +
                "    aa, bb, cc = range(3)\n" + //the heuristic to find the attrs (class HeuristicFindAttrs) was not getting this
                "    dd = 1\n" +
                "    def m1(self):\n" +
                "        self.";
        requestCompl(s, s.length(), -1, new String[] { "aa", "bb", "cc", "dd" });
    }

    public void testFromImport() throws Exception {
        //TODO: see AbstractASTManager.resolveImport
        String s;
        s = "" +
                "from testOtherImports.f3 import test\n" +
                "tes";
        ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "test(a, b, c)" }, nature);
        assertEquals("def test(a, b, c):    \"\"\"This is a docstring\"\"\"",
                StringUtils.removeNewLineChars(p[0].getAdditionalProposalInfo()));
    }

    public void testFromImportAs() throws Exception {
        String s;
        s = "" +
                "from testOtherImports.f3 import test as AnotherTest\n" +
                "t = AnotherTes";
        ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "AnotherTest(a, b, c)" }, nature);
        assertEquals("def test(a, b, c):    \"\"\"This is a docstring\"\"\"",
                StringUtils.removeNewLineChars(p[0].getAdditionalProposalInfo()));
    }

    public void testFromImportAs2() throws Exception {
        String s;
        s = "" +
                "from testOtherImports.f3 import Foo\n" +
                "t = Fo";
        ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "Foo" }, nature);
        assertEquals("class SomeOtherTest(object):    '''SomeOtherTest'''    def __init__(self, a, b):        pass",
                StringUtils.removeNewLineChars(p[0].getAdditionalProposalInfo()));
    }

    public void testInnerImport() throws Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String s;
        s = "" +
                "def m1():\n" +
                "    from testlib import unittest\n" +
                "    unittest.";
        requestCompl(s, s.length(), -1, new String[] { "AnotherTest", "GUITest",
                "main(module, defaultTest, argv, testRunner, testLoader)", "TestCase", "testcase", "TestCaseAlias"

                //gotten because unittest is actually an __init__, so, gather others that are in the same level
                , "anothertest", "guitestcase", "testcase" });
    }

    public void testSelfReferenceWithTabs() throws Exception {
        String s;
        s = "class C:\n" +
                "    def met1(self):\n" +
                "        pass\n" +
                "        \n" +
                "class B:\n"
                +
                "    def met2(self):\n" +
                "        self.c = C()\n" +
                "        \n" +
                "    def met3(self):\n"
                +
                "        self.c.";
        s = s.replaceAll("\\ \\ \\ \\ ", "\t");
        requestCompl(s, s.length(), -1, new String[] { "met1()" });
    }

    public void testClassCompl() throws Exception {
        String s;
        s = "" +
                "class Test:\n" +
                "    classVar = 1\n" +
                "    def findIt(self):\n" +
                "        self.";
        requestCompl(s, s.length(), -1, new String[] { "classVar" });
    }

    public void testInnerCtxt() throws Exception {
        String s;
        s = "" +
                "class Test:\n" +
                "    def findIt(self):\n" +
                "        pass\n" +
                "    \n" +
                "def m1():\n"
                +
                "    s = Test()\n" +
                "    s.";
        requestCompl(s, s.length(), -1, new String[] { "findIt()" });
    }

    public void testDeepNested() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.a.";
        requestCompl(s, s.length(), -1, new String[] { "fun()" });
    }

    public void testDeepNested2() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.b.";
        requestCompl(s, s.length(), -1, new String[] { "another()" });
    }

    public void testDeepNested3() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.c.";
        requestCompl(s, s.length(), -1, new String[] { "another()" });
    }

    public void testDeepNested4() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.d.";
        requestCompl(s, s.length(), -1, new String[] { "AnotherTest" });
    }

    public void testDeepNested5() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import hub\n" +
                "hub.c1.e.";
        requestCompl(s, s.length(), -1, new String[] { "assertBMPsNotEqual(f1, f2)" });
    }

    public void testDeepNested6() throws Exception {
        String s;
        s = "" +
                "from extendable.nested2 import mod2\n" +
                "mod2.c1.a.";
        requestCompl(s, s.length(), -1, new String[] { "fun()" });
    }

    public void testSelfReferenceWithTabs2() throws Exception {
        String s;
        s = "" +
                "class C:\n" +
                "    def met3(self):\n" +
                "        self.COMPLETE_HERE\n" +
                "                    \n"
                +
                "    def met1(self): \n" +
                "        pass        \n" +
                "";
        s = s.replaceAll("\\ \\ \\ \\ ", "\t");
        int iComp = s.indexOf("COMPLETE_HERE");
        s = s.replaceAll("COMPLETE_HERE", "");
        requestCompl(s, iComp, -1, new String[] { "met1()" });
    }

    public void testRelativeImport() throws FileNotFoundException, Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "testlib/unittest/relative/testrelative.py";
        String strDoc = "from toimport import ";
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[] { "Test1", "Test2" });
    }

    public void testRelativeImport2() throws FileNotFoundException, Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "extendable/relative_absolute_import/__init__.py";
        String strDoc = "from .foo import bar as buzz\nbuzz.";
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[] { "jazz()" });
    }

    public void testInModuleWithoutExtension() throws FileNotFoundException, Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "mod_without_extension";
        String strDoc = FileUtils.getFileContents(new File(file));
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[] { "ClassInModWithoutExtension" });
    }

    public void testRelativeImportWithSubclass() throws FileNotFoundException, Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "extendable/relative_with_sub/bb.py";
        String strDoc = FileUtils.getFileContents(new File(file));
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[] { "yyy()" });
    }

    public void testWildImportRecursive() throws BadLocationException, IOException, Exception {
        String s;
        s = "from testrecwild import *\n" +
                "";
        requestCompl(s, -1, -1, new String[] { "Class1" });
    }

    public void testWildImportRecursive2() throws BadLocationException, IOException, Exception {
        String s;
        s = "from testrecwild2 import *\n" +
                "";
        requestCompl(s, -1, -1, new String[] { "Class2" });
    }

    public void testWildImportRecursive3() throws BadLocationException, IOException, Exception {
        String s;
        s = "from testrec2 import *\n" +
                "";
        requestCompl(s, -1, -1, new String[] { "Leaf" });
    }

    public void testProperties() throws BadLocationException, IOException, Exception {
        String s;
        s = "class C:\n" +
                "    \n" +
                "    properties.create(test = 0)\n" +
                "    \n" +
                "c = C.";
        requestCompl(s, -1, -1, new String[] { "test" });
    }

    public void testImportMultipleFromImport() throws BadLocationException, IOException, Exception {
        String s;
        s = "import testlib.unittest.relative\n" +
                "";
        requestCompl(s, -1, -1, new String[] { "testlib", "testlib.unittest", "testlib.unittest.relative" });
    }

    public void testImportMultipleFromImport2() throws BadLocationException, IOException, Exception {
        String s;
        s = "import testlib.unittest.relative\n" +
                "testlib.";
        requestCompl(s, -1, -1, new String[] { "__path__" });
    }

    public void testNestedImports() throws BadLocationException, IOException, Exception {
        String s;
        s = "from extendable import nested\n" +
                "print nested.NestedClass.";
        requestCompl(s, -1, 1, new String[] { "nestedMethod(self)" });
    }

    public void testSameName() throws BadLocationException, IOException, Exception {
        String s;
        s = "from extendable.namecheck import samename\n" +
                "print samename.";
        requestCompl(s, -1, 1, new String[] { "method1(self)" });
    }

    public void testSameName2() throws BadLocationException, IOException, Exception {
        String s;
        s = "from extendable import namecheck\n" +
                "print namecheck.samename.";
        requestCompl(s, -1, 1, new String[] { "method1(self)" });
    }

    public void testCompositeImport() throws BadLocationException, IOException, Exception {
        String s;
        s = "import xml.sax\n" +
                "print xml.sax.";
        requestCompl(s, -1, -1, new String[] { "default_parser_list" });
    }

    public void testIsInGlobalTokens() throws BadLocationException, IOException, Exception {
        IModule module = nature.getAstManager().getModule("testAssist.__init__", nature, true);
        assertTrue(module.isInGlobalTokens("assist.ExistingClass.existingMethod", nature, new CompletionCache()));
    }

    public void testGetActTok() {
        String strs[];

        strs = PySelection.getActivationTokenAndQual(new Document(""), 0, false);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);

        strs = PySelection.getActivationTokenAndQual(
                new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42, false);
        assertEquals("", strs[0]);
        assertEquals("C", strs[1]);

        strs = PySelection.getActivationTokenAndQual(
                new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41, false);
        assertEquals("", strs[0]);
        assertEquals("C", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14, false);
        assertEquals("self.", strs[0]);
        assertEquals("c", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("[a,b].ap"), 8, false);
        assertEquals("list.", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12, false);
        assertEquals("dict.", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("''.ap"), 5, false);
        assertEquals("str.", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("\"\".ap"), 5, false);
        assertEquals("str.", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20, false);
        assertEquals("ClassA.someMethod.", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22, false);
        assertEquals("ClassA.someMethod().", strs[0]);
        assertEquals("ap", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28, false);
        assertEquals("ClassA.someMethod().", strs[0]);
        assertEquals("ap", strs[1]);

        String s = "Foo().";
        strs = PySelection.getActivationTokenAndQual(new Document(s), s.length(), false);
        assertEquals("Foo().", strs[0]);
        assertEquals("", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, false);
        assertEquals("", strs[0]);
        assertEquals("fo", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, false);
        assertEquals("", strs[0]);
        assertEquals("fo", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, true);
        assertEquals("", strs[0]);
        assertEquals("foo", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 2, true);
        assertEquals("", strs[0]);
        assertEquals("foo", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 5, true); //get the full qualifier
        assertEquals("foo.", strs[0]);
        assertEquals("bar", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 5, false); //get just a part of it
        assertEquals("foo.", strs[0]);
        assertEquals("b", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 100, true); //out of the league
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);

        String importsTipperStr = ImportsSelection.getImportsTipperStr(new Document("from coilib.decorators import "),
                30).importsTipperStr;
        assertEquals("coilib.decorators", importsTipperStr);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar.xxx   "), 9, true);
        assertEquals("foo.bar.", strs[0]);
        assertEquals("xxx", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar.xxx   "), 9, false);
        assertEquals("foo.bar.", strs[0]);
        assertEquals("x", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document("m1(a.b)"), 4, false);
        assertEquals("", strs[0]);
        assertEquals("a", strs[1]);

        //Ok, now, the tests for getting the activation token and qualifier for the calltips.
        //We should 'know' that we're just after a parenthesis and get the contents before it
        //This means: get the char before the offset (excluding spaces and tabs) and see
        //if it is a ',' or '(' and if it is, go to that offset and do the rest of the process
        //as if we were on that position
        ActivationTokenAndQual act = PySelection.getActivationTokenAndQual(new Document("m1()"), 3, false, true);
        assertEquals("", act.activationToken);
        assertEquals("m1", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertFalse(act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1.m2()"), 6, false, true);
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertFalse(act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1.m2(  \t)"), 9, false, true);
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertFalse(act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1(a  , \t)"), 9, false, true);
        assertEquals("", act.activationToken);
        assertEquals("m1", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertTrue(act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1(a)"), 4, false, true);
        assertEquals("", act.activationToken);
        assertEquals("a", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1(a.)"), 5, false, true);
        assertEquals("a.", act.activationToken);
        assertEquals("", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document("m1(a, b)"), 7, false, true);
        assertEquals("", act.activationToken);
        assertEquals("b", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(act.isInMethodKeywordParam);
    }

    public void testGetActTokOnCompound() {
        String str = "a[0].foo";
        ActivationTokenAndQual act = PySelection.getActivationTokenAndQual(new Document(str), str.length(), true, true);
        assertEquals("a.__getitem__().", act.activationToken);
        assertEquals("foo", act.qualifier);
    }

    public void testGetTextForCompletionInConsole() {
        Document doc = new Document("a[0].");
        assertEquals("a[0].", PySelection.getTextForCompletionInConsole(doc, doc.getLength()));
    }

    public void testGetTextForCompletionInConsole2() {
        Document doc = new Document("1, a[0].");
        assertEquals("a[0].", PySelection.getTextForCompletionInConsole(doc, doc.getLength()));
    }

    public void testGetTextForCompletionInConsole3() {
        Document doc = new Document("1, a[','].");
        assertEquals("a[','].", PySelection.getTextForCompletionInConsole(doc, doc.getLength()));
    }

    public void testGetTextForCompletionInConsole4() {
        Document doc = new Document("1, ','.");
        assertEquals("','.", PySelection.getTextForCompletionInConsole(doc, doc.getLength()));
    }

    /**
     * Add tests that demonstrate behaviour when doc starts with a .
     */
    public void testGetAckTok2() {
        String strs[];
        strs = PySelection.getActivationTokenAndQual(new Document("."), 1, false);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document(".a"), 1, false);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);

        strs = PySelection.getActivationTokenAndQual(new Document(".a"), 2, false);
        assertEquals("", strs[0]);
        assertEquals("a", strs[1]);

        ActivationTokenAndQual act = PySelection.getActivationTokenAndQual(new Document("."), 1, false, true);
        assertEquals("", act.activationToken);
        assertEquals("", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document(".a"), 1, false, true);
        assertEquals("", act.activationToken);
        assertEquals("", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document(".a"), 2, false, true);
        assertEquals("", act.activationToken);
        assertEquals("a", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document(".abc"), 1, true, true);
        assertEquals("", act.activationToken);
        assertEquals("abc", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);

        act = PySelection.getActivationTokenAndQual(new Document(".abc"), 2, true, true);
        assertEquals("", act.activationToken);
        assertEquals("abc", act.qualifier);
        assertTrue(!act.changedForCalltip);
        assertTrue(!act.alreadyHasParams);
        assertTrue(!act.isInMethodKeywordParam);
    }

    /**
     * @throws BadLocationException
     * @throws CoreException
     */
    public void testFor() throws Exception {
        String s;
        s = "" +
                "for event in a:   \n" +
                "    print event   \n" +
                "                  \n" +
                "print event.xx\n"
                +
                "print event." +
                "";
        requestCompl(s, s.length(), -1, new String[] { "xx" });
    }

    public void testForWithExtensions() throws Exception {
        String s;
        s = "" +
                "for event in a:   \n" +
                "    print event   \n" +
                "                  \n" +
                "print event.xx\n"
                +
                "print event." +
                "";
        checkParticipantAndXXInterface(s);
    }

    public void testForWithExtensions2() throws Exception {
        String s;
        s = "" +
                "for x in []:   \n" +
                "   x.xx = 10\n" +
                "   x." +
                "";
        checkParticipantAndXXInterface(s);
    }

    private void checkParticipantAndXXInterface(String s) throws Exception {
        try {
            Map<String, List<Object>> participants = new HashMap<String, List<Object>>();
            List<Object> completionParticipants = new ArrayList<Object>();
            participants.put(ExtensionHelper.PYDEV_COMPLETION, completionParticipants);
            completionParticipants.add(new ParticipantWithBarToken());
            ExtensionHelper.testingParticipants = participants;

            requestCompl(s, s.length(), -1, new String[] { "xx", "bar" });
        } catch (StackOverflowError e) {
            throw new RuntimeException(e);
        } finally {
            ExtensionHelper.testingParticipants = null;
        }
    }

    public void testForWithExtensions3() throws Exception {
        String s;
        s = "" +
                "for x in []:   \n" +
                "   x[0].a." +
                "";
        checkParticipant(s);
    }

    public void testExtensionsWithUndefined() throws Exception {
        String s;
        s = "" +
                "x = [1,2,3]" +
                "x[0]." +
                "";
        checkParticipant(s);
    }

    public void testExtensionsWithUndefinedMethodReturn() throws Exception {
        String s;
        s = "" +
                "def m1():\n" +
                "    return a\n" +
                "x = m1()\n" +
                "x." +
                "";
        checkParticipant(s);
    }

    private void checkParticipant(String s) throws Exception {
        try {
            Map<String, List<Object>> participants = new HashMap<String, List<Object>>();
            List<Object> completionParticipants = new ArrayList<Object>();
            participants.put(ExtensionHelper.PYDEV_COMPLETION, completionParticipants);
            completionParticipants.add(new ParticipantWithBarToken());
            ExtensionHelper.testingParticipants = participants;

            requestCompl(s, s.length(), -1, new String[] { "bar" });
        } catch (StackOverflowError e) {
            throw new RuntimeException(e);
        } finally {
            ExtensionHelper.testingParticipants = null;
        }
    }

    public void testCompletionAfterClassInstantiation() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def m1(self):pass\n" +
                "\n" +
                "Foo()." +
                "";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
    }

    public void testClassConstructorParams() throws Exception {
        String s;
        String original = "" +
                "class Foo:\n" +
                "    def __init__(self, a, b):pass\n\n" +
                "    def m1(self):pass\n\n"
                +
                "Foo(%s)" + //completion inside the empty parenthesis should: add the parameters in link mode (a, b) and let the calltip there.
                "";
        s = StringUtils.format(original, "");

        ICompletionProposal[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposal prop = proposals[0];
        assertEquals("Foo(a, b)", prop.getDisplayString());

        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) prop.getContextInformation();
        assertEquals("self, a, b", contextInformation.getContextDisplayString());
        assertEquals("self, a, b", contextInformation.getInformationDisplayString());

        Document doc = new Document(s);
        prop.apply(doc);
        String expected = StringUtils.format(original, "a, b");
        assertEquals(expected, doc.get());
    }

    public void testRegularClass() throws Exception {
        String s;
        s = "" +
                "class Fooooo:\n" +
                "    def __init__(self, a, b):pass\n\n" +
                "Fooo\n";
        ICompletionProposal[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposal p = proposals[0];
        assertEquals("Fooooo", p.getDisplayString());
    }

    public void testSelfCase() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def __init__(self, a, b):pass\n\n" +
                "Foo.__init__\n"; //we should only strip the self if we're in an instance (which is not the case)
        ICompletionProposal[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposal p = proposals[0];
        assertEquals("__init__(self, a, b)", p.getDisplayString());
    }

    public void testDuplicate() throws Exception {
        String s = "class Foo(object):\n" +
                "    def __init__(self):\n" +
                "        self.attribute = 1\n"
                +
                "        self.attribute2 = 2";

        ICompletionProposal[] proposals = requestCompl(s, s.length() - "ute2 = 2".length(), 1,
                new String[] { "attribute" });
        assertEquals(1, proposals.length);
    }

    public void testDuplicate2() throws Exception {
        String s = "class Bar(object):\n" +
                "    def __init__(self):\n" +
                "        foobar = 10\n"
                +
                "        foofoo = 20";
        //locals work because it will only get the locals that are before the cursor line
        ICompletionProposal[] proposals = requestCompl(s, s.length() - "foo = 20".length(), 1,
                new String[] { "foobar" });
        assertEquals(1, proposals.length);
    }

    public void testNoCompletionsForContext() throws Exception {
        String s = "class Foo(object):\n" +
                "    pass\n" +
                "class F(object):\n" +
                "    pass";
        //we don't want completions when we're declaring a class
        ICompletionProposal[] proposals = requestCompl(s, s.length() - "(object):\n    pass".length(), 0,
                new String[] {});
        assertEquals(0, proposals.length);
    }

    public void testClassmethod() throws Exception {
        String s0 = "class Foo:\n" +
                "    @classmethod\n" +
                "    def method1(cls, a, b):\n" +
                "        pass\n"
                +
                "    \n" +
                "Foo.met%s";

        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("method1(a, b)", p.getDisplayString());

        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "hod1(a, b)"), document.get());
    }

    public void testClassmethod2() throws Exception {
        String s0 = "class Foo:\n" +
                "    @classmethod\n" +
                "    def method1(cls, a, b):\n" +
                "        cls.m%s";

        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("method1(a, b)", p.getDisplayString());

        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "ethod1(a, b)"), document.get());
    }

    public void testClassmethod3() throws Exception {
        String s0 = "class Foo:\n" +
                "    def __init__(self):\n" +
                "        self.myvar = 10\n" +
                "\n"
                +
                "    def method3(self, a, b):\n" +
                "        pass\n" +
                "\n" +
                "    myvar3=10\n" +
                "    @classmethod\n"
                +
                "    def method2(cls, a, b):\n" +
                "        cls.myvar2 = 20\n" +
                "\n" +
                "    @classmethod\n"
                +
                "    def method1(cls, a, b):\n" +
                "        cls.m%s";

        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(5, proposals.length);
        assertContains("method1(a, b)", proposals);
        assertContains("method2(a, b)", proposals);
        assertContains("method3(self, a, b)", proposals);
        assertContains("myvar2", proposals);
        assertContains("myvar3", proposals);
    }

    public void testClassmethod4() throws Exception {
        String s0 = "from extendable.classmet.mod1 import Foo\n" +
                "Foo.Class%s";

        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("ClassMet()", p.getDisplayString());

        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "Met()"), document.get());
    }

    public void testRecursion() throws Exception {
        String s = "import testrec4\n" +
                "testrec4.url_for.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] { "m1(self)" });
        assertEquals(1, proposals.length);
    }

    public void testGlobal() throws Exception {
        String s = "class Log:\n" +
                "    def method1(self):\n" +
                "        pass\n" +
                "    \n" +
                "def main():\n"
                +
                "    global logger\n" +
                "    logger = Log()\n" +
                "    logger.method1()\n" +
                "    \n"
                +
                "def otherFunction():\n" +
                "    logger.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] { "method1()" });
        assertEquals(1, proposals.length);
    }

    public void testClassInNestedScope() throws Exception {
        String s = "def some_function():\n" +
                "   class Starter:\n" +
                "       def m1(self):\n" +
                "           pass\n"
                +
                "   Starter.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] { "m1(self)" });
        assertEquals(1, proposals.length);
    }

    public void testClassInNestedScope2() throws Exception {
        String s = "def some_function():\n" +
                "    class Starter:\n" +
                "        def m1(self):\n" +
                "            pass\n"
                +
                "    s = Starter()\n" +
                "    s.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] { "m1()" });
        assertEquals(1, proposals.length);
    }

    public void testGlobalClassInNestedScope() throws Exception {
        String s = "def some_function():\n" +
                "    class Starter:\n" +
                "        def m1(self):\n" +
                "            pass\n"
                +
                "    global s\n" +
                "    s = Starter()\n" +
                "\n" +
                "def foo():\n" +
                "    s.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] { "m1()" });
        assertEquals(1, proposals.length);
    }

    public void testAssign() throws Exception {
        String s = "class Foo(object):\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "class Bar(object):\n"
                +
                "    def bar(self):\n" +
                "        pass\n" +
                "    \n" +
                "def m1():\n" +
                "    if 1:\n"
                +
                "        c = Foo()\n" +
                "    elif 2:\n" +
                "        c = Bar()\n" +
                "    c.";
        requestCompl(s, s.length(), 2, new String[] { "foo()", "bar()" });
    }

    public void testAssign2() throws Exception {
        String s = "class Foo(object):\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "class Bar(object):\n"
                +
                "    def bar(self):\n" +
                "        pass\n" +
                "    \n" +
                "class KKK:\n" +
                "    def m1(self):\n"
                +
                "        self.c = Foo()\n" +
                "    def m2(self):\n" +
                "        self.c = Bar()\n"
                +
                "    def m3(self):\n" +
                "        self.c.";
        requestCompl(s, s.length(), 2, new String[] { "foo()", "bar()" });
    }

    public void testReturn() throws Exception {
        String s = "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "def m1():\n" +
                "    return Foo()\n"
                +
                "def m2():\n" +
                "    a = m1()\n" +
                "    a.";
        requestCompl(s, s.length(), 1, new String[] { "foo()" });
    }

    public void testReturn2() throws Exception {
        String s = "class Foo:\n" +
                "    def method10(self):\n" +
                "        pass\n" +
                "def some_function():\n"
                +
                "    class Starter:\n" +
                "        def m1(self):\n" +
                "            pass\n" +
                "    if 1:\n"
                +
                "        return Starter()\n" +
                "    else:\n" +
                "        return Foo()\n" +
                "    \n" +
                "def foo():\n"
                +
                "    some = some_function()\n" +
                "    some.";
        requestCompl(s, s.length(), 2, new String[] { "m1()", "method10()" });
    }

    public void testReturn3() throws Exception {
        String s = "class Foo:\n" +
                "    def method10(self):\n" +
                "        pass\n" +
                "def some_function():\n"
                +
                "    class Starter:\n" +
                "        def m1(self):\n" +
                "            pass\n" +
                "    def inner():\n"
                +
                "        return Starter()\n" + //ignore this one
                "    return Foo()\n" +
                "    \n" +
                "def foo():\n" +
                "    some = some_function()\n" +
                "    some.";
        requestCompl(s, s.length(), 1, new String[] { "method10()" });
    }

    public void testReturn4() throws Exception {
        String s = "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "def m1():\n" +
                "    return Foo()\n"
                +
                "def m2():\n" +
                "    m1().";
        requestCompl(s, s.length(), 1, new String[] { "foo()" });
    }

    public void testReturn5() throws Exception {
        String s = "import unittest\n" +
                "\n" +
                "\n" +
                "class Test(unittest.TestCase):\n" +
                "        \n"
                +
                "    def Compute(self):\n" +
                "        return Test()\n" +
                "\n" +
                "    def BB(self):\n"
                +
                "        self.Compute()." +
                "";
        ICompletionProposal[] requestCompl = requestCompl(s, s.length(), -1, new String[] { "BB()",
                "assertEquals(first, second, msg)" });
        boolean found = false;
        for (ICompletionProposal p : requestCompl) {
            if (p.getDisplayString().equals("assertEquals(first, second, msg)")) {
                IToken element = ((PyLinkedModeCompletionProposal) p).getElement();
                assertEquals(element.getType(), IToken.TYPE_FUNCTION);
                found = true;
            }
        }
        assertTrue(found);
    }

    public void testDecorateObject() throws Exception {
        String s = "class Foo:\n" +
                "    def bar():pass\n" +
                "foo = Foo()\n" +
                "foo.one = 1\n" +
                "foo.two =2\n"
                +
                "foo.";

        requestCompl(s, new String[] { "one", "two", "bar()" });
    }

    public void testShadeClassDeclaration() throws Exception {
        String s = "class Foo:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "    \n" +
                "Foo = Foo()\n" +
                "Foo.";

        //don't let the name override the definition -- as only the name will remain, we won't be able to find the original
        //one, so, it will find it as an unbound call -- this may be fixed later, but as it's a corner case, let's leave
        //it like that for now.
        requestCompl(s, new String[] { "m1()" });
    }

    public void testRecursion1() throws Exception {
        String s = "from testrec5.messages import foonotexistent\n" +
                "foonotexistent.";

        requestCompl(s, new String[] {});
    }

    public void testAssignErr() throws Exception {
        String s = "class ScalarBarManager:\n" +
                "    pass\n" +
                "manager = ScalarBarManager()\n"
                +
                "manager._scalar_bars[1].props\n" +
                "manager.";

        requestCompl(s, new String[] { "_scalar_bars" });
    }

    public void testInnerDefinition() throws Throwable {
        //NOTE: THIS TEST IS CURRENTLY EXPECTED TO FAIL!
        //testInnerDefinition2 is the same but gets the context correctly (must still check why this happens).
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        String s = "class Bar:\n" +
                "    \n" +
                "    class Foo:\n" +
                "        pass\n" +
                "    F"; //request at the Bar context
        try {
            requestCompl(s, new String[] { "Foo" });
        } catch (Throwable e) {
            if (e.getMessage() != null
                    && e.getMessage().indexOf("The string >>Foo<< was not found in the returned completions.") != -1) {
                fail("Expected to fail!");
            }
            throw e;
        }
    }

    public void testInnerDefinition2() throws Exception {
        String s = "class Bar:\n" +
                "    \n" +
                "    class Foo:\n" +
                "        pass\n" +
                "    \n" +
                "    F"; //request at the Bar context

        requestCompl(s, new String[] { "Foo" });
    }

    public void testClsCompletion() throws Exception {
        String s = "class myclass(object):\n" +
                "    def mymethod(self, hello):\n" +
                "        print hello\n"
                +
                "cls = myclass()\n" +
                "cls.m";

        requestCompl(s, new String[] { "mymethod(hello)" });
    }

    public void testWildImportWithAll() throws Exception {
        String s = "from extendable.all_check import *\n" +
                "This";

        requestCompl(s, new String[] { "ThisGoes", "ThisGoesToo" });
    }

    public void testRegularImportWithAll() throws Exception {
        String s = "from extendable.all_check import This";

        requestCompl(s, new String[] { "ThisGoes", "ThisGoesToo", "ThisDoesnt" });
    }

    public void testMultipleAssignCompletion() throws Exception {
        String s = "class A:\n" +
                "    def method1(self):\n" +
                "        pass\n" +
                "w,y = '', A()\n" +
                "y.";

        requestCompl(s, -1, new String[] { "method1()" });
    }

    public void testMultipleAssignCompletion2() throws Exception {
        String s = "class A:\n" +
                "    def method1(self):\n" +
                "        pass\n" +
                "w,y = [A(), '']\n" +
                "w.";

        requestCompl(s, -1, new String[] { "method1()" });
    }

    public void testMultipleAssignCompletion3() throws Exception {
        String s = "class A:\n" +
                "    def method1(self):\n" +
                "        pass\n" +
                "w = A()\n" +
                "w.";

        requestCompl(s, -1, new String[] { "method1()" });
    }

    public void testOuterSelf() throws Exception {
        String s = "class A:\n" +
                "    def method1(self):\n" +
                "        def m2():\n" +
                "            self."; //outer self

        requestCompl(s, -1, new String[] { "method1()" });
    }

    public void testNPEOnCompletion() throws Exception {
        String s = "def Foo(**kwargs):\n" +
                "    pass\n" +
                "\n" +
                "Foo(ah";

        requestCompl(s, -1, new String[] {});
    }

    public void testVarargsAndKwargsFound() throws Exception {
        String s = "class A:\n" +
                "    def method1(self, *args, **kwargs):\n" +
                "        ";

        requestCompl(s, -1, new String[] { "args", "kwargs" });
    }

    public void testInvalidNotFound() throws Exception {
        assertNull(nature.getAstManager().getModule("extendable.invalid-module", nature, true));
        assertNull(nature.getAstManager().getModule("extendable.invalid+module", nature, true));
    }

    public void testAcessInstanceOnClass() throws Exception {
        String s = "import testAssist.assist.ExistingClass\n" +
                "\n" +
                "class A:\n" +
                "    \n"
                +
                "    objects = testAssist.assist.ExistingClass()\n" +
                "" +
                "A.objects.";

        requestCompl(s, -1, new String[] { "existingMethod()" });
    }

    public void testNoImportOnLine() throws Exception {
        String s = "from testAssist import assist\n" +
                "import_export = assist.";

        requestCompl(s, -1, new String[] { "ExistingClass" });
    }

    public void testConfigObjEgg() throws Exception {
        String s = "import configobj\n" +
                "\n" +
                "configobj.";

        requestCompl(s, -1, new String[] { "__file__" });

    }

    public void testListItemAccess() throws Exception {
        String s;
        s = "" +
                "import testAssist.assist.ExistingClass\n" +
                "class Fooo:\n" +
                "  def __getitem__(self,k):\n"
                +
                "      return testAssist.assist.ExistingClass()\n\n" +
                "lst = Fooo()\n" +
                "lst[0].";
        requestCompl(s, -1, new String[] { "existingMethod()" });

        // if the type of the list item can't be inferred, expect an empty proposal list
        s = "" +
                "lst = list()\n" +
                "lst.append(1)\n" +
                "lst[0].";
        requestCompl(s, -1, new String[] {});
    }

    public void testAttributeAfterHasAttr() throws Exception {
        String s = "def m1(a)\n" +
                "    if hasattr(a, 'method'):\n" +
                "        a.";

        requestCompl(s, -1, new String[] { "method" });
    }

    public void testAttributeAfterHasAttr2() throws Exception {
        String s = "import extendable\n" +
                "def m1()\n" +
                "    if hasattr(extendable, 'method'):\n"
                +
                "        extendable.";

        requestCompl(s, -1, new String[] { "method" });
    }

    public void testPython30() throws Exception {
        String s = "def func(arg, *, arg2=None):\n" +
                "    ar" +
                "";
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_0;
            requestCompl(s, -1, new String[] { "arg", "arg2" });
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testCompletionUnderWithLowerPriority() throws Exception {
        String s = "class A:\n" +
                "    def __foo__(self):\n" +
                "        pass\n" +

        "    def _bar(self):\n" +
                "        pass\n" +
                "\n" +
                "class B(A):\n" +
                "    def m1(self):\n" +
                "        self." + //__foo should NOT be here!
                "";
        ICompletionProposal[] proposals = requestCompl(s, 3, new String[] { "m1()", "_bar()", "__foo__()" });
        assertEquals(proposals[0].getDisplayString(), "m1()");
        assertEquals(proposals[1].getDisplayString(), "_bar()");
        assertEquals(proposals[2].getDisplayString(), "__foo__()");
    }

    public void testOverrideCompletions() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "    \n" +
                "class Bar(Foo):\n"
                +
                "    def ";//bring override completions!
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "foo (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "    \n" +
                "class Bar(Foo):\n"
                +
                "    def foo(self):\n" +
                "        Foo.foo(self)", doc.get());
    }

    public void testOverrideCompletions2() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "    \n" +
                "class Bar(Foo):\n"
                +
                "    def fo";//bring override completions!
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "foo (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "    \n" +
                "class Bar(Foo):\n"
                +
                "    def foo(self):\n" +
                "        Foo.foo(self)", doc.get());
    }

    public void testOverrideCompletions3() throws Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String s;
        s = "" +
                "import unittest\n" +
                "class Bar(unittest.TestCase):\n" +
                "    def tearDow";//bring override completions!
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "tearDown (Override method in unittest.TestCase)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "import unittest\n" +
                "class Bar(unittest.TestCase):\n" +
                "    def tearDown(self):\n"
                +
                "        unittest.TestCase.tearDown(self)", doc.get());
    }

    public void testOverrideCompletions4() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    @classmethod\n" +
                "    def rara(cls):\n" +
                "        pass\n"
                +
                "class Bar(Foo):\n" +
                "    def ra";//bring override completions!
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "rara (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    @classmethod\n" +
                "    def rara(cls):\n" +
                "        pass\n"
                +
                "class Bar(Foo):\n" +
                "    @classmethod\n" +
                "    def rara(cls):\n"
                +
                "        super(Bar, cls).rara()", doc.get());
    }

    public void testOverrideCompletions5() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    #comment\n" +
                "    def rara(self):\n" +
                "        #comment\n" +
                "        pass\n"
                +
                "class Bar(Foo):\n" +
                "    def ra";//bring override completions!
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "rara (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    #comment\n" +
                "    def rara(self):\n" +
                "        #comment\n"
                +
                "        pass\n" +
                "class Bar(Foo):\n" +
                "    def rara(self):\n" +
                "        Foo.rara(self)",
                doc.get());
    }

    public void testCompletionsWithParametersFromAssign() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a, b):\n" +
                "        pass\n" +
                "    what = rara\n"
                +
                "f = Foo()\n" +
                "f.wha";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "what(a, b)" });
        assertEquals(1, comps.length);

    }

    public void testOverrideCompletions6() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a, b):\n" +
                "        pass\n" +
                "    what = rara\n" +
                "\n"
                +
                "class Bar(Foo):\n" +
                "    def wh" +
                "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "what (Override method in Foo)" });
        assertEquals(1, comps.length);
        Document doc = new Document(s);
        OverrideMethodCompletionProposal comp = (OverrideMethodCompletionProposal) comps[0];
        comp.applyOnDocument(null, doc, ' ', 0, s.length());
        assertEquals("" +
                "class Foo:\n" +
                "    def rara(self, a, b):\n" +
                "        pass\n" +
                "    what = rara\n"
                +
                "\n" +
                "class Bar(Foo):\n" +
                "    def what(self, a, b):\n" +
                "        Foo.what(self, a, b)",
                doc.get());
    }

    public void testGrammar2AbsoluteAndRelativeImports() throws Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "extendable/grammar3/sub1.py";
        String strDoc = "from relative import ";
        ICompletionProposal[] codeCompletionProposals = requestCompl(new File(file), strDoc, strDoc.length(), -1,
                new String[] { "NotFound" });
        assertNotContains("DTest", codeCompletionProposals);
    }

    public void testGrammar2GetRootsOnImport() throws Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "extendable/grammar3/sub1.py";
        String strDoc = "import zipf";
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[] { "zipfile" });
    }

    public void testGrammar2AbsoluteAndRelativeImportsWithFromFuture() throws Exception {
        String file = TestDependent.TEST_PYSRC_LOC +
                "extendable/grammar3/sub1.py";

        //Must behave as Py3
        String strDoc = "from __future__ import absolute_import\nfrom relative import ";
        ICompletionProposal[] codeCompletionProposals = requestCompl(new File(file), strDoc, strDoc.length(), -1,
                new String[] { "DTest" });
        assertNotContains("NotFound", codeCompletionProposals);
    }

    public void testDiscoverParamFromDocstring() throws Exception {
        String s;
        s = "" +
                "class Bar:\n" +
                "    def m1(self): pass\n" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':type a: Bar'\n" +
                "        a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testDiscoverReturnFromDocstring() throws Exception {
        String s;
        s = "" +
                "class Bar:\n" +
                "    def m1(self): pass\n" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype Bar'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testFindCompletionFromInstance() throws Exception {
        String s;
        s = "" +
                "class Class(object):\n" +
                "\n" +
                "    def method(self):\n" +
                "        if False:\n" +
                "            pass\n" +
                "\n" +
                "        elif True:\n" +
                "            self.completion = 10\n" +
                "\n" +
                "        self.comp" +
                "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "completion" });
        assertEquals(1, comps.length);
    }

    public void testTypeOnLocalVar() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "  def bar():pass\n"
                + "\n"
                + "def m1():\n"
                + "  n = somecall() #: :type n: F\n"
                + "  a = 10\n"
                + "  b = 20\n"
                + "  n."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "bar()" });
        assertEquals(1, comps.length);
    }

    public void testTypeOnLocalVar2() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "  def bar(self):\n"
                + "    self.n #: :type self.n: F\n"
                + "    self.n."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "bar()" });
        assertEquals(1, comps.length);
    }

    /**
     * Cases to work:
     *
     * x = [1, 2, 3]
     * x = (1, 2, 3)
     * x = set([1, 2, 3])
     * x = {'a': 1}
     * x = list({'a': 1}.iterkeys()) #py3
     * x = {'a': 1}.keys() #py2
     * x = list({'a': 1}.itervalues()) #py3
     * x = {'a': 1}.values() #py2
     *
     * class MyClass():
     *     def __iter__(self):
     *         yield 1
     *
     *     def __getitem__(self, i):
     *         return 1
     *
     * x = MyClass()
     *
     *
     * for a in x: __iter__
     *     a.
     *
     * a[0].  #__getitem__
     */
    public void testCodeCompletionForCompoundObjects1() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "a = [F()]\n"
                + "a[0]."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects2() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "a = [F()]\n"
                + "for x in a:\n"
                + "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects2a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "for x in [F()]:\n"
                + "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects3() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "def foo(a):\n"
                + "    ':type a: list[F]'\n"
                + "    for x in a:\n"
                + "        x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects3a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "def foo(a):\n"
                + "    ':type a: list(F)'\n"
                + "    for x in a:\n"
                + "        x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects4() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "class MyClass():\n"
                + "    def __iter__(self):\n"
                + "        yield G()\n"
                + "\n"
                + "x = MyClass()\n"
                + "\n"
                + "for a in x:\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects4a() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "class MyClass():\n"
                + "    def __getitem__(self):\n"
                + "        return G()\n"
                + "    def __len__(self):\n"
                + "        return 2\n"
                + "\n"
                + "x = MyClass()\n"
                + "\n"
                + "for a in x:\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects4b() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def mF(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "class MyClass():\n"
                + "    def __iter__(self):\n"
                + "        yield G()\n"
                + "\n"
                + "    def __getitem__(self, i):\n"
                + "        return F()\n"
                + "\n"
                + "x = MyClass()\n"
                + "\n"
                + "for a in x: #__iter__\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects4c() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "def MyMethod():\n"
                + "    return [G()]\n"
                + "\n"
                + "for a in MyMethod():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects5c() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "def MyMethod():\n"
                + "    return (G(),)\n"
                + "\n"
                + "for a in MyMethod():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects5d() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "def MyMethod():\n"
                + "    return {G(),}\n"
                + "\n"
                + "for a in MyMethod():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects5e() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "	       pass\n"
                + "		   \n"
                + "def MyMethod():\n"
                + "    return {G(),}\n"
                + "\n"
                + "x = MyMethod()\n"
                + "\n"
                + "for a in x:\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects5f() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def mF(self):\n"
                + "        pass\n"
                + "        \n"
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "        \n"
                + "class MyClass():\n"
                + "    def __iter__(self):\n"
                + "        yield G()\n"
                + "\n"
                + "    def __getitem__(self, i):\n"
                + "        return F()\n"
                + "\n"
                + "for a in MyClass(): #__iter__\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "for a in {G():'', G():''}:\n" // Default is iterating through dict keys
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6a() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "for a in {G():'', G():''}.keys():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6b() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "for a, b in {'':G(), '':G()}.items():\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6c() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "for a, b in {G():'', G():''}.items():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6d() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:dict(G, str)'\n"
                + "    for a, b in x.items():\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6e() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:dict(G, str)'\n"
                + "    for a in x.keys():\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6f() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:dict(str, G)'\n"
                + "    for a in x.values():\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6g() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "class X:\n"
                + "    def items(self):\n"
                + "        ':rtype: list(tuple(str, G))'\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:X'\n"
                + "    for a, b in x.items():\n"
                + "        b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6h() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "class X:\n"
                + "    def items(self):\n"
                + "        ':rtype: list((str, G))'\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:X'\n"
                + "    for a, b in x.items():\n"
                + "        b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects6i() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "class X:\n"
                + "    def items(self):\n"
                + "        ':rtype: list(tuple(str, G))'\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:X'\n"
                + "    for a, b in x.items():\n"
                + "        b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTuple() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "class X:\n"
                + "    def items(self):\n"
                + "        ':rtype: list(tuple(str, G))'\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:tuple(X, G)'\n"
                + "    a, b = x\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def check():\n"
                + "    x = [(G(), 1), (G(), 2)]\n"
                + "    for a, b in x:\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor2() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:list(tuple(G(), 1))'\n"
                + "    for a, b in x:\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor2a() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:list((G(), 1))'\n"
                + "    for a, b in x:\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor2b() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:((G(), 1))'\n"
                + "    for a, b in x:\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor2c() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:[(G(), 1)]'\n"
                + "    for a, b in x:\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionUnpackTupleInFor3() throws Exception {
        String s;
        s = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "\n"
                + "def ra():\n"
                + "    ':rtype: list(tuple(G(), 1))'\n"
                + "\n"
                + "def check():\n"
                + "    for a, b in ra():\n"
                + "        a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "mG()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "for x in  [F(i) for i in range(10)]:\n"
                + "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "for x in {F(i): str(i) for i in range(10)}:\n"
                + "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7b() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = {F(i): str(i) for i in range(10)}\n"
                + "for x in d:\n"
                + "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7c() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = {F(i): str(i) for i in range(10)}\n"
                + "for a, b in d.iteritems():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7c1() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = {i:F(i) for i in range(10)}\n"
                + "for a, b in d.iteritems():\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7d() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = {F(1): 1}\n"
                + "for a, b in d.iteritems():\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects7e() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = {1: F(1)}\n"
                + "for a, b in d.iteritems():\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects8() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "d = dict((1, F(1)) for i in xrange(10))\n"
                + "for a, b in d.iteritems():\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects9() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "d = [(1, F(1))]\n"
                + "for a, b in d:\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects10() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + ""
                + "d = F()\n"
                + "d.a = [(1, F(1))]\n"
                + "for a, b in d.a:\n"
                + "    b."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects11() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + ""
                + "x = [i for i in [F(1), F(2)]]\n"
                + "for k in x:\n"
                + "    k."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects11a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + ""
                + "x = dict(((i, str(i)) for i in [F(1), F(2)]))\n"
                + "for k, v in x.iteritems():\n"
                + "    k."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundObjects11b() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + ""
                + "x = dict(([i, str(i)] for i in [F(1), F(2)]))\n"
                + "for k, v in x.iteritems():\n"
                + "    k."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionInsideListComprehension() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [i. for i in [F()]]"
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length() - " for i in [F()]]".length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionInsideListComprehension2() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "i = F()\n"
                + "y = [i. for x in [10]]"
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length() - " for x in [10]]".length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionInsideListComprehension2a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "y = [(b. ,a) for (a, b) in [10, F()]]"
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length() - " ,a) for (a, b) in [10, F()]]".length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionInsideListComprehension3() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "\n"
                + "y = list((x. for x in [F()]))"
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length() - " for x in [F()]))".length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = list(F() for x in [xrange(10)])\n"
                + "for a in x:\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound1() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = tuple(F() for x in [xrange(10)])\n"
                + "for a in x:\n"
                + "    a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound2() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [F(i) for i in range(10)]\n"
                + "a = x[0]\n"
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound3() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [F(i) for i in range(10)]\n"
                + "a = x[-1]\n"
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound4() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [F(i) for i in range(10)]\n"
                + "a = x[50]\n" // The position shouldn't matter in this case...
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound5() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [None, F(i)]\n"
                + "a = x[1]\n" // The position matters here!
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound5b() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [None, F(i)]\n"
                + "a = x[-1]\n" // The position matters here!
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound5d() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [F(i), None]\n"
                + "a = x[-2]\n" // The position matters here!
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompound5a() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [None, F(i)]\n"
                + "a = x[0]\n" // The position matters here!
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] {});
        assertEquals(0, comps.length);
    }

    public void testCodeCompletionForCompound5c() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = [None, F(i)]\n"
                + "a = x[-2]\n" // The position matters here!
                + "a."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] {});
        assertEquals(0, comps.length);
    }

    public void testCodeCompletionForCompoundSet() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "a = set([F()])\n"
                + "for x in a:\n" +
                "    x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionForCompoundNotInNamespace() throws Exception {
        String s;
        s = ""
                + "import threading\n"
                + "def foo(a):\n"
                + "  ':param list(threading.Thread) a:'\n"
                + "  for x in a:\n" +
                "      x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "run()" });
        assertTrue(comps.length > 10);
    }

    public void testCodeCompletionForCompoundNotInNamespace2() throws Exception {
        registerThreadGlobalCompletion();

        String s;
        s = ""
                + "def foo(a):\n"
                + "  ':param list(threading.Thread) a:'\n"
                + "  for x in a:\n" +
                "      x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "run()" });
        assertTrue(comps.length == 1);
    }

    public void testCodeCompletionForCompoundNotInNamespace3() throws Exception {
        registerThreadGlobalCompletion();

        String s;
        s = ""
                + "def foo(a):\n"
                + "  ':param list(Thread) a:'\n"
                + "  for x in a:\n" +
                "      x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "run()" });
        assertTrue(comps.length == 1);
    }

    public void testCodeCompletionForCompoundInNamespace4() throws Exception {
        String s;
        s = ""
                + "import threading\n"
                + "def foo():\n"
                + "  a = [threading.Thread()]\n"
                + "  for x in a:\n" +
                "      x."
                + "";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "run()" });
        assertTrue(comps.length > 10);
    }

    public void testCodeCompletionFromInstanceGivenParameterType() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "class Person(object):\n" +
                "    def __init__(self, foo):\n" +
                "        '''\n" +
                "        :type foo: Foo\n" +
                "        '''\n" +
                "        self.foo = foo\n" +
                "        self.foo.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

    public void testCodeCompletionFromInstanceGivenVariableType() throws Exception {
        String s;
        s = "" +
                "class Foo:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "\n" +
                "class Person(object):\n" +
                "    def __init__(self):\n" +
                "        self.foo = foo #: :type foo: Foo\n" +
                "        self.foo.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1,
                new String[] { "m1()" });
        assertEquals(1, comps.length);
    }

}
