package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRefactoring;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;

public class ExtractMethodAction extends AbstractRefactoringAction {

	@Override
	public void run(IAction action) {
		run(ExtractMethodRefactoring.class, action);
	}

}
