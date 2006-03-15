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
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.BrowsingPlugin;
import com.python.pydev.browsing.ui.UIConstants;
import com.python.pydev.browsing.view.PackageExplorerMessages;
import com.python.pydev.browsing.view.PydevPackageExplorer;

/**
 * Collapse all nodes.
 */
class CollapseAllAction extends Action {
	
	private PydevPackageExplorer fPackageExplorer;
	
	CollapseAllAction(PydevPackageExplorer part) {
		super(PackageExplorerMessages.CollapseAllAction_label); 
		setDescription(PackageExplorerMessages.CollapseAllAction_description); 
		setToolTipText(PackageExplorerMessages.CollapseAllAction_tooltip);	
		
		ImageCache imageCache = new ImageCache(BrowsingPlugin.getDefault().getBundle().getEntry("/"));	
		try {
			setDisabledImageDescriptor( imageCache.getDescriptor(UIConstants.COLLAPSE_ALL_DISABLED) );
			setImageDescriptor( imageCache.getDescriptor(UIConstants.COLLAPSE_ALL_ENABLED) );
		} catch (MalformedURLException e) {		
			e.printStackTrace();
		}
		
		fPackageExplorer= part;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID() + ".open_type_hierarchy_action");
	}
 
	public void run() { 
		fPackageExplorer.collapseAll();
	}
}
