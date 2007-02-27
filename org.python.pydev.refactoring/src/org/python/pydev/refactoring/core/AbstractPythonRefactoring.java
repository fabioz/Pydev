package org.python.pydev.refactoring.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.core.change.CompositeChangeProcessor;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.ui.UITexts;

public abstract class AbstractPythonRefactoring extends Refactoring {

	protected RefactoringStatus status;

	protected String name;

	protected Collection<IWizardPage> pages;

	protected RefactoringInfo req;

	public AbstractPythonRefactoring(String name, RefactoringInfo req) {
		status = new RefactoringStatus();
		pages = new ArrayList<IWizardPage>();
		this.req = req;
		this.name = name;
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
		IChangeProcessor changeProcessor = new CompositeChangeProcessor(name, getChangeProcessors());
		if (changeProcessor == null) {
			status.addFatalError(UITexts.errorUnexpected);
			return new NullChange();
		}
		return changeProcessor.createChange();
	}

	protected abstract List<IChangeProcessor> getChangeProcessors();

	@Override
	public String getName() {
		return name;
	}

	public Collection<IWizardPage> getPages() {
		return pages;
	}
}
