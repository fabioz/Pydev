package org.python.pydev.refactoring.ui.pages.listener;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Listener;

public interface IValidationPage extends IWizardPage, Listener {

	public void setErrorMessage(String error);

	public void validate();
}
