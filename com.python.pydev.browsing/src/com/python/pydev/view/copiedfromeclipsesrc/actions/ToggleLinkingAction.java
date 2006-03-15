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

import org.python.pydev.core.bundle.ImageCache;

import com.python.pydev.browsing.BrowsingPlugin;
import com.python.pydev.browsing.ui.UIConstants;
import com.python.pydev.browsing.view.PydevPackageExplorer;


/**
 * This action toggles whether this package explorer links its selection to the active
 * editor.
 * 
 * @since 2.1
 */
public class ToggleLinkingAction extends AbstractToggleLinkingAction {
	
	PydevPackageExplorer fPydevPackageExplorer;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleLinkingAction(PydevPackageExplorer explorer) {
		setChecked(explorer.isLinkingEnabled());
		fPydevPackageExplorer= explorer;
		
		ImageCache imageCache = new ImageCache(BrowsingPlugin.getDefault().getBundle().getEntry("/"));	
		try {			
			setImageDescriptor( imageCache.getDescriptor(UIConstants.LINK_WITH_EDITOR) );
		} catch (MalformedURLException e) {		
			e.printStackTrace();
		}
	}

	/**
	 * Runs the action.
	 */
	public void run() {
		fPydevPackageExplorer.setLinkingEnabled(isChecked());
	}
}
