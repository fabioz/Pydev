/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.actions;

import org.python.pydev.refactoring.codegenerator.constructorfield.ConstructorFieldRefactoring;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;

public class ConstructorFieldAction extends AbstractRefactoringAction {
	@Override
	protected AbstractPythonRefactoring createRefactoring(RefactoringInfo info) {
		return new ConstructorFieldRefactoring(info);
	}
}
