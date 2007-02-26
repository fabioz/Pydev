package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.refactoring.codegenerator.constructorfield.ConstructorFieldRefactoring;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;

public class ConstructorFieldAction extends AbstractRefactoringAction {

	@Override
	public void run(IAction action) {
		super.run(ConstructorFieldRefactoring.class, action);
	}

}
