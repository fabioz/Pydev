/*
 * Created on Apr 8, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DummyChange extends Change {

    public DummyChange(IProgressMonitor pm) {
    }

    @Override
    public String getName() {
        return "Dummy Change";
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
        return null; //return null because there is no undo
    }

    @Override
    public Object getModifiedElement() {
        return null; //is not related to an element
    }

}
