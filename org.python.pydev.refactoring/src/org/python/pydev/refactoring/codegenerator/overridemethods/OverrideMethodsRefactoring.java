package org.python.pydev.refactoring.codegenerator.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.ui.UITexts;
import org.python.pydev.refactoring.ui.model.overridemethods.ClassMethodsTreeProvider;
import org.python.pydev.refactoring.ui.pages.OverrideMethodsPage;

public class OverrideMethodsRefactoring extends AbstractPythonRefactoring {

	private OverrideMethodsRequestProcessor requestProcessor;

	private OverrideMethodsChangeProcessor changeProcessor;

	public OverrideMethodsRefactoring(String name, RefactoringInfo req) {
		super(name, req);
		try {
			initWizard(name);
		} catch (Throwable e) {
			status.addInfo(UITexts.infoFixCode);
		}
	}

	private void initWizard(String name) {
		ClassMethodsTreeProvider provider = new ClassMethodsTreeProvider(req.getScopeClassAndBases());
		this.requestProcessor = new OverrideMethodsRequestProcessor(req.getScopeClass());
		this.changeProcessor = new OverrideMethodsChangeProcessor(this.name, this.req, this.requestProcessor);
		this.pages.add(new OverrideMethodsPage(name, provider, requestProcessor));
	}

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		ClassDefAdapter rootClass = this.req.getScopeClass();

		if (rootClass == null) {
			status.addFatalError(UITexts.overrideMethodsUnavailable);
		}

		return status;
	}

}
