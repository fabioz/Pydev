/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.listener;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Listener;

public interface IValidationPage extends IWizardPage, Listener {

    void setErrorMessage(String error);

    void validate();
}
