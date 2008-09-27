/*
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 * Copyright (C) 2007  Reto Schüttel, Robin Stocker
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
