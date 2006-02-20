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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.ui.packageview.GotoResourceAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup;
import org.eclipse.jdt.internal.ui.workingsets.ViewActionGroup;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetActionGroup;
import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.ImportActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;
import org.eclipse.jdt.ui.actions.ProjectActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenInNewWindowAction;
import org.eclipse.ui.views.framelist.BackAction;
import org.eclipse.ui.views.framelist.ForwardAction;
import org.eclipse.ui.views.framelist.Frame;
import org.eclipse.ui.views.framelist.FrameAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.GoIntoAction;
import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.framelist.UpAction;

import com.python.pydev.browsing.view.PydevPackageExplorer;
import com.python.pydev.view.copiedfromeclipsesrc.CustomFiltersActionGroup;
import com.python.pydev.view.copiedfromeclipsesrc.PackagesFrameSource;

public class PackageExplorerActionGroup extends CompositeActionGroup {

	private PydevPackageExplorer fPart;

	private FrameList fFrameList;
	private GoIntoAction fZoomInAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private UpAction fUpAction;
	private GotoTypeAction fGotoTypeAction;
	private GotoPackageAction fGotoPackageAction;
	private GotoResourceAction fGotoResourceAction;
	private CollapseAllAction fCollapseAllAction;
	
	
	private ToggleLinkingAction fToggleLinkingAction;

	private RefactorActionGroup fRefactorActionGroup;
	private NavigateActionGroup fNavigateActionGroup;
	private ViewActionGroup fViewActionGroup;
	
	private CustomFiltersActionGroup fCustomFiltersActionGroup;

	private IAction fGotoRequiredProjectAction;	
 	
