/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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
