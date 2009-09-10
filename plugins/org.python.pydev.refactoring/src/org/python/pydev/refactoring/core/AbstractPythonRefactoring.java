/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.core.change.CompositeChangeProcessor;
import org.python.pydev.refactoring.core.change.IChangeProcessor;

public abstract class AbstractPythonRefactoring extends Refactoring {
    protected RefactoringStatus status;
    protected Collection<IWizardPage> pages;
    protected RefactoringInfo info;

    public AbstractPythonRefactoring(RefactoringInfo info) {
        status = new RefactoringStatus();
        pages = new ArrayList<IWizardPage>();
        this.info = info;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return status;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        IChangeProcessor changeProcessor = new CompositeChangeProcessor(getName(), getChangeProcessors());

        return changeProcessor.createChange();
    }

    protected abstract List<IChangeProcessor> getChangeProcessors();

    public Collection<IWizardPage> getPages() {
        return pages;
    }
}
