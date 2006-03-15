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

import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.BrowsingPlugin;
import com.python.pydev.browsing.ui.UIConstants;
import com.python.pydev.browsing.view.PackageExplorerMessages;
import com.python.pydev.browsing.view.PydevPackageExplorer;

/**
 * Adds view menus to switch between flat and hierarchical layout.
 * 
 * @since 2.1
 */
class LayoutActionGroup extends MultiActionGroup {

	LayoutActionGroup(PydevPackageExplorer packageExplorer) {
		super(createActions(packageExplorer), getSelectedState(packageExplorer));
	}

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		contributeToViewMenu(actionBars.getMenuManager());
	}
	
	private void contributeToViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator());

		// Create layout sub menu
		
		IMenuManager layoutSubMenu= new MenuManager(PackageExplorerMessages.LayoutActionGroup_label); 
		final String layoutGroupName= "layout"; //$NON-NLS-1$
		Separator marker= new Separator(layoutGroupName);

		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		viewMenu.add(marker);
		viewMenu.appendToGroup(layoutGroupName, layoutSubMenu);
		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
		addActions(layoutSubMenu);
	}

	static int getSelectedState(PydevPackageExplorer packageExplorer) {
		if (packageExplorer.isFlatLayout())
			return 0;
		else
			return 1;
	}
	
	static IAction[] createActions(PydevPackageExplorer packageExplorer) {
		IAction flatLayoutAction= new LayoutAction(packageExplorer, true);
		flatLayoutAction.setText(PackageExplorerMessages.LayoutActionGroup_flatLayoutAction_label);
		
		IAction hierarchicalLayout= new LayoutAction(packageExplorer, false);
		hierarchicalLayout.setText(PackageExplorerMessages.LayoutActionGroup_hierarchicalLayoutAction_label);
		
		ImageCache imageCache = new ImageCache(BrowsingPlugin.getDefault().getBundle().getEntry("/"));		
		
		try {
			flatLayoutAction.setImageDescriptor(imageCache.getDescriptor(UIConstants.FLAT_LAYOUT));
			hierarchicalLayout.setImageDescriptor(imageCache.getDescriptor(UIConstants.HIERARCHICAL_LAYOUT));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		return new IAction[]{flatLayoutAction, hierarchicalLayout};
	}
}

public class LayoutAction extends Action implements IAction {

	private boolean fIsFlatLayout;
	private PydevPackageExplorer fPackageExplorer;

	public LayoutAction(PydevPackageExplorer packageExplorer, boolean flat) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$

		fIsFlatLayout= flat;
		fPackageExplorer= packageExplorer;
		if (fIsFlatLayout)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID()+".layout_flat_action");
		else
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID()+".layout_hierarchical_action");
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fPackageExplorer.isFlatLayout() != fIsFlatLayout)
			fPackageExplorer.toggleLayout();
	}
	
}
