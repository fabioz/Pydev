/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.view.copiedfromeclipsesrc.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import com.python.pydev.view.copiedfromeclipsesrc.actions.SelectionDispatchAction;

import com.python.pydev.browsing.view.PackageExplorerMessages;

public class OpenPropertiesWorkingSetAction extends SelectionDispatchAction {

	public OpenPropertiesWorkingSetAction(IWorkbenchSite site) {
		super(site);
		setText(PackageExplorerMessages.OpenPropertiesWorkingSetAction_label); 
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getWorkingSet(selection) != null);
	}

	private IWorkingSet getWorkingSet(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object element= selection.getFirstElement();
		if (!(element instanceof IWorkingSet))
			return null;
		IWorkingSet ws= (IWorkingSet)element;
		if (!ws.isEditable())
			return null;
		return ws;
	}
	
	public void run(IStructuredSelection selection) {
		IWorkingSet ws= getWorkingSet(selection);
		if (ws == null)
			return;
		IWorkingSetEditWizard wizard= PlatformUI.getWorkbench().
			getWorkingSetManager().createWorkingSetEditWizard(ws);
		WizardDialog dialog= new WizardDialog(getShell(), wizard);
		dialog.open();
	}
}
