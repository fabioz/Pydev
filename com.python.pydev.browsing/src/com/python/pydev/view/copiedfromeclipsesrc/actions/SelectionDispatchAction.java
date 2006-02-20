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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;

/**
 * Action that dispatches the <code>IAction#run()</code> and the 
 * <code>ISelectionChangedListener#selectionChanged</code> 
 * according to the type of the selection. 
 * 
 * <ul>
 * 	<li>if selection is of type <code>ITextSelection</code> then
 * 	<code>run(ITextSelection)</code> and <code>selectionChanged(ITextSelection)</code>
 * 	is called.</li> 
 * 	<li>if selection is of type <code>IStructuredSelection</code> then
 * 	<code>run(IStructuredSelection)</code> and <code>
 * 	selectionChanged(IStructuredSelection)</code> is called.</li>
 * 	<li>default is to call <code>run(ISelection)</code> and <code>
 * 	selectionChanged(ISelection)</code>.</li>
 * </ul>
 * 
 * <p>
 * Note: This class is not intended to be subclassed outside the JDT UI plug-in.
 * </p>
 * 
 * @since 2.0
 */
public abstract class SelectionDispatchAction extends Action implements ISelectionChangedListener {
	
	private IWorkbenchSite fSite;
	
	/**
	 * Creates a new action with no text and no image.
	 * <p>
	 * Configure the action later using the set methods.
	 * </p>
	 * 
	 * @param site the site this action is working on
	 */
	protected SelectionDispatchAction(IWorkbenchSite site) {
		Assert.isNotNull(site);
		fSite= site;
	}

	/**
	 * Returns the site owning this action.
	 * 
	 * @return the site owning this action
	 */
	public IWorkbenchSite getSite() {
		return fSite;
	}

	/**
	 * Returns the selection provided by the site owning this action.
	 * 
	 * @return the site's selection
	 */	
	public ISelection getSelection() {
		if (getSelectionProvider() != null)
			return getSelectionProvider().getSelection();
		else
			return null;
	}

	/**
	 * Returns the shell provided by the site owning this action.
	 * 
	 * @return the site's shell	
	 */
	public  Shell getShell() {
		return fSite.getShell();
	}
	
	/**
	 * Returns the selection provider managed by the site owning this action.
	 * 
	 * @return the site's selection provider	
	 */
	public ISelectionProvider getSelectionProvider() {
		return fSite.getSelectionProvider();
	}

	/**
	 * Updates the action's enablement state according to the given selection. This
	 * default implementation calls one of the <code>selectionChanged</code>
	 * methods depending on the type of the passed selection.
	 * 
	 * @param selection the selection this action is working on
	 */
	public void update(ISelection selection) {
		dispatchSelectionChanged(selection);
	}

	/**
	 * Notifies this action that the given structured selection has changed. This default
	 * implementation calls <code>selectionChanged(ISelection selection)</code>.
	 * 
	 * @param selection the new selection
 	 */
	public void selectionChanged(IStructuredSelection selection) {
		selectionChanged((ISelection)selection);
	}

	/**
	 * Executes this actions with the given structured selection. This default implementation
	 * calls <code>run(ISelection selection)</code>.
	 * 
	 * @param selection the selection
	 */
	public void run(IStructuredSelection selection) {
		run((ISelection)selection);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * 
	 * @param selection the selection
	 */
	public void selectionChanged(TextSelection selection) {
		selectionChanged((ITextSelection)selection);
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * 
	 * @param selection the selection
	 */
	public void run(TextSelection selection) {
		run((ITextSelection)selection);
	}
	
	/**
	 * Notifies this action that the given text selection has changed.  This default
	 * implementation calls <code>selectionChanged(ISelection selection)</code>.
	 * 
	 * @param selection the new selection
 	 */
	public void selectionChanged(ITextSelection selection) {
		selectionChanged((ISelection)selection);
	}
	
	/**
	 * Executes this actions with the given text selection. This default implementation
	 * calls <code>run(ISelection selection)</code>.
	 * 
	 * @param selection the selection
	 */
	public void run(ITextSelection selection) {
		run((ISelection)selection);
	}
	
	/**
	 * Notifies this action that the given selection has changed.  This default
	 * implementation sets the action's enablement state to <code>false</code>.
	 * 
	 * @param selection the new selection
 	 */
	public void selectionChanged(ISelection selection) {
		setEnabled(false);
	}
	
	/**
	 * Executes this actions with the given selection. This default implementation
	 * does nothing.
	 * 
	 * @param selection the selection
	 */
	public void run(ISelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on IAction.
	 */
	public void run() {
		dispatchRun(getSelection());
	}
	
	/* (non-Javadoc)
	 * Method declared on ISelectionChangedListener.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		dispatchSelectionChanged(event.getSelection());
	}

	private void dispatchSelectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			selectionChanged((IStructuredSelection)selection);
		}/* else if (selection instanceof JavaTextSelection) {
			selectionChanged((JavaTextSelection)selection);
		}*/ else if (selection instanceof ITextSelection) {
			selectionChanged((ITextSelection)selection);
		} else {
			selectionChanged(selection);
		}
	}

	private void dispatchRun(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			run((IStructuredSelection)selection);
		}/* else if (selection instanceof JavaTextSelection) {
			run((JavaTextSelection)selection);
	    }*/ else if (selection instanceof ITextSelection) {
			run((ITextSelection)selection);
		} else {
			run(selection);
		}
	}
}
