/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.DecoratableObject;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This class encapsulates all the info needed in order to do a refactoring
 * As we'd have a getter/setter without any side-effects, let's leave them all public...
 *
 * It is a Decoratable Object, so that clients can add additional information to this
 * object at runtime.
 */
public class RefactoringRequest extends DecoratableObject {

    public static final String FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE = "findReferencesOnlyOnLocalScope";

    public static final String FIND_DEFINITION_IN_ADDITIONAL_INFO = "findDefinitionInAdditionalInfo";

    /**
     * Flag used when renaming modules. If set and True, we won't do a refactoring rename and will
     * only really change the resource name.
     */
    public static final String SIMPLE_RESOURCE_RENAME = "simpleResourceRename";

    /**
     * The file associated with the editor where the refactoring is being requested
     */
    public final File file;

    /**
     * Only available on module renames.
     */
    private IFile iFileResource;

    public IFile getIFileResource() {
        return iFileResource;
    }

    /**
     * The current selection when the refactoring was requested
     */
    public PySelection ps;

    /**
     * The progress monitor to give feedback to the user (may be checked in another thread)
     * May be null
     *
     * Note that this is the monitor for the initial request, but, clients may use it in othe
     */
    private final Stack<IProgressMonitor> monitors = new Stack<IProgressMonitor>();

    /**
     * The nature used
     */
    public IPythonNature nature;

    /**
     * The python editor. May be null (especially on tests)
     */
    public final PyEdit pyEdit;

    /**
     * The module for the passed document. Has a getter that caches the result here.
     */
    private IModule module;

    /**
     * The module name (may be null)
     */
    public String moduleName;

    /**
     * The new name in a refactoring (may be null if not applicable)
     */
    public String inputName;

    /**
     * The initial representation of the selected name
     */
    public String initialName;

    /**
     * If the file is passed, we also set the document automatically
     * @param file the file correspondent to this request
     */
    public RefactoringRequest(File file, PySelection selection, IPythonNature nature) {
        this(file, selection, null, nature, null);
    }

    /**
     * If the file is passed, we also set the document automatically
     * @param file the file correspondent to this request
     * @throws MisconfigurationException
     */
    public RefactoringRequest(PyEdit pyEdit, PySelection ps) throws MisconfigurationException {
        this(pyEdit.getEditorFile(), ps, null, pyEdit.getPythonNature(), pyEdit);
    }

    /**
     * Assigns parameters to attributes (tries to resolve the module name and create a SystemPythonNature if the
     * nature is not specified)
     */
    public RefactoringRequest(File file, PySelection ps, IProgressMonitor monitor, IPythonNature nature, PyEdit pyEdit) {
        this.file = file;
        this.ps = ps;
        this.pushMonitor(monitor);

        if (nature == null) {
            Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(file);
            if (infoForFile != null) {
                this.nature = infoForFile.o1;
                this.moduleName = infoForFile.o2;
            } else {
                this.nature = null;
            }
        } else {
            this.nature = nature;
            if (file != null) {
                this.moduleName = resolveModule();
            }
        }

        this.pyEdit = pyEdit;
    }

    public File getFile() {
        return file;
    }

    /**
     * Used to make the work communication (also checks to see if it has been cancelled)
     * @param desc Some string to be shown in the progress
     */
    public synchronized void communicateWork(String desc) throws OperationCanceledException {
        IProgressMonitor monitor = getMonitor();
        if (monitor != null) {
            monitor.setTaskName(desc);
            monitor.worked(1);
            checkCancelled();
        }
    }

