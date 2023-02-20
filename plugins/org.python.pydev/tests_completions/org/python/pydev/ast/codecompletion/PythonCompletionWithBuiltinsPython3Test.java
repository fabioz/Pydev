/**
 * Copyright (c) 2016 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.ast.codecompletion;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.PythonShell;
import org.python.pydev.ast.codecompletion.shell.PythonShellTest;
import org.python.pydev.core.BaseModuleRequest;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.preferences.InterpreterGeneralPreferences;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;

public class PythonCompletionWithBuiltinsPython3Test extends CodeCompletionTestsBase {

    protected boolean isInTestFindDefinition = false;

    public static void main(String[] args) {
        try {
            PythonCompletionWithBuiltinsPython3Test builtins = new PythonCompletionWithBuiltinsPython3Test();
            builtins.setUp();
            builtins.testCodeCompletionPep484Str();
            builtins.tearDown();

            junit.textui.TestRunner.run(PythonCompletionWithBuiltinsPython3Test.class);

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
                return IPythonNature.GRAMMAR_PYTHON_VERSION_3_8;
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

    @Override
    protected boolean isPython3Test() {
        return true;
    }

    private static PythonShell shell;

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        InterpreterGeneralPreferences.FORCE_USE_TYPESHED = true;

        super.setUp();

        ADD_MX_TO_FORCED_BUILTINS = false;

        CompiledModule.COMPILED_MODULES_ENABLED = true;

        String paths = TestDependent.PYTHON_LIB;
        if (TestDependent.PYTHON38_QT5_PACKAGES != null) {
            paths += "|" + TestDependent.PYTHON38_QT5_PACKAGES;
        }

        this.restorePythonPath(paths, false);

        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if (shell == null) {
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);
        IModule builtinMod = nature.getBuiltinMod(new BaseModuleRequest(false));
        if (builtinMod == null) {
            throw new AssertionError("builtins not found");
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        InterpreterGeneralPreferences.FORCE_USE_TYPESHED = null;
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), null);
        super.tearDown();
    }

    public void testCodeCompletionPep484Str() throws Exception {
        String s;
        s = ""
                + "def seek(s:str):\n"
                + "    s.t"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484Dict() throws Exception {
        String s;
        s = ""
                + "from typing import Dict\n"
                + "def seek(s:Dict[str, int]):\n"
                + "    s.i"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "items()" });
    }

    public void testCodeCompletionPep484Set() throws Exception {
        String s;
        s = ""
                + "from typing import Set\n"
                + "def seek(s:Set[str]):\n"
                + "    s.a"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "add(element)" });
    }

    public void testCodeCompletionPep484List() throws Exception {
        String s;
        s = ""
                + "from typing import List\n"
                + "def seek(s:List[str]):\n"
                + "    s.app"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "append(object)" });
    }

    public void testCodeCompletionPep484DefaultDict() throws Exception {
        String s;
        s = ""
                + "from typing import DefaultDict\n"
                + "def seek(s:DefaultDict[str, int]):\n"
                + "    s.i"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "items()" });
    }

    public void testCodeCompletionPep484DictUnpack() throws Exception {
        String s;
        s = ""
                + "from typing import Dict\n"
                + "def seek(s:Dict[str, int]):\n"
                + "    for a, b in s.items():\n"
                + "        a.t"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484DictUnpack2() throws Exception {
        String s;
        s = ""
                + "from typing import Dict\n"
                + "def seek(s:Dict[int, str]):\n"
                + "    for a, b in s.items():\n"
                + "        b.t"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484ListUnpack() throws Exception {
        String s;
        s = ""
                + "from typing import List\n"
                + "def seek(s:List[str]):\n"
                + "    for a in s:\n"
                + "        a."
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484ListUnpack2() throws Exception {
        String s;
        s = ""
                + "from typing import List\n" +
                "\n" +
                "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def list_bar() -> List[str]:\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    for a in list_bar():\n" +
                "        a.t";
        requestCompl(s, s.length(), -1,
                new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484ListUnpack3() throws Exception {
        String s;
        s = "" +
                "def list_bar() -> str:\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    for a in list_bar():\n" +
                "        a.t";
        requestCompl(s, s.length(), -1,
                new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484ListUnpack4() throws Exception {
        String s;
        s = ""
                + "from typing import List\n" +
                "\n" +
                "class Bar(object):\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def list_bar() -> List[Bar]:\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    for a in list_bar():\n" +
                "        a.";
        requestCompl(s, s.length(), -1,
                new String[] { "bar()" });
    }

    public void testCodeCompletionPep484ListUnpack5() throws Exception {
        String s;
        s = ""
                + "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def list_bar():\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    lst = list_bar()\n" +
                "    a:List[Bar] = lst\n" +
                "    for b in a:\n" +
                "        b.";
        requestCompl(s, s.length(), -1,
                new String[] { "bar()" });
    }

    public void testCodeCompletionPep484ListUnpack6() throws Exception {
        String s;
        s = "" +
                "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def list_bar():\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    lst = list_bar()\n" +
                "    a:List[Bar] = lst\n" +
                "    b = a[0]\n" +
                "    b.";
        requestCompl(s, s.length(), -1,
                new String[] { "bar()" });
    }

    public void testCodeCompletionPep484DictUnpack3() throws Exception {
        String s;
        s = ""
                + "from typing import Dict\n"
                + "\n"
                + "def call() -> Dict[int, str]:\n"
                + "    pass\n"
                + "\n"
                + "def seek():\n"
                + "    x = call()\n"
                + "    for a, b in x.items():\n"
                + "        b.t"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484Return() throws Exception {
        String s;
        s = ""
                + "def call() -> str:\n"
                + "    pass\n"
                + "\n"
                + "def seek():\n"
                + "    a = call()\n"
                + "    a.t"
                + "";
        requestCompl(s, s.length(), -1, new String[] { "title()", "translate(table)" });
    }

    public void testCodeCompletionPep484Return2() throws Exception {
        String s;
        s = ""
                + "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "def list_bar():\n" +
                "    pass\n" +
                "\n" +
                "def something():\n" +
                "    lst = list_bar()\n" +
                "    a:Bar = lst[0]\n" +
                "    a.";
        requestCompl(s, s.length(), -1, new String[] { "bar()" });
    }

    public void testCompletionWithWalrus() throws Exception {
        String s;
        String original = "" +
                "def test_anything():\n" +
                "    while a := 10:\n" +
                "        a.d";
        s = StringUtils.format(original, "");
        requestCompl(s, s.length(), -1,
                new String[] { "denominator" });
    }

    public void testCompletionWithWalrus2() throws Exception {
        String s;
        String original = "" +
                "while (x := 10) > 5:\n" +
                "  x.d";
        s = StringUtils.format(original, "");
        requestCompl(s, s.length(), -1,
                new String[] { "denominator" });
    }

    public void testCompletionWithWalrus3() throws Exception {
        String s;
        String original = "" +
                "if x := 10:\n" +
                "  x.d";
        s = StringUtils.format(original, "");
        requestCompl(s, s.length(), -1,
                new String[] { "denominator" });
    }

    public void testCompletionWithWalrus4() throws Exception {
        String s;
        String original = "" +
                "if (x := 10) > 5:\n" +
                "  x.d";
        s = StringUtils.format(original, "");
        requestCompl(s, s.length(), -1,
                new String[] { "denominator" });
    }

    public void testBuiltins() throws Exception {
        IModule builtinModTypeshed = nature.getBuiltinMod(new BaseModuleRequest(true));
        IModule builtinModCompiled = nature.getBuiltinMod(new BaseModuleRequest(false));

        assertNotSame(builtinModTypeshed, builtinModCompiled);
        TokensList globalTokensTypeshed = builtinModTypeshed.getGlobalTokens();
        TokensList globalTokensCompiled = builtinModCompiled.getGlobalTokens();
        Set<String> typeshedNames = new HashSet<String>();
        Set<String> compiledNames = new HashSet<String>();
        Iterator<IterTokenEntry> iterator = globalTokensTypeshed.iterator();
        while (iterator.hasNext()) {
            typeshedNames.add(iterator.next().getToken().getRepresentation());
        }
        iterator = globalTokensCompiled.iterator();
        while (iterator.hasNext()) {
            compiledNames.add(iterator.next().getToken().getRepresentation());
        }
        Set<String> ignore = new HashSet<>(Arrays.asList(
                "__spec__",
                "__debug__",
                "__package__",
                "__doc__",
                "__class__",
                "__build_class__",
                "__loader__"));
        String error = "";
        for (String s : compiledNames) {
            if (ignore.contains(s)) {
                continue;
            }

            if (!typeshedNames.contains(s)) {
                error += ("Did not find: " + s + "\n");
            }
        }
        if (!error.isEmpty()) {
            fail(error + "\nAvailable:\n" + typeshedNames);
        }
    }

    public void testTypeshed() throws Exception {
        String s;
        s = "" +
                "def main():\n"
                + "    from re import RegexFlag\n"
                + "    RegexFlag.";
        requestCompl(s, s.length(), -1, new String[] { "ASCII" });
    }

    public void testContextManagerCompletion() throws Exception {
        String s = "import contextlib\n"
                + "from typing import Iterator\n"
                + "@contextlib.contextmanager\n"
                + "def context() -> Iterator[int]:\n"
                + "    yield 1\n"
                + "with context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion2() throws Exception {
        String s = "import contextlib\n"
                + "from typing import Iterator\n"
                + "@contextlib.contextmanager\n"
                + "def context():\n"
                + "    yield 1\n"
                + "with context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion3() throws Exception {
        String s = "from typing import ContextManager\n"
                + "class Context(ContextManager[int]):\n"
                + "    def __enter__(self) -> int:\n"
                + "        return 1\n"
                + "    def __exit__(self, exc_type, exc_val, exc_tb) -> None:\n"
                + "        return\n"
                + "with Context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion4() throws Exception {
        String s = "class Context():\n"
                + "    def __enter__(self) -> int:\n"
                + "        return 1\n"
                + "    def __exit__(self, exc_type, exc_val, exc_tb) -> None:\n"
                + "        return\n"
                + "with Context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion5() throws Exception {
        String s = "class Context():\n"
                + "    def __enter__(self) -> int:\n"
                + "        pass\n"
                + "    def __exit__(self, exc_type, exc_val, exc_tb) -> None:\n"
                + "        return\n"
                + "with Context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion6() throws Exception {
        String s = "class Context():\n"
                + "    def __enter__(self):\n"
                + "        return 1\n"
                + "    def __exit__(self, exc_type, exc_val, exc_tb) -> None:\n"
                + "        return\n"
                + "with Context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion7() throws Exception {
        String s = "def context() -> int:\n"
                + "    pass\n"
                + "with context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testContextManagerCompletion8() throws Exception {
        String s = "from typing import Iterator\n"
                + "def context() -> Iterator[int]:\n"
                + "    pass\n"
                + "with context() as ctx:\n"
                + "    ctx.";
        requestCompl(s, s.length(), -1, new String[] { "denominator" });
    }

    public void testSubclassWithGenericTypes() throws Exception {
        String s;
        s = "" +
                "from typing import Generic, AnyStr\n"
                + "class ClassWithWrite(Generic[AnyStr]):\n"
                + "    def write(self, s: AnyStr) -> int:\n"
                + "        pass\n"
                + "class SubClass(ClassWithWrite[str]):\n"
                + "    def some_method(self) -> str:\n"
                + "        pass\n"
                + "c = SubClass()\n"
                + "c.";
        requestCompl(s, s.length(), -1, new String[] { "write(s)", "some_method()", "capitalize()" });
    }

    public void testSubclassWithGenericTypes2() throws Exception {
        String s = ""
                + "import typing\n"
                + "class SubClass(typing.TextIO):\n"
                + "    def some_method(self) -> str:\n"
                + "        pass\n"
                + "c = SubClass()\n"
                + "c.";
        requestCompl(s, s.length(), -1, new String[] { "close()", "some_method()" });
    }

    public void testPyQt5() throws Exception {
        if (TestDependent.PYTHON38_QT5_PACKAGES != null) { //we can only test what we have
            //check for builtins with reference..3
            String s = "" +
                    "from PyQt5.QtWidgets import *\n" +
                    "                \n" +
                    "q = QLabel()    \n" +
                    "q.";
            requestCompl(s, s.length(), -1, new String[] { "acceptDrops()", "clear()" });
        }
    }

}
