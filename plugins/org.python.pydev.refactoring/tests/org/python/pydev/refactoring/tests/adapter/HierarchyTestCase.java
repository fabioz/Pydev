/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.adapter;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;
import org.python.pydev.editor.codecompletion.shell.PythonShellTest;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

public class HierarchyTestCase extends CodeCompletionTestsBase {

    public HierarchyTestCase(String name) {
        super(name);
    }

    private static PythonShell shell;

    File file = new File(TestDependent.TEST_PYDEV_REFACTORING_PLUGIN_LOC
            + "tests/python/adapter/classdefwithbuiltins/testBaseClass2.py");

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.GetCompletePythonLib(true) + "|" + file.getParent(), false);
        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if (shell == null) {
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, shell);

    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, null);
    }

    public void testHierarchyWithBuiltins() throws Throwable {

        ModuleAdapter module = VisitorFactory.createModuleAdapter(new PythonModuleManager(nature), file, new Document(
                REF.getFileContents(file)), nature, nature);

        List<IClassDefAdapter> classes = module.getClasses();
        assertEquals(1, classes.size());
        List<IClassDefAdapter> baseClasses = classes.get(0).getBaseClasses();

        HashSet<String> actual = new HashSet<String>();
        for (IClassDefAdapter adapter : baseClasses) {
            actual.add(adapter.getName());
        }
        HashSet<String> expected = new HashSet<String>();
        expected.add("MyList2");
        expected.add("__builtin__.list");
        expected.add("MyListBase");

        assertEquals(expected, actual);
    }

}