    /**
     * Checks if the process was cancelled (throws CancelledException in this case)
     */
    public void checkCancelled() throws OperationCanceledException {
        if (getMonitor().isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * @return the module name or null if it is not possible to determine the module name
     */
    public String resolveModule() {
        if (moduleName == null) {
            if (file != null && nature != null) {
                try {
                    moduleName = nature.resolveModule(file);
                } catch (MisconfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return moduleName;
    }

    // Some shortcuts to the PySelection
    /**
     * @return the initial column selected (starting at 0)
     */
    public int getBeginCol() {
        return ps.getAbsoluteCursorOffset() - ps.getStartLine().getOffset();
    }

    /**
     * @return the final column selected (starting at 0)
     */
    public int getEndCol() {
        return ps.getAbsoluteCursorOffset() + ps.getSelLength() - ps.getEndLine().getOffset();
    }

    /**
     * @return the last line selected (starting at 1)
     */
    public int getEndLine() {
        return ps.getEndLineIndex() + 1;
    }

    /**
     * @return the initial line selected (starting at 1)
     */
    public int getBeginLine() {
        return ps.getStartLineIndex() + 1;
    }

    /**
     * @return the module for the document (may return the ast from the pyedit if it is available).
     */
    public IModule getModule() {
        if (module == null) {
            if (pyEdit != null) {
                SimpleNode ast = pyEdit.getAST();
                if (ast != null) {
                    IDocument doc = ps.getDoc();
                    long astModificationTimeStamp = pyEdit.getAstModificationTimeStamp();
                    if (astModificationTimeStamp != -1
                            && astModificationTimeStamp == (((IDocumentExtension4) doc).getModificationStamp())) {
                        //Matched time stamp -- so, we can use the ast without fear of being unsynched.
                        module = AbstractModule.createModule(ast, file, resolveModule());
                    } else {
                        //Did not match time stamp!! We'll reparse the document later on to get a synched version.
                    }
                }
            }

            if (module == null) {
                try {
                    module = AbstractModule.createModuleFromDoc(resolveModule(), file, ps.getDoc(), nature, false);
                } catch (MisconfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return module;
    }

    /**
     * @return the ast for the current module
     */
    public SimpleNode getAST() {
        IModule mod = getModule();
        if (mod instanceof SourceModule) {
            return ((SourceModule) mod).getAst();
        }
        return null;
    }

    /**
     * Fills the initial name and initial offset from the PySelection
     */
    public void fillInitialNameAndOffset() {
        try {
            Tuple<String, Integer> currToken = ps.getCurrToken();
            initialName = currToken.o1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the current document where this refactoring request was asked
     */
    public IDocument getDoc() {
        return ps.getDoc();
    }

    /**
     * Sets the monitor to be used (because it may change depending on the current step we're in)
     * @param monitor the monitor to be used
     */
    public void pushMonitor(IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        this.monitors.push(monitor);
    }

    /**
     * Removes and returns the current top-most progress monitor
     */
    public IProgressMonitor popMonitor() {
        return this.monitors.pop();
    }

    /**
     * Clients using the RefactoringRequest are expected to receive it as a parameter, then:
     *
     * getMonitor().beginTask("my task", total)
     *
     *
     * try{
     *     //Calling another function
     *     req.pushMonitor(new SubProgressMonitor(monitor, 10));
     *     callIt(req);
     * finally{
     *     req.popMonitor().done();
     * }
     *
     * try{
     *     //Calling another function
     *     req.pushMonitor(new SubProgressMonitor(monitor, 90));
     *     callIt(req);
     * finally{
     *     req.popMonitor().done();
     * }
     *
     *
     * getMonitor().done();
     *
     *
     *
     *
     *
     * @return the current progress monitor
     */
    public IProgressMonitor getMonitor() {
        return this.monitors.peek();
    }

    public IFile getIFile() {
        if (this.pyEdit == null) {
            return null;
        }
        return this.pyEdit.getIFile();
    }

    public boolean isModuleRenameRefactoringRequest() {
        return false;
    }

    public IPythonNature getTargetNature() {
        return this.nature;
    }

    private Map<String, List<Tuple<List<ModulesKey>, IPythonNature>>> tokenToLastReferences = new HashMap<>();

    public List<Tuple<List<ModulesKey>, IPythonNature>> getPossibleReferences(String initialName) {
        return tokenToLastReferences.get(initialName);
    }

    public void setPossibleReferences(String initialName, List<Tuple<List<ModulesKey>, IPythonNature>> ret) {
        tokenToLastReferences.put(initialName, ret);
    }

    public void setUpdateReferences(boolean updateReferences) {
        setAdditionalInfo(FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, !updateReferences);
    }

    public void setSimpleResourceRename(boolean simpleResourceRename) {
        setAdditionalInfo(SIMPLE_RESOURCE_RENAME, simpleResourceRename);
    }

    public boolean getSimpleResourceRename() {
        return (boolean) getAdditionalInfo(SIMPLE_RESOURCE_RENAME, false);
    }

    public void setFileResource(IFile file2) {
        this.iFileResource = file2;

    }

}