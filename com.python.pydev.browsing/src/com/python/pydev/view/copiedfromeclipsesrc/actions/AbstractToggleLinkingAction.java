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

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.view.PackageExplorerMessages;

/**
 * This is an action template for actions that toggle whether
 * it links its selection to the active editor.
 * 
 * @since 3.0
 */
public abstract class AbstractToggleLinkingAction extends Action {
	
	/**
	 * Constructs a new action.
	 */
	public AbstractToggleLinkingAction() {
		super(PackageExplorerMessages.ToggleLinkingAction_label); 
		setDescription(PackageExplorerMessages.ToggleLinkingAction_description); 
		setToolTipText(PackageExplorerMessages.ToggleLinkingAction_tooltip); 
		//JavaPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, PydevPlugin.getPluginID() + ".link_editor_action");
	}

	/**
	 * Runs the action.
	 */
	public abstract void run();
}
