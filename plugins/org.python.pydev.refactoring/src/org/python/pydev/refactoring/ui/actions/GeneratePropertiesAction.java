/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
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
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.refactoring.codegenerator.generateproperties.GeneratePropertiesRefactoring;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.model.generateproperties.PropertyTreeProvider;
import org.python.pydev.refactoring.ui.actions.internal.AbstractRefactoringAction;
import org.python.pydev.refactoring.ui.pages.GeneratePropertiesPage;

public class GeneratePropertiesAction extends AbstractRefactoringAction {
    @Override
    protected AbstractPythonRefactoring createRefactoring(RefactoringInfo info) {
        return new GeneratePropertiesRefactoring(info);
    }

    @Override
    protected IWizardPage createPage(RefactoringInfo info) {
        PropertyTreeProvider provider = new PropertyTreeProvider(info.getClasses());
        return new GeneratePropertiesPage(provider);
    }
}
