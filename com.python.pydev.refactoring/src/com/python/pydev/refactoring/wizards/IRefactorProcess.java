/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public interface IRefactorProcess {

    public abstract void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request);

    public abstract void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange);

    /**
     * @return a list of entries with the ocurrences that will be affected in the refactoring or null if it
     * does not have this kind of association.
     */
    public List<ASTEntry> getOcurrences();
}