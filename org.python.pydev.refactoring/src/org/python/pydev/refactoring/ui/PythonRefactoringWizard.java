package org.python.pydev.refactoring.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.python.pydev.refactoring.PepticPlugin;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;

public class PythonRefactoringWizard extends RefactoringWizard {

	protected AbstractPythonRefactoring refactoring;

	public PythonRefactoringWizard(AbstractPythonRefactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		this.refactoring = refactoring;
		ImageDescriptor wizardImg = PepticPlugin.imageDescriptorFromPlugin(PepticPlugin.PLUGIN_ID, UITexts.imagePath + UITexts.imgLogo);
		this.setDefaultPageImageDescriptor(wizardImg);
	}

	@Override
	protected void addUserInputPages() {
		this.getShell().setMinimumSize(640, 480);
		for (IWizardPage page : refactoring.getPages()) {
			addPage(page);
		}
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		return super.getNextPage(page);
	}

}
