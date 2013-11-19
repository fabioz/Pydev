/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;

public class RenameModuleRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameModuleRefactoringTest test = new RenameModuleRefactoringTest();
            test.setUp();
            test.testRenameModuleInWorkspace3();
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
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 0, 8);
        assertEquals(3, references.size());

        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());

        assertTrue(references.containsKey("reflib.renamemodule.mod1.__init__")); //module renamed 
        assertEquals(1, references.get("reflib.renamemodule.mod1.__init__").size());

        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertEquals(4, references.get("reflib.renamemodule.importer2").size());
    }

    public void testRenameModuleInWorkspace2() throws Exception {
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 1, 18);
        checkSubMod1References(references);
        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertTrue(references.containsKey("reflib.renamemodule.importer3"));
        assertTrue(references.containsKey("reflib.renamemodule.importer4"));
        assertTrue(references.containsKey("reflib.renamemodule.importer5"));
        assertTrue(references.containsKey("reflib.renamemodule.mod1.submod1")); //module renamed 
    }

    public void testRenameModuleInWorkspace3() throws Exception {
        //from reflib.renamemodule.mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer5", 0, 40);
        checkSubMod1References(references);
        assertTrue(references.containsKey("reflib.renamemodule.importer"));
        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertTrue(references.containsKey("reflib.renamemodule.importer3"));
        assertTrue(references.containsKey("reflib.renamemodule.importer4"));
        assertTrue(references.containsKey("reflib.renamemodule.mod1.submod1")); //module renamed 
    }

    public void testRenameModuleInWorkspace4() throws Exception {

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule2.mod_ren1", false);
        assertContains("reflib.renamemodule2.mod_ren1", referencesForModuleRename);
        assertContains("reflib.renamemodule2.mod_ren2", referencesForModuleRename);
        assertContains("reflib.renamemodule2.mod_ren3", referencesForModuleRename);
        assertContains("reflib.renamemodule2.mod_ren4", referencesForModuleRename);
        assertContains("reflib.renamemodule2.mod_ren5", referencesForModuleRename);
    }

    private void assertContains(String string, Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename) {
        Set<Tuple<String, File>> keySet = referencesForModuleRename.keySet();
        for (Tuple<String, File> tuple : keySet) {
            if (tuple.o1.equals(string)) {
                return;
            }
        }
        fail("Could not find module: " + string);
    }

    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForModuleRename(String moduleName,
            boolean expectError) {
        Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesToReturn = null;
        try {
            IProjectModulesManager modulesManager = (IProjectModulesManager) natureRefactoring.getAstManager()
                    .getModulesManager();
            IModule module = modulesManager.getModuleInDirectManager(moduleName, natureRefactoring, true);
            if (module == null) {
                throw new RuntimeException("Unable to get source module for module:" + moduleName);
            }

            ModuleRenameRefactoringRequest request = new ModuleRenameRefactoringRequest(module.getFile(),
                    natureRefactoring);
            request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false);
            request.moduleName = moduleName;
            request.fillInitialNameAndOffset();

            PyRenameEntryPoint processor = new PyRenameEntryPoint(request);
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
            checkStatus(processor.checkInitialConditions(nullProgressMonitor), expectError);
            lastProcessorUsed = processor;
            checkProcessors();

            checkStatus(processor.checkFinalConditions(nullProgressMonitor, null, false), expectError);
            occurrencesToReturn = processor.getOccurrencesInOtherFiles();
            occurrencesToReturn.put(new Tuple<String, File>(CURRENT_MODULE_IN_REFERENCES, null),
                    processor.getOccurrences());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return occurrencesToReturn;
    }

    private void checkSubMod1References(Map<String, HashSet<ASTEntry>> references) {
        assertEquals(6, references.size());

        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(1, references.get(CURRENT_MODULE_IN_REFERENCES).size());

        for (Collection<ASTEntry> values : references.values()) {
            assertEquals(1, values.size());
        }
    }

}
