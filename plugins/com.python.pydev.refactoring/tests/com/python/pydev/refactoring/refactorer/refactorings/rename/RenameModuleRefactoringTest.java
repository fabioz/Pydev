/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.FastStringBuffer;
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

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule.mod1", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule.importer\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "  ASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "  ASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule.importer2\n"
                        + "  ImportFromRenameAstEntry<import mod1 (Import L=1 C=8)>\n"
                        + "  ASTEntry<mod1 (Name L=3 C=2)>\n"
                        + "  ASTEntry<mod1 (Name L=4 C=2)>\n"
                        + "  ImportFromModPartRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule.importer5\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule.mod1 import submod1 (ImportFrom L=1 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule.importer6\n"
                        + "  ASTEntry<reflib.renamemodule.mod1 (Name L=1 C=2)>\n" //Only in string, but has to match!
                        + "\n"
                        + "reflib.renamemodule.mod1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n" //this is the module renamed
                        + "\n", asStr);

    }

    public void testRenameModuleInWorkspace2A() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename = getReferencesForModuleRename(
                "reflib.renamemodule.mod1.submod1", false);

        String asStr = asStr(referencesForModuleRename);
        assertEquals(""
                + "reflib.renamemodule.importer\n"
                + "  ImportFromRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                + "\n"
                + "reflib.renamemodule.importer2\n"
                + "  ImportFromRenameAstEntry<from mod1 import submod1 (ImportFrom L=2 C=6)>\n"
                + "\n"
                + "reflib.renamemodule.importer3\n"
                + "  ImportFromRenameAstEntry<from importer2 import submod1 (ImportFrom L=1 C=6)>\n"
                + "\n"
                + "reflib.renamemodule.importer3a\n"
                + "  ASTEntry<submod1 (NameTok L=3 C=16)>\n"
                + "\n"
                + "reflib.renamemodule.importer4\n"
                + "  ImportFromRenameAstEntry<from importer3 import submod1 (ImportFrom L=1 C=6)>\n"
                + "\n"
                + "reflib.renamemodule.importer5\n"
                + "  ImportFromRenameAstEntry<from reflib.renamemodule.mod1 import submod1 (ImportFrom L=1 C=6)>\n"
                + "\n"
                + "reflib.renamemodule.importer6\n"
                + "  ASTEntry<reflib.renamemodule.mod1.submod1 (Name L=1 C=2)>\n"
                + "\n"
                + "reflib.renamemodule.mod1.submod1\n"
                + "  ASTEntryWithSourceModule<Module (Module L=0 C=0)>\n"
                + "\n"
                + "", asStr);
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
        String asStr = asStr(referencesForModuleRename);
        assertEquals(
                ""
                        + "reflib.renamemodule2.mod_ren1\n"
                        + "  ASTEntryWithSourceModule<Module (Module L=1 C=1)>\n" //This is the module renamed!
                        + "\n"
                        + "reflib.renamemodule2.mod_ren2\n"
                        + "  ImportFromRenameAstEntry<from reflib.renamemodule2 import mod_ren1 (ImportFrom L=1 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren3\n"
                        + "  ImportFromRenameAstEntry<from  import mod_ren1 (ImportFrom L=1 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren4\n"
                        + "  ImportFromModPartRenameAstEntry<from mod_ren1 import Mod1 (ImportFrom L=1 C=6)>\n"
                        + "\n"
                        + "reflib.renamemodule2.mod_ren5\n"
                        + "  ImportFromModPartRenameAstEntry<from reflib.renamemodule2.mod_ren1 import Mod1 (ImportFrom L=1 C=6)>\n"
                        + "\n"
                        + "", asStr);
    }

    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForModuleRename(String moduleName,
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

    private String asStr(Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename) {
        Set<Entry<Tuple<String, File>, HashSet<ASTEntry>>> entrySet = referencesForModuleRename.entrySet();
        FastStringBuffer buf = new FastStringBuffer();
        ArrayList<Entry<Tuple<String, File>, HashSet<ASTEntry>>> lst = new ArrayList<>(entrySet);
        Comparator<Entry<Tuple<String, File>, HashSet<ASTEntry>>> c = new Comparator<Entry<Tuple<String, File>, HashSet<ASTEntry>>>() {

            @Override
            public int compare(Entry<Tuple<String, File>, HashSet<ASTEntry>> o1,
                    Entry<Tuple<String, File>, HashSet<ASTEntry>> o2) {
                return o1.getKey().o1.compareTo(o2.getKey().o1);
            }
        };
        Collections.sort(lst, c);
        for (Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : lst) {
            HashSet<ASTEntry> value = entry.getValue();
            if (value.size() > 0) {
                ArrayList<ASTEntry> lst2 = new ArrayList<>(value);
                Comparator<ASTEntry> c2 = new Comparator<ASTEntry>() {

                    @Override
                    public int compare(ASTEntry o1, ASTEntry o2) {
                        return o1.toString().compareTo(o2.toString());
                    }
                };

                Collections.sort(lst2, c2);
                buf.append(entry.getKey().o1).append("\n");
                for (ASTEntry e : value) {
                    buf.append("  ");
                    buf.append(e.toString()).append("\n");
                }
                buf.append("\n");
            }
        }
        return buf.toString();
    }

}
