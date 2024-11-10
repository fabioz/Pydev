/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.adapter;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.adapters.IClassDefAdapter;
import org.python.pydev.ast.adapters.ModuleAdapter;
import org.python.pydev.ast.adapters.PythonModuleManager;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.PythonShell;
import org.python.pydev.ast.codecompletion.shell.PythonShellTest;
import org.python.pydev.core.ShellId;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.io.FileUtils;

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
    @Override
    public void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.getCompletePythonLib(true, isPython3Test()) + "|" + file.getParent(),
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
        ShellId shellId = AbstractShell.getShellId();
        AbstractShell.putServerShell(nature, shellId, null);
        super.tearDown();
    }

    public void testHierarchyWithBuiltins() throws Throwable {

        ModuleAdapter module = org.python.pydev.ast.adapters.visitors.VisitorFactory
                .createModuleAdapter(new PythonModuleManager(nature), file, new Document(
                        FileUtils.getFileContents(file)), nature, nature);

        List<IClassDefAdapter> classes = module.getClasses();
        assertEquals(1, classes.size());
        List<IClassDefAdapter> baseClasses = classes.get(0).getBaseClasses();

        HashSet<String> actual = new HashSet<String>();
        for (IClassDefAdapter adapter : baseClasses) {
            actual.add(adapter.getName());
        }
        HashSet<String> expected = new HashSet<String>();
        expected.add("Reversible");
        expected.add("MyList2");
        expected.add("Iterable");
        expected.add("MutableSequence");
        expected.add("Sequence");
        expected.add("list");
        expected.add("MyListBase");
        expected.add("object");

        assertEquals(expected, actual);
    }

}
