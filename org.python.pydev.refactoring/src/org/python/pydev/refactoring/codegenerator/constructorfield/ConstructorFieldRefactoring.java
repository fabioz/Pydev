/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.constructorfield;

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
import org.python.pydev.refactoring.ui.model.constructorfield.ClassFieldTreeProvider;
import org.python.pydev.refactoring.ui.pages.ConstructorFieldPage;

public class ConstructorFieldRefactoring extends AbstractPythonRefactoring {

	private ConstructorFieldRequestProcessor requestProcessor;

	private IChangeProcessor changeProcessor;

	public ConstructorFieldRefactoring(RefactoringInfo req) {
		super(req);
		try {
			initWizard();
		} catch (Throwable e) {
			status.addInfo(Messages.infoFixCode);
		}
	}

	private void initWizard() throws Throwable {
		ClassFieldTreeProvider provider = new ClassFieldTreeProvider(info.getScopeClass());
		this.requestProcessor = new ConstructorFieldRequestProcessor(this.info.getNewLineDelim());
		this.changeProcessor = new ConstructorFieldChangeProcessor(getName(), this.info, this.requestProcessor);
		this.pages.add(new ConstructorFieldPage(getName(), provider, requestProcessor));
	}

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		IClassDefAdapter rootClass = this.info.getScopeClass();

		if (rootClass != null) {
			if (rootClass.getAttributes().size() > 0) {
				return status;
			}
		}

		status.addFatalError(Messages.constructorFieldUnavailable);

		return status;
	}

	@Override
	public String getName() {
		return Messages.constructorFieldLabel;
	}
}
