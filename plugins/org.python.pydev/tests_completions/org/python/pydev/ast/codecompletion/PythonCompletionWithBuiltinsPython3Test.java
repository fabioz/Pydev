/**
 * Copyright (c) 2016 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.ast.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.PythonShell;
import org.python.pydev.ast.codecompletion.shell.PythonShellTest;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.nature.PythonNature;

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
        super.setUp();

        ADD_MX_TO_FORCED_BUILTINS = false;

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.PYTHON_30_LIB, false);

        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if (shell == null) {
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);
        IModule builtinMod = nature.getBuiltinMod();
        if (builtinMod == null) {
            throw new AssertionError("builtins not found");
        }

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

}
