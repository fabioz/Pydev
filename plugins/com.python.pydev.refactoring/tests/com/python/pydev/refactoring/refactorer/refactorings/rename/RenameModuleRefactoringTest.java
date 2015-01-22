/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;

public class RenameModuleRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameModuleRefactoringTest test = new RenameModuleRefactoringTest();
            test.setUp();
            //            test.testRenameModuleInWorkspace3();
            test.tearDown();

            junit.textui.TestRunner.run(RenameModuleRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<PyRenameImportProcess> getProcessUnderTest() {
        return PyRenameImportProcess.class;
    }

    public void testRenameModuleInWorkspace() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule.mod1", "new_mod_name", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule.importer\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "    Line: 2  #mod1 comment --> #new_mod_name comment\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "    Line: 3  'mod1 string' --> 'new_mod_name string'\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> from new_mod_name import submod1\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "    Line: 0  import mod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer2\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "    Line: 2  #mod1 comment --> #new_mod_name comment\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "    Line: 3  'mod1 string' --> 'new_mod_name string'\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> from new_mod_name import submod1\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "    Line: 0  import mod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer5\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule.mod1 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule.mod1 import submod1 --> from new_mod_name import submod1\n"
                        + "\n"
                        + "reflib.renamemodule.importer6\n"
                        + "  FixedInputStringASTEntry<reflib.renamemodule.mod1 (Name L=1 C=2)>\n"
                        + "    Line: 0  'reflib.renamemodule.mod1.submod1' --> 'new_mod_name.submod1'\n"
                        + "\n"
                        + "reflib.renamemodule.importer7\n"
                        + "  ImportFromRenameAstEntry<from reflib.renamemodule import mod1importer (ImportFrom L=1 C=6)>\n"
                        + "    Initial:\n"
                        + "from reflib.renamemodule import mod1, importer\n"
                        + "    Final:\n"
                        + "from reflib.renamemodule import importer\n"
                        + "import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.mod1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                , asStr);

    }

    public void testRenameModuleInWorkspaceA() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule.mod1", "my.mod.new_name", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule.importer\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "    Line: 2  #mod1 comment --> #new_name comment\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "    Line: 3  'mod1 string' --> 'new_name string'\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> from my.mod.new_name import submod1\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "    Line: 0  import mod1 --> from my.mod import new_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer2\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "    Line: 2  #mod1 comment --> #new_name comment\n"
                        + "  FixedInputStringASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "    Line: 3  'mod1 string' --> 'new_name string'\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> from my.mod.new_name import submod1\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "    Line: 0  import mod1 --> from my.mod import new_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer5\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule.mod1 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule.mod1 import submod1 --> from my.mod.new_name import submod1\n"
                        + "\n"
                        + "reflib.renamemodule.importer6\n"
                        + "  FixedInputStringASTEntry<reflib.renamemodule.mod1 (Name L=1 C=2)>\n"
                        + "    Line: 0  'reflib.renamemodule.mod1.submod1' --> 'my.mod.new_name.submod1'\n"
                        + "\n"
                        + "reflib.renamemodule.importer7\n"
                        + "  ImportFromRenameAstEntry<from reflib.renamemodule import mod1importer (ImportFrom L=1 C=6)>\n"
                        + "    Initial:\n"
                        + "from reflib.renamemodule import mod1, importer\n"
                        + "    Final:\n"
                        + "from reflib.renamemodule import importer\n"
                        + "from my.mod import new_name\n"
                        + "\n"
                        + "reflib.renamemodule.mod1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                , asStr);

    }

    public void testRenameModuleInWorkspace2() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule.mod1.submod1", "new_mod_name", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule.importer\n"
                        + "  FixedInputStringASTEntry<submod1 (Name L=6 C=5)>\n"
                        + "    Line: 5  a = submod1 --> a = new_mod_name\n"
                        + "  ImportFromRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer2\n"
                        + "  ImportFromRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "    Line: 1  from mod1 import submod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer3\n"
                        + "  ImportFromRenameAstEntry<from importer2 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from importer2 import submod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer3a\n"
                        + "  AttributeASTEntry<submod1 (NameTok L=3 C=16)>\n"
                        + "    Line: 2  my = importer2.submod1  #must be renamed because it'll be renamed on importer2 --> my = importer2.new_mod_name  #must be renamed because it'll be renamed on importer2\n"
                        + "\n"
                        + "reflib.renamemodule.importer4\n"
                        + "  ImportFromRenameAstEntry<from importer3 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from importer3 import submod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer5\n"
                        + "  ImportFromRenameAstEntry<from reflib.renamemodule.mod1 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule.mod1 import submod1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule.importer6\n"
                        + "  FixedInputStringASTEntry<reflib.renamemodule.mod1.submod1 (Name L=1 C=2)>\n"
                        + "    Line: 0  'reflib.renamemodule.mod1.submod1' --> 'new_mod_name'\n"
                        + "\n"
                        + "reflib.renamemodule.mod1.submod1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace4() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule2.mod_ren1", "new_mod_name", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule2.mod_ren1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n" //This is the module renamed!
                        + "\n"
                        + "reflib.renamemodule2.mod_ren2\n"
                        + "  ImportFromRenameAstEntry<from reflib.renamemodule2 import mod_ren1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule2 import mod_ren1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren3\n"
                        + "  ImportFromRenameAstEntry<from  import mod_ren1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from . import mod_ren1 --> import new_mod_name\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren4\n"
                        + "  ImportFromModPartRenameAstEntry<from mod_ren1 import Mod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from .mod_ren1 import Mod1 --> from new_mod_name import Mod1\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren5\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule2.mod_ren1 import Mod1 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule2.mod_ren1 import Mod1 --> from new_mod_name import Mod1\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace5() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule3.__init__", "new_mod", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule3.__init__\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "reflib.renamemodule3.ren1\n"
                        + "  AttributeASTEntry<reflib (Name L=3 C=5)>\n"
                        + "    Line: 2  a = reflib.renamemodule3.pack1 --> a = new_mod.pack1\n"
                        + "  AttributeASTEntry<reflib (Name L=4 C=5)>\n"
                        + "    Line: 3  b = reflib.renamemodule3 --> b = new_mod\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule3.pack1 import * (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule3.pack1 import * --> from new_mod.pack1 import *\n"
                        + "  ImportFromRenameAstEntry<import reflib.renamemodule3.pack1 (Import L=2 C=8)>\n"
                        + "    Line: 1  import reflib.renamemodule3.pack1 --> import new_mod.pack1\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace6() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule3.__init__", "my.new.mod", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule3.__init__\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "reflib.renamemodule3.ren1\n"
                        + "  AttributeASTEntry<reflib (Name L=3 C=5)>\n"
                        + "    Line: 2  a = reflib.renamemodule3.pack1 --> a = my.new.mod.pack1\n"
                        + "  AttributeASTEntry<reflib (Name L=4 C=5)>\n"
                        + "    Line: 3  b = reflib.renamemodule3 --> b = my.new.mod\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule3.pack1 import * (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule3.pack1 import * --> from my.new.mod.pack1 import *\n"
                        + "  ImportFromRenameAstEntry<import reflib.renamemodule3.pack1 (Import L=2 C=8)>\n"
                        + "    Line: 1  import reflib.renamemodule3.pack1 --> import my.new.mod.pack1\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace7() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "renamemodule_root.__init__", "p2.bar", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "renamemodule_root.__init__\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "renamemodule_root.mod_in_root\n"
                        + "  FixedInputStringASTEntry<renamemodule_root (Name L=3 C=5)>\n"
                        + "    Line: 2  b = renamemodule_root.mod_in_root2 --> b = p2.bar.mod_in_root2\n"
                        + "  ImportFromRenameAstEntry<import renamemodule_root.mod_in_root2 (Import L=1 C=8)>\n"
                        + "    Line: 0  import renamemodule_root.mod_in_root2 --> import p2.bar.mod_in_root2\n"
                        + "\n"
                        + "renamemodule_root.mod_in_root3\n"
                        + "  ImportFromModPartRenameAstEntry<from renamemodule_root import mod_in_root2 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from renamemodule_root import mod_in_root2 --> from p2.bar import mod_in_root2\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace8() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "renamemodule_root.mod_in_root2", "p2", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "renamemodule_root.mod_in_root\n"
                        + "  AttributeASTEntry<renamemodule_root (Name L=3 C=5)>\n"
                        + "    Line: 2  b = renamemodule_root.mod_in_root2 --> b = p2\n"
                        + "  ImportFromRenameAstEntry<import renamemodule_root.mod_in_root2 (Import L=1 C=8)>\n"
                        + "    Line: 0  import renamemodule_root.mod_in_root2 --> import p2\n"
                        + "\n"
                        + "renamemodule_root.mod_in_root2\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n"
                        + "\n"
                        + "renamemodule_root.mod_in_root3\n"
                        + "  FixedInputStringASTEntry<mod_in_root2 (Name L=3 C=5)>\n"
                        + "    Line: 2  a = mod_in_root2 --> a = p2\n"
                        + "  ImportFromRenameAstEntry<from renamemodule_root import mod_in_root2 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from renamemodule_root import mod_in_root2 --> import p2\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace9() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule4", "p2", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule4\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "reflib.renamemodule4.mymod\n"
                        + "  FixedInputStringASTEntry<renamemodule4 (Name L=2 C=5)>\n"
                        + "    Line: 1  a = renamemodule4 --> a = p2\n"
                        + "  ImportFromRenameAstEntry<from reflib import renamemodule4 (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib import renamemodule4 --> import p2\n"
                        + "  ImportFromRenameAstEntry<import reflib.renamemodule4 (Import L=5 C=12)>\n"
                        + "    Line: 4      import reflib.renamemodule4 -->     import p2\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace10() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "renamemodule5.__init__", "p2", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "renamemodule5.__init__\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                        + "\n"
                        + "renamemodule5.app_setup\n"
                        + "  ImportFromModPartRenameAstEntry<from renamemodule5._tests.foo import RenameModule5 (ImportFrom L=3 C=6)>\n"
                        + "    Line: 2  from renamemodule5._tests.foo import RenameModule5 --> from p2._tests.foo import RenameModule5\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace11() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "renamemodule5._tests.foo", "p2", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "renamemodule5._tests.foo\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n"
                        + "\n"
                        + "renamemodule5.app_setup\n"
                        + "  ImportFromModPartRenameAstEntry<from renamemodule5._tests.foo import RenameModule5 (ImportFrom L=3 C=6)>\n"
                        + "    Line: 2  from renamemodule5._tests.foo import RenameModule5 --> from p2 import RenameModule5\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace12() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule6.scene", "p2", false);
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule6.another\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule6.scene import Scene (ImportFrom L=1 C=6)>\n"
                        + "    Line: 0  from reflib.renamemodule6.scene import Scene --> from p2 import Scene\n"
                        + "\n"
                        + "reflib.renamemodule6.scene\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n"
                        + "\n"
                        + "", asStr);
    }

    public void testRenameModuleInWorkspace13() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "testpkg._imp", "testpkg._impo", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "testpkg._imp\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n"
                        + "\n"
                , asStr);

    }

    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForModuleRename(String moduleName,
            String newName,
            boolean expectError) {
        Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesToReturn = null;
        try {
            IProjectModulesManager modulesManager = (IProjectModulesManager) natureRefactoring.getAstManager()
                    .getModulesManager();
            IModule module = modulesManager.getModuleInDirectManager(moduleName, natureRefactoring, true);
            if (module == null) {
                if (!moduleName.endsWith("__init__")) {
                    module = modulesManager.getModuleInDirectManager(moduleName + ".__init__", natureRefactoring, true);
                }
                if (module == null) {
                    throw new RuntimeException("Unable to get source module for module:" + moduleName);
                }
            }

            ModuleRenameRefactoringRequest request = new ModuleRenameRefactoringRequest(module.getFile(),
                    natureRefactoring, null);
            request.setAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false);
            request.moduleName = moduleName;
            request.fillInitialNameAndOffset();
            request.inputName = newName;

            PyRenameEntryPoint processor = new PyRenameEntryPoint(request);
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
            checkStatus(processor.checkInitialConditions(nullProgressMonitor), expectError);
            lastProcessorUsed = processor;
            checkProcessors();

            checkStatus(processor.checkFinalConditions(nullProgressMonitor, null), expectError);
            occurrencesToReturn = processor.getOccurrencesInOtherFiles();
            occurrencesToReturn.put(new Tuple<String, File>(CURRENT_MODULE_IN_REFERENCES, null),
                    processor.getOccurrences());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return occurrencesToReturn;
    }

}
