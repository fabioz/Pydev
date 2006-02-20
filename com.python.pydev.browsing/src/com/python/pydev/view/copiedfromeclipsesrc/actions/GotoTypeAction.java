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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.view.PackageExplorerMessages;
import com.python.pydev.browsing.view.PydevPackageExplorer;
import com.python.pydev.view.copiedfromeclipsesrc.ExceptionHandler;

class GotoTypeAction extends Action {
	public static final int CONSIDER_CLASSES= 1 << 1;
	public static final int CONSIDER_INTERFACES= 1 << 2;
	public static final int CONSIDER_TYPES= CONSIDER_CLASSES | CONSIDER_INTERFACES;
	
	
	private PydevPackageExplorer fPackageExplorer;
	
	GotoTypeAction(PydevPackageExplorer part) {
		super();
		setText(PackageExplorerMessages.GotoType_action_label); 
		setDescription(PackageExplorerMessages.GotoType_action_description); 
		fPackageExplorer= part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID() + "goto_type_action");
	}


	public void run() {
		System.out.println("GotoTypeAction");
		Shell shell= PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
		SelectionDialog dialog= null;
		/*try {			
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), CONSIDER_TYPES, false);
		} catch (Exception e) {
			String title= getDialogTitle();
			String message= PackagesMessages.GotoType_error_message; 
			ExceptionHandler.handle(e, title, message);			
			return;
		}
	
		dialog.setTitle(getDialogTitle());
		dialog.setMessage(PackagesMessages.GotoType_dialog_message); 
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			gotoType((IType) types[0]);
		}*/
	}
	
	//private void gotoType(IType type) {
		/*ICompilationUnit cu= (ICompilationUnit) type.getAncestor(IJavaElement.COMPILATION_UNIT);
		IJavaElement element= null;
		if (cu != null) {
			element= JavaModelUtil.toOriginal(cu);
		}
		else {
			element= type.getAncestor(IJavaElement.CLASS_FILE);
		}
		if (element != null) {
			PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
			if (view != null) {
				view.selectReveal(new StructuredSelection(element));
				if (!element.equals(getSelectedElement(view))) {
					MessageDialog.openInformation(fPackageExplorer.getSite().getShell(), 
						getDialogTitle(), 
						Messages.format(PackagesMessages.PackageExplorer_element_not_present, element.getElementName())); 
				}
			}
		}*/
	//}
	
	private Object getSelectedElement(PydevPackageExplorer view) {
		return ((IStructuredSelection)view.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}	
	
	private String getDialogTitle() { 
		return PackageExplorerMessages.GotoType_dialog_title;
	}
}
