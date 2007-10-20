/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.listener;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Listener;

public interface IValidationPage extends IWizardPage, Listener {

	public void setErrorMessage(String error);

	public void validate();
}
