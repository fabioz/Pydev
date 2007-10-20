/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.model.generateproperties.PropertyTreeProvider;
import org.python.pydev.refactoring.ui.pages.GeneratePropertiesPage;

public class GeneratePropertiesRefactoring extends AbstractPythonRefactoring {

	private GeneratePropertiesRequestProcessor requestProcessor;

	private IChangeProcessor changeProcessor;

	public GeneratePropertiesRefactoring(RefactoringInfo req) {
		super(req);
		try {
			initWizard();
		} catch (Throwable e) {
			status.addInfo(Messages.infoFixCode);
		}
	}

	private void initWizard() throws Throwable {
		PropertyTreeProvider provider = new PropertyTreeProvider(info.getClasses());
		this.requestProcessor = new GeneratePropertiesRequestProcessor(this.info.getNewLineDelim());
		this.changeProcessor = new GeneratePropertiesChangeProcessor(getName(), this.info, this.requestProcessor);
		this.pages.add(new GeneratePropertiesPage(getName(), provider, requestProcessor));
	}

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		List<IClassDefAdapter> classes = this.info.getClasses();

		if (classes.size() > 0) {
			for (IClassDefAdapter adapter : classes) {
				if (adapter.getAttributes().size() > 0) {
					return super.checkInitialConditions(pm);
				}
			}
		}
		status.addFatalError(Messages.generatePropertiesUnavailable);

		return status;
	}

	@Override
	public String getName() {
		return Messages.generatePropertiesLabel;
	}
}
