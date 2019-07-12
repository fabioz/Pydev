/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.refactoring.refactorer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.ast.item_pointer.ItemPointer;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.ast.refactoring.HierarchyNodeModel;
import org.python.pydev.ast.refactoring.IPyRefactoring2;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.ast.refactoring.TooManyMatchesException;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.refactoring.wizards.rename.PyReferenceSearcher;

/**
 * This is the entry point for any refactoring that we implement.
 *
 * @author Fabio
 */
public class Refactorer extends AbstractPyRefactoring implements IPyRefactoring2 {

    @Override
    public String getName() {
        return "PyDev Extensions Refactorer";
    }

    @Override
    public ItemPointer[] findDefinition(RefactoringRequest request)
            throws TooManyMatchesException, BadLocationException {
        return new RefactorerFindDefinition().findDefinition(request);
    }

    // --------------------------------------------------------- IPyRefactoring2
    @Override
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs) {
        return new RefactorerFinds(this).areAllInSameClassHierarchy(defs);
    }

    @Override
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request, boolean findOnlyParents) {
        return new RefactorerFinds(this).findClassHierarchy(request, findOnlyParents);
    }

    @Override
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
            occurrencesInOtherFiles.put(
                    new Tuple<String, File>(req.moduleName, req.pyEdit != null ? req.pyEdit.getEditorFile() : req.file),
                    occurrences);

            req.getMonitor().worked(5);
        } finally {
            monitor.done();
        }
        return occurrencesInOtherFiles;
    }

}
