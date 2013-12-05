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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.navigator.ProjectStub;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;
import com.python.pydev.refactoring.wizards.rename.TextEditCreation;

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
    public void setUp() throws Exception {
        super.setUp();
        TextEditCreation.projectStub = new ProjectStub(new File(TestDependent.TEST_COM_REFACTORING_PYSRC_LOC),
                natureRefactoring);
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
                        + "", asStr);
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
                    natureRefactoring);
            request.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false);
            request.moduleName = moduleName;
            request.fillInitialNameAndOffset();
            request.inputName = newName;

            PyRenameEntryPoint processor = new PyRenameEntryPoint(request);
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
            checkStatus(processor.checkInitialConditions(nullProgressMonitor), expectError);
            lastProcessorUsed = processor;
            checkProcessors();

            checkStatus(processor.checkFinalConditions(nullProgressMonitor, null, true), expectError);
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

    @SuppressWarnings("unchecked")
    private String asStr(Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename) throws Exception {
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
                File f = entry.getKey().o2;
                String fileContents = FileUtils.getFileContents(f);

                Document initialDoc = new Document(fileContents);

                buf.append(entry.getKey().o1).append("\n");
                for (ASTEntry e : lst2) {
                    buf.append("  ");
                    buf.append(e.toString()).append("\n");

                    List<TextEdit> edits = (List<TextEdit>) e.getAdditionalInfo(
                            AstEntryScopeAnalysisConstants.AST_ENTRY_REPLACE_EDIT, null);
                    if (edits == null) {
                        if (!(e instanceof ASTEntryWithSourceModule)) {
                            throw new AssertionError("Only ASTEntryWithSourceModule can have null edits. Found: " + e);
                        }
                    } else {
                        Document changedDoc = new Document(fileContents);
                        for (TextEdit textEdit : edits) {
                            textEdit.apply(changedDoc);
                        }
                        List<String> changedLines = getChangedLines(initialDoc, changedDoc);
                        for (String i : changedLines) {
                            buf.append("    ");
                            buf.append(StringUtils.rightTrim(i)).append("\n");
                        }
                    }
                }
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    private List<String> getChangedLines(Document initialDoc, Document changedDoc) {
        int numberOfLines = initialDoc.getNumberOfLines();
        int numberOfLines2 = changedDoc.getNumberOfLines();
        List<String> ret = new ArrayList<>();

        if (numberOfLines != numberOfLines2) {
            ret.add("Initial:\n" + StringUtils.replaceNewLines(initialDoc.get(), "\n"));
            ret.add("Final:\n" + StringUtils.replaceNewLines(changedDoc.get(), "\n"));

        } else {
            for (int i = 0; i < numberOfLines; i++) {
                String l1 = PySelection.getLine(initialDoc, i);
                String l2 = PySelection.getLine(changedDoc, i);
                if (!l1.equals(l2)) {
                    ret.add(StringUtils.format("Line: %s  %s --> %s", i, l1, l2));
                }
            }
        }
        return ret;
    }

}