	public PackageExplorerActionGroup(PydevPackageExplorer part) {
		super();
		fPart= part;
		TreeViewer viewer= part.getViewer();
		
		IPropertyChangeListener workingSetListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doWorkingSetChanged(event);
			}
		};
		
		IWorkbenchPartSite site = fPart.getSite();
		setGroups(new ActionGroup[] {
			new NewWizardsActionGroup(site),
			fNavigateActionGroup= new NavigateActionGroup(fPart), 
			new CCPActionGroup(fPart),
            new GenerateBuildPathActionGroup(fPart),
			new GenerateActionGroup(fPart), 
			fRefactorActionGroup= new RefactorActionGroup(fPart),
			new ImportActionGroup(fPart),
			new BuildActionGroup(fPart),
			new JavaSearchActionGroup(fPart),
			new ProjectActionGroup(fPart), 
			fViewActionGroup= new ViewActionGroup(fPart.getRootMode(), workingSetListener, site),
			fCustomFiltersActionGroup= new CustomFiltersActionGroup(fPart, viewer),
			new LayoutActionGroup(fPart),
			// the working set action group must be created after the project action group
			new WorkingSetActionGroup(fPart)});
		

		fViewActionGroup.fillFilters(viewer);
		
		PackagesFrameSource frameSource= new PackagesFrameSource(fPart);
		fFrameList= new FrameList(frameSource);
		frameSource.connectTo(fFrameList);
			
		fZoomInAction= new GoIntoAction(fFrameList);
		fBackAction= new BackAction(fFrameList);
		fForwardAction= new ForwardAction(fFrameList);
		fUpAction= new UpAction(fFrameList);
		
		fGotoTypeAction= new GotoTypeAction(fPart);
		fGotoPackageAction= new GotoPackageAction(fPart);
		//fGotoResourceAction= new GotoResourceAction(fPart);
		fCollapseAllAction= new CollapseAllAction(fPart);	
		fToggleLinkingAction = new ToggleLinkingAction(fPart); 
		//fGotoRequiredProjectAction= new GotoRequiredProjectAction(fPart);
	}

	public void dispose() {
		super.dispose();
	}
	

	//---- Persistent state -----------------------------------------------------------------------

	public void restoreFilterAndSorterState(IMemento memento) {
		fViewActionGroup.restoreState(memento);
		fCustomFiltersActionGroup.restoreState(memento);
	}
	
	public void saveFilterAndSorterState(IMemento memento) {
		fViewActionGroup.saveState(memento);
		fCustomFiltersActionGroup.saveState(memento);
	}

	//---- Action Bars ----------------------------------------------------------------------------

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		setGlobalActionHandlers(actionBars);
		fillToolBar(actionBars.getToolBarManager());
		fillViewMenu(actionBars.getMenuManager());		
	}

	/* package  */ void updateActionBars(IActionBars actionBars) {
		actionBars.getToolBarManager().removeAll();
		actionBars.getMenuManager().removeAll();
		fillActionBars(actionBars);
		actionBars.updateActionBars();
		fZoomInAction.setEnabled(true);
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		// Navigate Go Into and Go To actions.
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_INTO, fZoomInAction);
		actionBars.setGlobalActionHandler(ActionFactory.BACK.getId(), fBackAction);
		actionBars.setGlobalActionHandler(ActionFactory.FORWARD.getId(), fForwardAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.UP, fUpAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.GO_TO_RESOURCE, fGotoResourceAction);
		actionBars.setGlobalActionHandler("org.eclipse.jdt.ui.actions.GoToType", fGotoTypeAction);
		actionBars.setGlobalActionHandler("org.python.pydev.views.packageexplorer.copiedfromeclipsesrc.actions.GoToPackage", fGotoPackageAction);
		
		fRefactorActionGroup.retargetFileMenuActions(actionBars);
	}

	/* package */ void fillToolBar(IToolBarManager toolBar) {
		toolBar.add(fBackAction);
		toolBar.add(fForwardAction);
		toolBar.add(fUpAction); 
		
		toolBar.add(new Separator());
		toolBar.add(fCollapseAllAction);
		toolBar.add(fToggleLinkingAction);

	}
	
	/* package */ void fillViewMenu(IMenuManager menu) {
		menu.add(fToggleLinkingAction);

		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
	}

	//---- Context menu -------------------------------------------------------------------------

	public void fillContextMenu(IMenuManager menu) {		
		IStructuredSelection selection= (IStructuredSelection)getContext().getSelection();
		int size= selection.size();
		Object element= selection.getFirstElement();
		
		/*if (element instanceof ClassPathContainer.RequiredProjectWrapper) 
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fGotoRequiredProjectAction);*/
		
		addGotoMenu(menu, element, size);
		
		addOpenNewWindowAction(menu, element);
		
		super.fillContextMenu(menu);
	}
	
	 private void addGotoMenu(IMenuManager menu, Object element, int size) {
		boolean enabled= size == 1 && fPart.getViewer().isExpandable(element) && (isGoIntoTarget(element) || element instanceof IContainer);
		fZoomInAction.setEnabled(enabled);
		if (enabled)
			menu.appendToGroup("group.goto", fZoomInAction);
	}
	
	private boolean isGoIntoTarget(Object element) {
		if (element == null)
			return false;
		if ( element instanceof IProject || element instanceof IFolder || element instanceof IWorkingSet ) {			
			return true;
		}		
		return false;
	}

	private void addOpenNewWindowAction(IMenuManager menu, Object element) {		
		// fix for 64890 Package explorer out of sync when open/closing projects [package explorer] 64890  
		if (element instanceof IProject && !((IProject)element).isOpen()) 
			return;
		
		if (!(element instanceof IContainer))
			return;
		menu.appendToGroup(
				"group.open", 
			new OpenInNewWindowAction(fPart.getSite().getWorkbenchWindow(), (IContainer)element));
	}

	//---- Key board and mouse handling ------------------------------------------------------------

	/* package*/ /*void handleDoubleClick(DoubleClickEvent event) {
		TreeViewer viewer= fPart.getViewer();
		Object element= ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (viewer.isExpandable(element)) {
			if (doubleClickGoesInto()) {
				// don't zoom into compilation units and class files
				if (element instanceof ICompilationUnit || element instanceof IClassFile)
					return;
				if (element instanceof IOpenable || element instanceof IContainer) {
					fZoomInAction.run();
				}
			} else {
				IAction openAction= fNavigateActionGroup.getOpenAction();
				if (openAction != null && openAction.isEnabled() && OpenStrategy.getOpenMethod() == OpenStrategy.DOUBLE_CLICK)
					return;
				viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
		}
	}*/
	
	/* package */ void handleOpen(OpenEvent event) {
		/*IAction openAction= fNavigateActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}*/
	}
	
	public void handleKeyEvent(KeyEvent event) {
		if (event.stateMask != 0) 
			return;		
		
		if (event.keyCode == SWT.BS) {
			if (fUpAction != null && fUpAction.isEnabled()) {
				fUpAction.run();
				event.doit= false;
			}
		}
	}
	
	private void doWorkingSetChanged(PropertyChangeEvent event) {
		System.out.println("MODE_CHANGED: " + ViewActionGroup.MODE_CHANGED);
		System.out.println("event.getProperty(): " + event.getProperty());
		if (ViewActionGroup.MODE_CHANGED.equals(event.getProperty())) {
			System.out.println("True");
			fPart.rootModeChanged(((Integer)event.getNewValue()).intValue());
			Object oldInput= null;
			Object newInput= null;
			if (fPart.showProjects()) {				
				oldInput= fPart.getWorkingSetModel();
				newInput= ResourcesPlugin.getWorkspace().getRoot();
				System.out.println("Passou projects");
			} else if (fPart.showWorkingSets()) {
				oldInput= ResourcesPlugin.getWorkspace().getRoot();
				newInput= fPart.getWorkingSetModel();
				System.out.println("Passou working set");
			}
			System.out.println("newInput " + newInput);
			if (oldInput != null && newInput != null) {
				Frame frame;
				for (int i= 0; (frame= fFrameList.getFrame(i)) != null; i++) {
					if (frame instanceof TreeFrame) {
						TreeFrame treeFrame= (TreeFrame)frame;
						if (oldInput.equals(treeFrame.getInput()))
							treeFrame.setInput(newInput);
					}
				}
			}
			System.out.println("newInput " + newInput);
			fPart.getViewer().setInput(newInput);
			fPart.getViewer().refresh();
		} else {
			System.out.println("False");
			IWorkingSet workingSet= (IWorkingSet) event.getNewValue();
			System.out.println("workingSet: " + workingSet);
			
			String workingSetName= null;
			if (workingSet != null)
				workingSetName= workingSet.getName();
			fPart.setWorkingSetName(workingSetName);
			fPart.updateTitle();
			
			String property= event.getProperty();
			if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
				System.out.println("Eu passei por aki...");
				TreeViewer viewer= fPart.getViewer();
				viewer.getControl().setRedraw(false);
				if( workingSet!=null ) {
					viewer.setInput(workingSet);
				} else {
					viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());					
				}
				viewer.refresh();
				viewer.getControl().setRedraw(true);
			}
		}
	}

	private boolean doubleClickGoesInto() {
		//return PreferenceConstants.DOUBLE_CLICK_GOES_INTO.equals(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.DOUBLE_CLICK));
		return true;
	}

	public FrameAction getUpAction() {
		return fUpAction;
	}

	public FrameAction getBackAction() {
		return fBackAction;
	}
	public FrameAction getForwardAction() {
		return fForwardAction;
	}

	public ViewActionGroup getWorkingSetActionGroup() {
	    return fViewActionGroup;
	}
	
	public CustomFiltersActionGroup getCustomFilterActionGroup() {
	    return fCustomFiltersActionGroup;
	}		
}
