package org.python.pydev.refactoring.codegenerator.generateproperties;

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
import org.python.pydev.refactoring.ui.model.generateproperties.PropertyTreeProvider;
import org.python.pydev.refactoring.ui.pages.GeneratePropertiesPage;

public class GeneratePropertiesRefactoring extends AbstractPythonRefactoring {

	private GeneratePropertiesRequestProcessor requestProcessor;

	private IChangeProcessor changeProcessor;

	public GeneratePropertiesRefactoring(String name, RefactoringInfo req) {
		super(name, req);
		try {
			initWizard(name);
		} catch (Throwable e) {
			status.addInfo(UITexts.infoFixCode);
		}
	}

	private void initWizard(String name) throws Throwable {
		PropertyTreeProvider provider = new PropertyTreeProvider(req.getClasses());
		this.requestProcessor = new GeneratePropertiesRequestProcessor();
		this.changeProcessor = new GeneratePropertiesChangeProcessor(this.name, this.req,
				this.requestProcessor);
		this.pages.add(new GeneratePropertiesPage(name, provider,
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
		List<ClassDefAdapter> classes = this.req.getClasses();

		if (classes.size() > 0) {
			for (ClassDefAdapter adapter : classes) {
				if (adapter.getAttributes().size() > 0) {
					return super.checkInitialConditions(pm);
				}
			}
		}
		status.addFatalError(UITexts.generatePropertiesUnavailable);

		return status;
	}

}
