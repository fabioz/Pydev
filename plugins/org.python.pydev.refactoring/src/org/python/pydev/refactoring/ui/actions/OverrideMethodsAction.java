/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.actions;

import org.eclipse.jface.wizard.IWizardPage;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.codegenerator.overridemethods.OverrideMethodsRefactoring;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.model.overridemethods.ClassMethodsTreeProvider;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;
import org.python.pydev.refactoring.ui.pages.OverrideMethodsPage;

public class OverrideMethodsAction extends AbstractRefactoringAction {
    @Override
    protected AbstractPythonRefactoring createRefactoring(RefactoringInfo info) {
        return new OverrideMethodsRefactoring(info);
    }

    @Override
    protected IWizardPage createPage(RefactoringInfo info) {
        ClassMethodsTreeProvider provider;
        try {
            provider = new ClassMethodsTreeProvider(info.getScopeClassAndBases());
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        return new OverrideMethodsPage(provider);
    }
}
