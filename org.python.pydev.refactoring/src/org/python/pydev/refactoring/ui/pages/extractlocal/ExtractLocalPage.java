/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.extractlocal;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRequestProcessor;
import org.python.pydev.refactoring.ui.pages.PyDevInputWizardPage;

// FIXME: The page is too large, make it smaller.
public class ExtractLocalPage extends PyDevInputWizardPage {

	public ExtractLocalRequestProcessor requestProcessor;

	private ExtractLocalComposite extractComposite;

	private Composite parent;

	public ExtractLocalPage(String name, ExtractLocalRequestProcessor requestProcessor) {
		super(name);
		this.setTitle(name);
		this.requestProcessor = requestProcessor;
	}

	public void createControl(Composite parent) {
		this.parent = parent;
		setupComposite();
	}

	public void setupComposite() {
		if (extractComposite != null) {
			extractComposite.dispose();
			extractComposite = null;
		}
		
		extractComposite = new ExtractLocalComposite(this, parent, requestProcessor.getScopeAdapter());

		extractComposite.registerListeners(this);
		setControl(this.extractComposite);

		voodooResizeToPage();
		setPageComplete(false);
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	public void validate() {
		setErrorMessage(null);
		extractComposite.validate();
		setPageComplete(getErrorMessage() == null);
		if (isPageComplete()) {
			applySettings();
		}
	}

	private void applySettings() {
		this.requestProcessor.setVariableName(extractComposite.getVariableName());
	}

	public void handleEvent(Event event) {
		validate();
	}
}
