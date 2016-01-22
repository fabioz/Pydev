/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;

/**
 * @author Fabio
 *
 */
public class FindActualDefinitionTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        try {
            FindActualDefinitionTest test = new FindActualDefinitionTest();
            test.setUp();
            test.testFindActualDefinition3();
            test.tearDown();
            junit.textui.TestRunner.run(FindActualDefinitionTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        restorePythonPath(false);
    }

    public void testFindActualDefinition() throws Exception {
        String str = "" +
                "class Test(unittest.TestCase):\n" +
                "\n" +
                "    def testCountCalls(self):\n"
                +
                "        string_io = StringIO()\n" +
                "        printed = string_io.getvalue()\n"
                +
                "        remove_chars = [',', '(', ')', ':']\n" +
                "        for c in remove_chars:\n"
                +
                "            printed = printed.replace(c, '')\n" +
                "";
        IModule mod = SourceModule.createModuleFromDoc(null, new Document(str), nature);
        ICompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        PyRefactoringFindDefinition.findActualDefinition(null, mod, "printed.replace", selected, 8, 32, nature,
                completionCache);
        assertEquals(0, selected.size());
    }

    public void testFindActualDefinition2() throws Exception {
        String str = "" +
                "class Test(unittest.TestCase):\n" +
                "\n" +
                "    def testCountCalls(self):\n"
                +
                "        exp_format = '%*.*e' % (exp_format_digits + 8, exp_format_digits, datum)\n"
                +
                "        mantissa, _exponent_str = exp_format.split('e')\n"
                +
                "        mantissa = mantissa.strip().rjust(exp_format_digits + 3)\n" +
                "";
        IModule mod = SourceModule.createModuleFromDoc(null, new Document(str), nature);
        ICompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        PyRefactoringFindDefinition.findActualDefinition(null, mod, "mantissa.strip", selected, 6, 20, nature,
                completionCache);
        assertEquals(0, selected.size());
    }

    public void testFindActualDefinition3() throws Exception {
        String str = "" +
                "class Test(unittest.TestCase):\n" +
                "\n" +
                "    def testCountCalls(self):\n" +
                "        parent = self.root\n" +
                "        if name == '':\n" +
                "            result = parent\n" +
                "        else:\n" +
                "            parts = name.split('/')\n" +
                "            for i_part in parts:\n" +
                "                result = parent.find(i_part)\n" +
                "                parent = result\n" +
                "        return result\n" +
                "\n" +
                "";
        IModule mod = SourceModule.createModuleFromDoc(null, new Document(str), nature);
        ICompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        PyRefactoringFindDefinition.findActualDefinition(null, mod, "parent.find", selected, 10, 33, nature,
                completionCache);
        assertEquals(0, selected.size());
    }

    public void testFindActualDefinition4() throws Exception {
        String str = ""
                + "class G:\n"
                + "    def mG(self):\n"
                + "        pass\n"
                + "     \n"
                + "\n"
                + "class X:\n"
                + "    def items(self):\n"
                + "        ':rtype: list(str, G)'\n"
                + "     \n"
                + "\n"
                + "def check(x):\n"
                + "    ':type x:X'\n"
                + "    for a, b in x.items():\n" //should get the items following the user hints.
                + "        b.mG()"
                + "";
        IModule mod = SourceModule.createModuleFromDoc(null, new Document(str), nature);
        ICompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        PyRefactoringFindDefinition.findActualDefinition(null, mod, "x.items", selected, 13, 23, nature,
                completionCache);
        assertEquals(1, selected.size());
    }
}
