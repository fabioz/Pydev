/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRefactoring;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;
import org.python.pydev.refactoring.ui.pages.extractlocal.ExtractLocalInputPage;

public class ExtractLocalAction extends AbstractRefactoringAction {

    @Override
    protected AbstractPythonRefactoring createRefactoring(RefactoringInfo info) {
        return new ExtractLocalRefactoring(info);
    }

    @Override
    protected int getWizardFlags() {
        return RefactoringWizard.DIALOG_BASED_USER_INTERFACE | RefactoringWizard.PREVIEW_EXPAND_FIRST_NODE;
    }

    @Override
    protected IWizardPage createPage(RefactoringInfo info) {
        return new ExtractLocalInputPage();
    }
}
