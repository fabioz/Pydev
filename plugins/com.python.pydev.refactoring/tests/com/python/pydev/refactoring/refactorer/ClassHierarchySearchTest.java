/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 11, 2006
 */
package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class ClassHierarchySearchTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {
        try {
            ClassHierarchySearchTest test = new ClassHierarchySearchTest();
            test.setUp();
            test.testFindHierarchy9();
            test.tearDown();

            junit.textui.TestRunner.run(ClassHierarchySearchTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Refactorer refactorer;
    private static File baseDir;
    private static File baseDir2;

    @Override
    public void setUp() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        SourceModule.TESTING = true;

        refactorer = new Refactorer();
        if (baseDir != null && !baseDir.exists()) {
            baseDir.mkdirs();
        }
        if (baseDir2 != null && !baseDir2.exists()) {
            baseDir2.mkdirs();
        }
        super.setUp();

    }

    @Override
    public String getProjectPythonpath() {
        if (baseDir == null) {
            //This will be called only once for the class and we want to use the same path over and over...
            baseDir = FileUtils.getTempFileAt(new File("."), "data_temp_class_hierarchy_search_test");
            if (baseDir.exists()) {
                try {
                    FileUtils.deleteDirectoryTree(baseDir);
                } catch (IOException e) {
                    Log.log(e);
                }
            }
            baseDir.mkdir();
        }
        return super.getProjectPythonpath() + "|" + baseDir.getAbsolutePath();
    }

    @Override
    public String getProjectPythonpathNature2() {
        if (baseDir2 == null) {
            //This will be called only once for the class and we want to use the same path over and over...
            baseDir2 = FileUtils.getTempFileAt(new File("."), "data_temp_class_hierarchy_search_test");
            if (baseDir2.exists()) {
                try {
                    FileUtils.deleteDirectoryTree(baseDir2);
                } catch (IOException e) {
                    Log.log(e);
                }
            }
            baseDir2.mkdir();
        }
        return super.getProjectPythonpathNature2() + "|" + baseDir2.getAbsolutePath();
    }

    @Override
    protected String getNameToCacheNature() {
        return "ClassHierarchySearchTest.testProjectStub";
    }

    @Override
    protected String getNameToCacheNature2() {
        return "ClassHierarchySearchTest.testProjectStub2";
    }

    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        SourceModule.TESTING = false;

        ProjectModulesManager projectModulesManager = ((ProjectModulesManager) nature.getAstManager()
                .getModulesManager());
        projectModulesManager.doRemoveSingleModule(new ModulesKey("foo", null));
        projectModulesManager.doRemoveSingleModule(new ModulesKey("foo0", null));
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn1", null));
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn10", null));

        projectModulesManager = ((ProjectModulesManager) nature2.getAstManager().getModulesManager());
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn2", null));
        projectModulesManager.doRemoveSingleModule(new ModulesKey("fooIn20", null));

        FileUtils.deleteDirectoryTree(baseDir);
        FileUtils.deleteDirectoryTree(baseDir2);

        super.tearDown();
    }

    public void testFindHierarchy() throws Throwable {
        final int line = 1;
        final int col = 9;

        //        System.out.println("START ------------");
        //        System.out.println("START ------------");
        //        System.out.println("START ------------");
        //        System.out.println("START ------------");
        RefactoringRequest request = setUpFooModule(line, col);

        try {
            //            RefactorerFinds.DEBUG = true;
            //            AbstractAdditionalDependencyInfo.DEBUG = true;

            HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
            assertEquals("Bar", node.name);
            assertTrue(node.moduleName.startsWith("foo"));

            assertIsIn("Pickler", "pickle", node.parents);
            assertIsIn("Foo", node.moduleName, node.children);
        } finally {
            //            System.out.println("END ------------");
            //            System.out.println("END ------------");
            //            System.out.println("END ------------");
            //            System.out.println("END ------------");
            //            AbstractAdditionalDependencyInfo.DEBUG = false;
            //            RefactorerFinds.DEBUG = false;
        }
    }

    public void testFindHierarchy2() {
        final int line = 3;
        final int col = 9;

        RefactoringRequest request = setUpFooModule(line, col);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("Foo", node.name);
        assertTrue(node.moduleName.startsWith("foo"));

        HierarchyNodeModel model = assertIsIn("Bar", node.moduleName, node.parents);
        assertIsIn("Pickler", "pickle", model.parents);

    }

    public void testFindHierarchy3() {
        String str = "" +
                "import pickle             \n" +
                "class Bar:\n" +
                "    pass                  \n"
                +
                "class Foo(Bar, pickle.Pickler):\n" +
                "    pass                  \n" +
                "\n" +
                "";
        final int line = 3;
        final int col = 9;

        RefactoringRequest request = setUpFooModule(line, col, str);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("Foo", node.name);
        assertTrue(node.moduleName.startsWith("foo"));

        assertIsIn("Bar", node.moduleName, node.parents);
        assertIsIn("Pickler", "pickle", node.parents);

    }

    public void testFindHierarchy4() {
        String str = "" +
                "class Bar:                \n" +
                "    pass                  \n"
                +
                "class Foo(Bar):           \n" +
                "    pass                  \n" +
                "class Foo1(Foo):          \n"
                +
                "    pass                  \n" +
                "\n" +
                "";
        final int line = 0;
        final int col = 8;

        RefactoringRequest request = setUpFooModule(line, col, str);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("Bar", node.name);
        assertTrue(node.moduleName.startsWith("foo"));

        node = assertIsIn("Foo", node.moduleName, node.children);
        assertIsIn("Foo1", node.moduleName, node.children);

    }

    public void testFindHierarchy5() {
        String str = "" +
                "class Root(object):\n" +
                "    pass\n" +
                "class Mid1(Root):\n" +
                "    pass\n"
                +
                "class Mid2(Root):\n" +
                "    pass\n" +
                "class Leaf(Mid1, Mid2):\n" +
                "    pass\n" +
                "\n" +
                "";

        final int line = 6;
        final int col = 8;

        RefactoringRequest request = setUpFooModule(line, col, str);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("Leaf", node.name);
        assertTrue(node.moduleName.startsWith("foo"));

        HierarchyNodeModel mid1 = assertIsIn("Mid1", node.moduleName, node.parents);
        HierarchyNodeModel mid2 = assertIsIn("Mid2", node.moduleName, node.parents);
        assertIsIn("Root", node.moduleName, mid1.parents);
        HierarchyNodeModel root = assertIsIn("Root", node.moduleName, mid2.parents);
        assertIsIn("object", null, root.parents);

    }

    public void testFindHierarchy6() {
        String str = "" +
                "class Root(object):\n" +
                "    pass\n" +
                "class Mid1(Root):\n" +
                "    pass\n"
                +
                "class Mid2(Root):\n" +
                "    pass\n" +
                "class Leaf(Mid1, Mid2):\n" +
                "    pass\n" +
                "import pickle\n"
                +
                "class Bla(Leaf, Foo):\n" +
                "    pass\n" +
                "class Foo:\n" +
                "    pass\n" +
                "";

        final int line = 9;
        final int col = 8;

        RefactoringRequest request = setUpFooModule(line, col, str);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("Bla", node.name);
        assertTrue(node.moduleName.startsWith("foo"));

        HierarchyNodeModel foo = assertIsIn("Foo", node.moduleName, node.parents);
        assertEquals(0, foo.parents.size());

    }

    public void testFindHierarchy8() {
        String str = "class FooIn1(object):pass\n";
        String str2 = "from fooIn1 import FooIn1\nclass FooIn2(FooIn1):pass\n";

        final int line = 0;
        final int col = 8;

        RefactoringRequest request;
        request = setUpModule(line, col, str2, "fooIn2", nature2);
        request = setUpModule(line, col, str, "fooIn1", nature);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("FooIn1", node.name);
        assertTrue(node.moduleName.startsWith("fooIn1"));

        HierarchyNodeModel foo = assertIsIn("FooIn2", "fooIn2", node.children);
        assertEquals(0, foo.parents.size());
    }

    public void testFindHierarchy9() {
        String fooIn1Original = "class FooIn1(object):pass\n";
        String fooIn1Dep = "from fooIn1Original import FooIn1\n";
        String fooIn2 = "from fooIn1Dep import FooIn1\nclass FooIn2(FooIn1):pass\n";

        final int line = 1;
        final int col = 8;

        setUpModule(0, 0, fooIn1Original, "fooIn1Original", nature);
        setUpModule(0, 0, fooIn1Dep, "fooIn1Dep", nature);
        RefactoringRequest request;
        request = setUpModule(line, col, fooIn2, "fooIn2", nature);

        HierarchyNodeModel node = refactorer.findClassHierarchy(request, false);
        assertEquals("FooIn2", node.name);
        assertTrue(node.moduleName.startsWith("fooIn2"));

        assertEquals(node.parents.size(), 1);

        HierarchyNodeModel foo = node.parents.get(0);
        assertNotNull(foo.ast);
    }

    private RefactoringRequest setUpFooModule(final int line, final int col) {
        String str = "" +
                "import pickle\n" +
                "class Bar(pickle.Pickler):\n" +
                "    pass\n" +
                "class Foo(Bar):\n"
                +
                "    pass\n" +
                "\n" +
                "";
        return setUpFooModule(line, col, str);
    }

    private RefactoringRequest setUpFooModule(final int line, final int col, String str) {
        String modName = "foo";
        PythonNature natureToAdd = nature;
        return setUpModule(line, col, str, modName, natureToAdd);
    }

    private RefactoringRequest setUpModule(final int line, final int col, String str, String modName,
            PythonNature natureToAdd) {

        File f = new File(natureToAdd == nature2 ? baseDir2 : baseDir, modName +
                ".py");

        Document doc = new Document(str);
        PySelection ps = new PySelection(doc, line, col);

        RefactoringRequest request = new RefactoringRequest(null, ps, natureToAdd);
        request.moduleName = modName;
        final SimpleNode ast = request.getAST();

        FileUtils.writeStrToFile(str, f);

        addModuleToNature(ast, modName, natureToAdd, f);
        return request;
    }

    private HierarchyNodeModel assertIsIn(String name, String modName, List<HierarchyNodeModel> parents) {
        FastStringBuffer available = new FastStringBuffer();

        for (HierarchyNodeModel model : parents) {
            available.append(model.name).append(" - ").append(model.moduleName);
            if (model.name.equals(name)) {
                if (modName == null) {
                    return model;
                } else if (model.moduleName.equals(modName) || model.moduleName.startsWith(modName)) {
                    return model;
                }
            }
        }
        try {
            RefactorerFindReferences references = new RefactorerFindReferences();
            RefactoringRequest request = new RefactoringRequest(null, null, nature);
            request.initialName = name;

            List<Tuple<List<ModulesKey>, IPythonNature>> findPossibleReferences = references
                    .findPossibleReferences(request);

            String errorMsg = "Unable to find node with name:" + name +
                    " mod:" + modName +
                    "\nAvailable:" + available + "\n\nPythonpath: "
                    + nature.getPythonPathNature().getOnlyProjectPythonPathStr(true) + "\n" +
                    "Found possible references: " + StringUtils.join("\n", findPossibleReferences);

            fail(errorMsg);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
