package org.python.pydev.django.ui.wizards.project;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;

public class DjangoNewProjectPage extends NewProjectNameAndLocationWizardPage {
	/**
	 * Creates a new project creation wizard page.
	 * 
	 * @param pageName
	 *            the name of this page
	 */
	public DjangoNewProjectPage(String pageName) {
		super(pageName);
		setTitle("Pydev Django Project");
		setDescription("Create a new Pydev Django Project.");
	}

	/*
	 * (non-Javadoc) Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
	}
}
