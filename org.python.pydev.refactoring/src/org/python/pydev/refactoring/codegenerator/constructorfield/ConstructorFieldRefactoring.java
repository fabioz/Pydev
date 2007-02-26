package org.python.pydev.refactoring.codegenerator.constructorfield;

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
import org.python.pydev.refactoring.ui.model.constructorfield.ClassFieldTreeProvider;
import org.python.pydev.refactoring.ui.pages.ConstructorFieldPage;

public class ConstructorFieldRefactoring extends AbstractPythonRefactoring {

	private ConstructorFieldRequestProcessor requestProcessor;

	private IChangeProcessor changeProcessor;

	public ConstructorFieldRefactoring(String name, RefactoringInfo req) {
		super(name, req);
		try {
			initWizard(name);
		} catch (Throwable e) {
			status.addInfo(UITexts.infoFixCode);
		}
	}

	private void initWizard(String name) throws Throwable {
		ClassFieldTreeProvider provider = new ClassFieldTreeProvider(req
				.getScopeClass());
		this.requestProcessor = new ConstructorFieldRequestProcessor();
		this.changeProcessor = new ConstructorFieldChangeProcessor(this.name,
				this.req, this.requestProcessor);
		this.pages.add(new ConstructorFieldPage(name, provider,
				requestProcessor));
	}

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		ClassDefAdapter rootClass = this.req.getScopeClass();

		if (rootClass != null) {
			if (rootClass.getAttributes().size() > 0) {
				return status;
			}
		}

		status.addFatalError(UITexts.constructorFieldUnavailable);

		return status;
	}

}
