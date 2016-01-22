/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoringRequest;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.editor.refactoring.MultiModuleMoveRefactoringRequest;
import org.python.pydev.editor.refactoring.PyRefactoringRequest;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;
import com.python.pydev.refactoring.wizards.rename.PyReferenceSearcher;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.PyRenameRefactoringWizard;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * This is the entry point for any refactoring that we implement.
 * 
 * @author Fabio
 */
public class Refactorer extends AbstractPyRefactoring implements IPyRefactoring2 {

    public String getName() {
        return "PyDev Extensions Refactorer";
    }

    /**
     * Renames something... 
     * 
     * Basically passes things to the rename processor (it will choose the kind of rename that will happen). 
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#rename(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public String rename(IPyRefactoringRequest request) {
        try {
            List<RefactoringRequest> actualRequests = request.getRequests();
            if (actualRequests.size() == 1) {
                RefactoringRequest req = actualRequests.get(0);

                //Note: if it's already a ModuleRenameRefactoringRequest, no need to change anything.
                if (!(req.isModuleRenameRefactoringRequest())) {

                    //Note: if we're renaming an import, we must change to the appropriate req
                    IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                    ItemPointer[] pointers = pyRefactoring.findDefinition(req);
                    for (ItemPointer pointer : pointers) {
                        Definition definition = pointer.definition;
                        if (RefactorProcessFactory.isModuleRename(definition)) {
                            try {
                                request = new PyRefactoringRequest(new ModuleRenameRefactoringRequest(
                                        definition.module.getFile(), req.nature, null));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }

            PyRenameEntryPoint entryPoint = new PyRenameEntryPoint(request);
            RenameRefactoring renameRefactoring = new RenameRefactoring(entryPoint);
            request.fillInitialNameAndOffset();

            String title = "Rename";
            if (request instanceof MultiModuleMoveRefactoringRequest) {
                MultiModuleMoveRefactoringRequest multiModuleMoveRefactoringRequest = (MultiModuleMoveRefactoringRequest) request;
                title = "Move To package (project: "
                        + multiModuleMoveRefactoringRequest.getTarget().getProject().getName()
                        + ")";
            }
            final PyRenameRefactoringWizard wizard = new PyRenameRefactoringWizard(renameRefactoring, title,
                    "inputPageDescription", request);
            try {
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                op.run(EditorUtils.getShell(), "Rename Refactor Action");
            } catch (InterruptedException e) {
                // do nothing. User action got cancelled
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    public ItemPointer[] findDefinition(RefactoringRequest request)
            throws TooManyMatchesException, BadLocationException {
        return new RefactorerFindDefinition().findDefinition(request);
    }

    // --------------------------------------------------------- IPyRefactoring2
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs) {
        return new RefactorerFinds(this).areAllInSameClassHierarchy(defs);
    }

    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request, boolean findOnlyParents) {
        return new RefactorerFinds(this).findClassHierarchy(request, findOnlyParents);
    }

    public Map<Tuple<String, File>, HashSet<ASTEntry>> findAllOccurrences(RefactoringRequest req)
            throws OperationCanceledException, CoreException {
        PyReferenceSearcher pyReferenceSearcher = new PyReferenceSearcher(req);
        //to see if a new request was not created in the meantime (in which case this one will be cancelled)
        req.checkCancelled();

        IProgressMonitor monitor = req.getMonitor();

        Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesInOtherFiles;

        try {
            monitor.beginTask("Find all occurrences", 100);
            monitor.setTaskName("Find all occurrences");
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 10));
                pyReferenceSearcher.prepareSearch(req);
            } catch (PyReferenceSearcher.SearchException | BadLocationException e) {
                return null;
            } finally {
                req.popMonitor().done();
            }
            req.checkCancelled();
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 85));
                pyReferenceSearcher.search(req);
            } catch (PyReferenceSearcher.SearchException e) {
                return null;
            } finally {
                req.popMonitor().done();
            }
            req.checkCancelled();
            occurrencesInOtherFiles = pyReferenceSearcher.getWorkspaceReferences(req);
            HashSet<ASTEntry> occurrences = pyReferenceSearcher.getLocalReferences(req);
            occurrencesInOtherFiles.put(new Tuple<String, File>(req.moduleName, req.pyEdit.getEditorFile()),
                    occurrences);

            req.getMonitor().worked(5);
        } finally {
            monitor.done();
        }
        return occurrencesInOtherFiles;
    }

}
