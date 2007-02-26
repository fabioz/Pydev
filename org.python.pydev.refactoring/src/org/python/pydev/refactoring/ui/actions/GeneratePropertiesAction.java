package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.refactoring.codegenerator.generateproperties.GeneratePropertiesRefactoring;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;

public class GeneratePropertiesAction extends AbstractRefactoringAction {

	@Override
	public void run(IAction action) {
		run(GeneratePropertiesRefactoring.class, action);
	}

}