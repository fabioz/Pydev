package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.refactoring.codegenerator.overridemethods.OverrideMethodsRefactoring;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;

public class OverrideMethodsAction extends AbstractRefactoringAction {

	@Override
	public void run(IAction action) {
		super.run(OverrideMethodsRefactoring.class, action);
	}

}
