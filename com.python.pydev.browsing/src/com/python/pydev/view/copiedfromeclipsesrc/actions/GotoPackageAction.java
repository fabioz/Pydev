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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.view.PackageExplorerMessages;
import com.python.pydev.browsing.view.PydevPackageExplorer;
import com.python.pydev.browsing.view.PydevPackageExplorerContentProvider;
import com.python.pydev.view.copiedfromeclipsesrc.Core;


class GotoPackageAction extends Action {
	
	private PydevPackageExplorer fPackageExplorer;
	
	GotoPackageAction(PydevPackageExplorer part) {
		super(PackageExplorerMessages.GotoPackage_action_label); 
		setDescription(PackageExplorerMessages.GotoPackage_action_description); 
		fPackageExplorer = part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID()+"."+"goto_package_action");
	}
 
	public void run() { 
		try {
			Shell shell= PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
			SelectionDialog dialog= createAllPackagesDialog(shell);
			dialog.setTitle(getDialogTitle());
			dialog.setMessage(PackageExplorerMessages.GotoPackage_dialog_message); 
			dialog.open();		
			Object[] res= dialog.getResult();
			if (res != null && res.length == 1) 
				gotoPackage((IResource)res[0]); 
		} catch (Exception e) {
		}
	}
	
	private SelectionDialog createAllPackagesDialog(Shell shell) {
//		ElementListSelectionDialog dialog= new ElementListSelectionDialog(
//			shell, (ILabelProvider)new PydevPackageExplorerContentProvider(fPackageExplorer.getViewer()) );
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(
				shell, (ILabelProvider)PydevPackageExplorerContentProvider.getInstance() );
		dialog.setIgnoreCase(false);
		dialog.setElements(collectPackages()); // XXX inefficient
		return dialog;
	}
	
	private Object[] collectPackages() {
		IWorkspaceRoot wsroot= PydevPlugin.getWorkspace().getRoot();				
		IProject[] projects= wsroot.getProjects();
		Set set= new HashSet(); 
		List allPackages= new ArrayList();
		//PydevPackageExplorerContentProvider cp = new PydevPackageExplorerContentProvider(fPackageExplorer.getViewer());  
		PydevPackageExplorerContentProvider cp = PydevPackageExplorerContentProvider.getInstance();
		for (int i= 0; i < projects.length; i++) {
			//IPackageFragmentRoot[] roots= projects[i].getPackageFragmentRoots();
			Object[] roots = cp.getChildren( projects[i] );
			for (int j= 0; j < roots.length; j++) {
				//IPackageFragmentRoot root= roots[j];
				Object root= roots[j];
		 		if (!isFiltered(root) && !set.contains(root)) {
					set.add(root);
					Object[] packages = cp.getChildren(root);					
					//IJavaElement[] packages= root.getChildren();
					appendPackages(allPackages, (IResource[]) packages);
				}
			}
		}
		return allPackages.toArray();
	}
	
	private void appendPackages(List all, IResource[] packages) {
		for (int i= 0; i < packages.length; i++) {
			IResource element= packages[i];
			if (!isFiltered(element))
				all.add(element);
		}
	}
		
	private void gotoPackage(IResource p) {
		fPackageExplorer.selectReveal(new StructuredSelection(p));
		if (!p.equals(getSelectedElement())) {
			MessageDialog.openInformation(fPackageExplorer.getSite().getShell(), 
				getDialogTitle(), MessageFormat.format(PackageExplorerMessages.PackageExplorer_element_not_present, new Object[] { p.getName()}));			 
		}
	}
	
	private Object getSelectedElement() {
		return ((IStructuredSelection)fPackageExplorer.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}	
	
	private String getDialogTitle() {
		return PackageExplorerMessages.GotoPackage_dialog_title; 
	}
	
	private boolean isFiltered(Object element) {
		StructuredViewer viewer= fPackageExplorer.getViewer();
		ViewerFilter[] filters= viewer.getFilters();
		if (filters != null) {
			for (int i = 0; i < filters.length; i++) {
				if (!filters[i].select(viewer, viewer.getInput(), element))
					return true;
			}
		}
		return false;
	}
}

