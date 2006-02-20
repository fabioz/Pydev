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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class CompositeActionGroup extends ActionGroup {

	private ActionGroup[] fGroups;
	
	public CompositeActionGroup() {
	}
	
	public CompositeActionGroup(ActionGroup[] groups) {
		setGroups(groups);
	}

	protected void setGroups(ActionGroup[] groups) {
		Assert.isTrue(fGroups == null);
		Assert.isNotNull(groups);
		fGroups= groups;		
	}
		
	public ActionGroup get(int index) {
		if (fGroups == null)
			return null;
		return fGroups[index];
	}
	
	public void addGroup(ActionGroup group) {
		if (fGroups == null) {
			fGroups= new ActionGroup[] { group };
		} else {
			ActionGroup[] newGroups= new ActionGroup[fGroups.length + 1];
			System.arraycopy(fGroups, 0, newGroups, 0, fGroups.length);
			newGroups[fGroups.length]= group;
			fGroups= newGroups;
		}
	}
	
	public void dispose() {
		super.dispose();
		if (fGroups == null)
			return;
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].dispose();
		}
	}

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (fGroups == null)
			return;
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].fillActionBars(actionBars);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (fGroups == null)
			return;
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].fillContextMenu(menu);
		}
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
		if (fGroups == null)
			return;
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].setContext(context);
		}
	}

	public void updateActionBars() {
		super.updateActionBars();
		if (fGroups == null)
			return;
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].updateActionBars();
		}
	}
}
