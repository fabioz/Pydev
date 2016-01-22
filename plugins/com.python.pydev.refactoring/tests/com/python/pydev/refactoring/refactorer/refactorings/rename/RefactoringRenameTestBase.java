/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectStub;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.PyFileListing;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;
import com.python.pydev.refactoring.refactorer.refactorings.renamelocal.RefactoringLocalTestBase;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.TextEditCreation;

/**
 * A class used for the refactorings that need the rename project (in pysrcrefactoring)
 *
 * @author Fabio
 */
public abstract class RefactoringRenameTestBase extends RefactoringLocalTestBase {
    /**
     * We want to keep it initialized among runs from the same class.
     * Check the restorePythonPath function.
     */
    public static PythonNature natureRefactoring;

    /**
     * This is the last rename processor used (so, we may query it about other things)
     */
    protected PyRenameEntryPoint lastProcessorUsed;

    /**
     * Backwards-compatibility interface
     */
    protected boolean restoreProjectPythonPathRefactoring(boolean force, String path) {
        return restoreProjectPythonPathRefactoring(force, path, "testProjectStubRefactoring");
    }

    /**
     * The list of python files contained in the refactoring pysrc project
     */
    protected static Collection<PyFileInfo> filesInRefactoringProject;

    public static final String CURRENT_MODULE_IN_REFERENCES = "__current_module__";

    protected static boolean DEBUG_REFERENCES = false;

    /**
     * In the setUp, it initializes the files in the refactoring project
     * @see com.python.pydev.refactoring.refactorer.refactorings.renamelocal.RefactoringLocalTestBase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (filesInRefactoringProject == null) {
            filesInRefactoringProject = PyFileListing.getPyFilesBelow(
                    new File(TestDependent.TEST_COM_REFACTORING_PYSRC_LOC), new NullProgressMonitor(), true, false)
                    .getFoundPyFileInfos();

            ArrayList<Tuple<List<ModulesKey>, IPythonNature>> iFiles = new ArrayList<Tuple<List<ModulesKey>, IPythonNature>>();
            List<ModulesKey> modules = new ArrayList<ModulesKey>();

            iFiles.add(new Tuple<List<ModulesKey>, IPythonNature>(modules, natureRefactoring));
            FastStringBuffer tempBuf = new FastStringBuffer();
            for (PyFileInfo info : filesInRefactoringProject) {
                File f = info.getFile();
                String modName = info.getModuleName(tempBuf);
                ModulesKey modulesKey = new ModulesKey(modName, f);
                modules.add(modulesKey);

                SourceModule mod = (SourceModule) AbstractModule.createModule(modName, f, natureRefactoring, true);

                //also create the additional info so that it can be used for finds
                AbstractAdditionalTokensInfo additionalInfo = AdditionalProjectInterpreterInfo
                        .getAdditionalInfoForProject(natureRefactoring);
                additionalInfo.addAstInfo(mod.getAst(), modulesKey, false);
            }

            //            RefactorerFindReferences.FORCED_RETURN = iFiles;
        }
        setUpConfigWorkspaceFiles();
    }

    private org.python.pydev.shared_core.resource_stubs.ProjectStub projectStub;

    public void setUpConfigWorkspaceFiles() throws Exception {
        projectStub = new org.python.pydev.shared_core.resource_stubs.ProjectStub(
                new File(TestDependent.TEST_COM_REFACTORING_PYSRC_LOC),
                natureRefactoring);
        TextEditCreation.createWorkspaceFile = new ICallback<IFile, File>() {

            @Override
            public IFile call(File file) {
                return new FileStub(projectStub, file) {
                    @Override
                    public IPath getFullPath() {
                        return Path.fromOSString(this.file.getAbsolutePath());
                    }

                };
            }
        };
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Checks if the refactor processes are all of the type we're testing here.
     */
    protected void checkProcessors() {
        if (lastProcessorUsed != null) {
            List<IRefactorRenameProcess> processes = lastProcessorUsed.getAllProcesses();
            assertEquals(1, processes.size());

            Class processUnderTest = getProcessUnderTest();
            if (processUnderTest != null) {
                for (IRefactorRenameProcess p : processes) {
                    assertTrue(StringUtils.format("Expected %s. Received:%s",
                            processUnderTest, p.getClass()),
                            processUnderTest.isInstance(p)); //we should only activate the rename class process in this test case
                }
            }
        }
    }

    /**
     * @return the process class that we're testing.
     */
    protected abstract Class getProcessUnderTest();

    /**
     * A method that creates a project that references no other project
     *
     * @param force whether the creation of the new nature should be forced
     * @param path the pythonpath for the new nature
     * @param name the name for the project
     * @return true if the creation was needed and false if it wasn't
     */
    protected boolean restoreProjectPythonPathRefactoring(boolean force, String path, String name) {
        PythonNature n = checkNewNature(name, force);
        if (n != null) {
            natureRefactoring = n;

            ProjectStub projectFromNatureRefactoring = new ProjectStub(name, path, new IProject[0], new IProject[0]);
            setAstManager(path, projectFromNatureRefactoring, natureRefactoring);
            return true;
        }
        return false;
    }

