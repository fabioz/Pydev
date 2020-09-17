/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This tests the 'whole' code completion, passing through all modules.
 *
 * @author Fabio Zadrozny
 */
public class PythonCompletionWithoutBuiltinsGrammar3Test extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            PythonCompletionWithoutBuiltinsGrammar3Test test = new PythonCompletionWithoutBuiltinsGrammar3Test();
            test.setUp();
            test.testGrammar3AbsoluteAndRelativeImports();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionWithoutBuiltinsGrammar3Test.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath("", false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }

        };
    }

    @Override
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        //No checks: created it without a system pythonpath
    }

    @Override
    protected void checkSize() {
        //No checks: created it without a system pythonpath
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        PyCodeCompletion.onCompletionRecursionException = null;
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
        };
    }

    public void testGrammar3AbsoluteAndRelativeImports() throws Exception {
        String file = TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/grammar3/sub1.py";
        String strDoc = "from relative import ";
        ICompletionProposalHandle[] codeCompletionProposals = requestCompl(new File(file), strDoc, strDoc.length(), -1,
                new String[] { "DTest" });
        assertNotContains("NotFound", codeCompletionProposals);
    }

    public void testDictAccess() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "class Starship:\n" +
                "    stats: Dict[A, A] = {}\n" +
                "    for key, val in stats.items():\n" +
                "        key.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }

    public void testListAccess() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "primes: List[A] = []\n"
                + "for p in primes:\n"
                + "    p.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }

    public void testListAccess2() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "primes: List[A] = []\n"
                + "primes[0].";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }

    public void testAssignCompletionWithTypeAsString() throws Exception {
        String s = ""
                + "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def method():\n" +
                "    foo: 'Bar'\n" +
                "    foo.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("bar()", prop.getDisplayString());
    }

    public void testParamTypeInfoAsString() throws Exception {
        String s = ""
                + "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def method(a: 'Bar'):\n" +
                "    a.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("bar()", prop.getDisplayString());
    }

    public void testParamTypeInfoAsString2() throws Exception {
        String s = ""
                + "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def method(a: 'List[Bar]'):\n" +
                "    for b in a:\n" +
                "        b.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("bar()", prop.getDisplayString());
    }

    public void testParamTypeInfoAsString3() throws Exception {
        String s = ""
                + "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "class MyClass(object):\n" +
                "    def __init__(self, a: 'Bar'):\n" +
                "        self.bar = a\n" +
                "        self.bar.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("bar()", prop.getDisplayString());
    }

    public void testCompletionForOptional() throws Exception {
        String s = ""
                + "from typing import Protocol, Optional\n" +
                "\n" +
                "class IFoo(Protocol):\n" +
                "    def some_method(self) -> bool:\n" +
                "        pass\n" +
                "\n" +
                "def method() -> Optional[IFoo]:\n" +
                "    pass\n" +
                "\n" +
                "a = method()\n" +
                "a.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("some_method()", prop.getDisplayString());
    }

    public void testCompletionForOptional2() throws Exception {
        String s = ""
                + "from typing import Protocol, Optional\r\n" +
                "\n" +
                "class IFoo(Protocol):\n" +
                "    def some_method(self) -> bool:\n" +
                "        pass\n" +
                "def method():\n" +
                "    pass\n" +
                "\n" +
                "a: Optional[IFoo] = method()\n" +
                "a.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("some_method()", prop.getDisplayString());
    }

    public void testCompletionWithWalrus() throws Exception {
        String s;
        String original = "class Foo(object):\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def test_anything():\n" +
                "    if a := Foo():\n" +
                "        a.";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("foo()", proposals[0].getDisplayString());
    }
}
