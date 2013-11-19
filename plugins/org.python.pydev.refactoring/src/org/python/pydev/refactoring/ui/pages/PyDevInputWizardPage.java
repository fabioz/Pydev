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

package org.python.pydev.refactoring.ui.pages;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.refactoring.ui.pages.listener.IValidationPage;

public abstract class PyDevInputWizardPage extends UserInputWizardPage implements IValidationPage {

    public PyDevInputWizardPage(String name) {
        super(name);
    }

    protected void voodooResizeToPage() {
        Point size = getShell().getSize();
        size.x += 1;
        size.y += 1;
        getShell().setSize(size);
        getShell().layout(true);
        size.x -= 1;
        size.y -= 1;
        getShell().setSize(size);
        getShell().layout(true);
    }

}