    /**
     * Overriden so that the pythonpath is only restored for the system and the refactoring nature
     *
     * @param force whether this should be forced, even if it was previously created for this class
     */
    @Override
    public void restorePythonPath(boolean force) {
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring system pythonpath");
        }
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB);
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring project pythonpath for refactoring nature");
        }
        restoreProjectPythonPathRefactoring(force, TestDependent.TEST_COM_REFACTORING_PYSRC_LOC);
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Checking size (for projrefactoring)");
        }

        checkSize();
    }

    /**
     * checks if the size of the system modules manager and the project moule manager are coherent
     * (we must have more modules in the system than in the project)
     */
    @Override
    protected void checkSize() {
        try {
            IInterpreterManager iMan = getInterpreterManager();
            InterpreterInfo info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(false);
            assertTrue(info.getModulesManager().getSize(true) > 0);

            int size = ((ASTManager) natureRefactoring.getAstManager()).getSize();
            assertTrue("Interpreter size:" + info.getModulesManager().getSize(true)
                    + " should be smaller than project size:" + size + " "
                    + "(because it contains system+project info)", info.getModulesManager().getSize(true) < size);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Gets the references for the rename without expecting any error.
     * @param line: starts at 0
     * @param col: starts at 0
     */
    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForRenameSimple(String moduleName, int line,
            int col) {
        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForRename = getReferencesForRenameSimple(moduleName,
                line, col, false);
        if (DEBUG_REFERENCES) {
            for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : referencesForRename.entrySet()) {
                System.out.println(entry.getKey());
                for (ASTEntry e : entry.getValue()) {
                    System.out.println(e);
                }
            }
        }
        return referencesForRename;
    }

    /**
     * Same as {@link #getReferencesForRename(String, int, int, boolean)} but returning
     * the key for the map as a string with the module name.
     */
    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForRenameSimple(String moduleName, int line,
            int col,
            boolean expectError) {
        Map<String, HashSet<ASTEntry>> occurrencesToReturn = new HashMap<String, HashSet<ASTEntry>>();

        Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForRename = getReferencesForRename(moduleName, line, col,
                expectError);
        for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> entry : referencesForRename.entrySet()) {
            if (occurrencesToReturn.get(entry.getKey()) != null) {
                throw new RuntimeException("Error. Module: " + entry.getKey() + " already exists.");
            }
            occurrencesToReturn.put(entry.getKey().o1, entry.getValue());
        }
        return referencesForRename;
    }

    /**
     * Goes through all the workspace (in this case the refactoring project) and gathers the references
     * for the current selection.
     *
     * @param moduleName the name of the module we're currently in
     * @param line the line we're in
     * @param col the col we're in
     * @param expectError whether we are expecting some error or not
     * @return a map with the name of the module and the file representing it pointing to the
     * references found in that module.
     */
    protected Map<Tuple<String, File>, HashSet<ASTEntry>> getReferencesForRename(String moduleName, int line, int col,
            boolean expectError) {
        Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesToReturn = null;
        try {
            IProjectModulesManager modulesManager = (IProjectModulesManager) natureRefactoring.getAstManager()
                    .getModulesManager();
            IModule module = modulesManager.getModuleInDirectManager(moduleName, natureRefactoring, true);
            if (module == null) {
                throw new RuntimeException("Unable to get source module for module:" + moduleName);
            }
            String strDoc = FileUtils.getFileContents(module.getFile());

            Document doc = new Document(strDoc);
            PySelection ps = new PySelection(doc, line, col);

            RefactoringRequest request = new RefactoringRequest(null, ps, natureRefactoring);
            request.setAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false);
            request.moduleName = moduleName;
            request.inputName = "new_name";
            request.fillInitialNameAndOffset();

            PyRenameEntryPoint processor = new PyRenameEntryPoint(request);
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
            checkStatus(processor.checkInitialConditions(nullProgressMonitor), expectError);
            lastProcessorUsed = processor;
            checkProcessors();

            checkStatus(processor.checkFinalConditions(nullProgressMonitor, null), expectError);
            occurrencesToReturn = processor.getOccurrencesInOtherFiles();
            occurrencesToReturn.put(new Tuple<String, File>(moduleName, module.getFile()),
                    processor.getOccurrences());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return occurrencesToReturn;
    }

    /**
     * Used to see if some line/col is available in a list of entries.
     */
    protected void assertContains(int line, int col, HashSet<ASTEntry> names) {
        for (ASTEntry name : names) {
            SimpleNode node = name.node;
            if (node instanceof ClassDef) {
                node = ((ClassDef) node).name;
            }
            if (node instanceof FunctionDef) {
                node = ((FunctionDef) node).name;
            }
            if (node.beginLine == line && node.beginColumn == col) {
                return;
            }
        }
        fail(StringUtils.format("Unable to find line:%s col:%s in %s", line, col,
                names));

    }

    @SuppressWarnings("unchecked")
    protected String asStr(Map<Tuple<String, File>, HashSet<ASTEntry>> referencesForModuleRename) throws Exception {
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
