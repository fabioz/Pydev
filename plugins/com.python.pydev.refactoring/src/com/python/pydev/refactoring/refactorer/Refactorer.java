/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.IPyRefactoring2;
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
    public String rename(RefactoringRequest request) {
        try {
            RenameRefactoring renameRefactoring = new RenameRefactoring(new PyRenameEntryPoint(request));
            request.fillInitialNameAndOffset();
            final PyRenameRefactoringWizard wizard = new PyRenameRefactoringWizard(renameRefactoring, "Rename",
                    "inputPageDescription", request, request.initialName);
            try {
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                op.run(PyAction.getShell(), "Rename Refactor Action");
            } catch (InterruptedException e) {
                // do nothing. User action got cancelled
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    public ItemPointer[] findDefinition(RefactoringRequest request) throws TooManyMatchesException {
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
        PyRenameEntryPoint processor = new PyRenameEntryPoint(req);
        //to see if a new request was not created in the meantime (in which case this one will be cancelled)
        req.checkCancelled();

        IProgressMonitor monitor = req.getMonitor();

        Map<Tuple<String, File>, HashSet<ASTEntry>> occurrencesInOtherFiles;

        try {
            monitor.beginTask("Find all occurrences", 100);
            monitor.setTaskName("Find all occurrences");
            RefactoringStatus status;
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 10));
                status = processor.checkInitialConditions(req.getMonitor());
                if (status.getSeverity() == RefactoringStatus.FATAL) {
                    return null;
                }
            } finally {
                req.popMonitor().done();
            }
            req.checkCancelled();
            try {
                req.pushMonitor(new SubProgressMonitor(monitor, 85));
                status = processor.checkFinalConditions(req.getMonitor(), null, false);
                if (status.getSeverity() == RefactoringStatus.FATAL) {
                    return null;
                }
            } finally {
                req.popMonitor().done();
            }
            req.checkCancelled();
            occurrencesInOtherFiles = processor.getOccurrencesInOtherFiles();
            HashSet<ASTEntry> occurrences = processor.getOccurrences();
            occurrencesInOtherFiles.put(new Tuple<String, File>(req.moduleName, req.pyEdit.getEditorFile()),
                    occurrences);

            req.getMonitor().worked(5);
        } finally {
            monitor.done();
        }
        return occurrencesInOtherFiles;
    }

}
