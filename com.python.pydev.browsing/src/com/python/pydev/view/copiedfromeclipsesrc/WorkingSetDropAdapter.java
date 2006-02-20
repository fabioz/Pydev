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
package com.python.pydev.view.copiedfromeclipsesrc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.workingsets.OthersWorkingSetUpdater;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import com.python.pydev.view.copiedfromeclipsesrc.MultiElementSelection;

import com.python.pydev.browsing.view.PydevPackageExplorer;
import com.python.pydev.view.copiedfromeclipsesrc.actions.TreePath;

public class WorkingSetDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {
	
	private PydevPackageExplorer fPackageExplorer;
	
	private IStructuredSelection fSelection;
	private Object[] fElementsToAdds;
	private Set fCurrentElements;
	private IWorkingSet fWorkingSet;

	public WorkingSetDropAdapter(PydevPackageExplorer part) {
		super(part.getTreeViewer(), DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
		fPackageExplorer= part;
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}
	
	public boolean isEnabled(DropTargetEvent event) {
		Object target= event.item != null ? event.item.getData() : null;
		if (target == null)
			return false;
		ISelection selection= LocalSelectionTransfer.getInstance().getSelection();
		if (!isValidSelection(selection)) {
			return false;
		}
		if (!isValidTarget(target))
			return false;
		
		initializeState(target, selection);
		return true;
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		switch(operation) {
			case DND.DROP_DEFAULT:
			case DND.DROP_COPY:
			case DND.DROP_MOVE:
				event.detail= validateTarget(target, operation); 
				break;
			case DND.DROP_LINK:
				event.detail= DND.DROP_NONE; 
				break;
		}
	}
	
	private int validateTarget(Object target, int operation) {
		showInsertionFeedback(false);
		setDefaultFeedback(DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
		if (!isValidTarget(target))
			return DND.DROP_NONE;
		ISelection s= LocalSelectionTransfer.getInstance().getSelection();
		if (!isValidSelection(s)) {
			return DND.DROP_NONE;
		}
		
		initializeState(target, s);
		
		if (isWorkingSetSelection()) {
			setDefaultFeedback(DND.FEEDBACK_SCROLL);
			if (fLocation == LOCATION_BEFORE || fLocation == LOCATION_AFTER) {
				showInsertionFeedback(true);
				return DND.DROP_MOVE;
			}
			return DND.DROP_NONE;
		} else {
			if (isOthersWorkingSet(fWorkingSet) && operation == DND.DROP_COPY)
				return DND.DROP_NONE;
			
			List realJavaElements= new ArrayList();
			List realResource= new ArrayList();
			ReorgUtils.splitIntoJavaElementsAndResources(fElementsToAdds, realJavaElements, realResource);
			if (fElementsToAdds.length != realJavaElements.size() + realResource.size())
				return DND.DROP_NONE;
			for (Iterator iter= realJavaElements.iterator(); iter.hasNext();) {
				IJavaElement element= (IJavaElement)iter.next();
				if (ReorgUtils.containsElementOrParent(fCurrentElements, element))
					return DND.DROP_NONE;
			}
			for (Iterator iter= realResource.iterator(); iter.hasNext();) {
				IResource element= (IResource)iter.next();
				if (ReorgUtils.containsElementOrParent(fCurrentElements, element))
					return DND.DROP_NONE;
			}
			if (!(fSelection instanceof MultiElementSelection)) {
				return DND.DROP_COPY;
			}
			MultiElementSelection ms= (MultiElementSelection)fSelection;
			TreePath[] paths= ms.getAllTreePaths();
			for (int i= 0; i < paths.length; i++) {
				TreePath path= paths[i];
				if (path.getSegmentCount() != 2)
					return DND.DROP_COPY;
				if (!(path.getSegment(0) instanceof IWorkingSet))
					return DND.DROP_COPY;
				if (paths.length == 1) {
					IWorkingSet ws= (IWorkingSet)path.getSegment(0);
					if (OthersWorkingSetUpdater.ID.equals(ws.getId()))
						return DND.DROP_MOVE;
				}
			}
		}
		if (operation == DND.DROP_DEFAULT)
			return DND.DROP_MOVE;
		return operation;
	}

	private boolean isValidTarget(Object target) {
		return target instanceof IWorkingSet;
	}
	
	private boolean isValidSelection(ISelection selection) {
		return selection instanceof IStructuredSelection;
	}
	
	private boolean isOthersWorkingSet(IWorkingSet ws) {
		return OthersWorkingSetUpdater.ID.equals(ws.getId());
	}
	
	private void initializeState(Object target, ISelection s) {
		fWorkingSet= (IWorkingSet)target;
		fSelection= (IStructuredSelection)s;
		fElementsToAdds= fSelection.toArray();
		fCurrentElements= new HashSet(Arrays.asList(fWorkingSet.getElements()));
	}
	
	private boolean isWorkingSetSelection() {
		for (int i= 0; i < fElementsToAdds.length; i++) {
			if (!(fElementsToAdds[i] instanceof IWorkingSet))
				return false;
		}
		return true;
	}

	public void drop(Object target, final DropTargetEvent event) {
		if (isWorkingSetSelection()) {
			performWorkingSetReordering();
		} else {
			performElementRearrange(event.detail);
		}
		// drag adapter has nothing to do, even on move.
		event.detail= DND.DROP_NONE;
	}

	private void performWorkingSetReordering() {
		WorkingSetModel model= fPackageExplorer.getWorkingSetModel();
		List activeWorkingSets= new ArrayList(Arrays.asList(model.getActiveWorkingSets()));
		int index= activeWorkingSets.indexOf(fWorkingSet);
		if (index != -1) {
			if (fLocation == LOCATION_AFTER)
				index++;
			List result= new ArrayList(activeWorkingSets.size());
			List selected= new ArrayList(Arrays.asList(fElementsToAdds));
			for (int i= 0; i < activeWorkingSets.size(); i++) {
				if (i == index) {
					result.addAll(selected);
				}
				Object element= activeWorkingSets.get(i);
				if (!selected.contains(element)) {
					result.add(element);
				}
			}
			if (index == activeWorkingSets.size())
				result.addAll(selected);
			model.setActiveWorkingSets((IWorkingSet[])result.toArray(new IWorkingSet[result.size()]));
		}
	}
	
	private void performElementRearrange(int eventDetail) {
		// only move if target isn't the other working set. If this is the case
		// the move will happenn automatically by refreshing the other working set
		if (!isOthersWorkingSet(fWorkingSet)) {
			List elements= new ArrayList(Arrays.asList(fWorkingSet.getElements()));
			elements.addAll(Arrays.asList(fElementsToAdds));
			fWorkingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		}
		if (eventDetail == DND.DROP_MOVE) {
			MultiElementSelection ms= (MultiElementSelection)fSelection;
			Map workingSets= groupByWorkingSets(ms.getAllTreePaths());
			for (Iterator iter= workingSets.keySet().iterator(); iter.hasNext();) {
				IWorkingSet ws= (IWorkingSet)iter.next();
				List toRemove= (List)workingSets.get(ws);
				List currentElements= new ArrayList(Arrays.asList(ws.getElements()));
				currentElements.removeAll(toRemove);
				ws.setElements((IAdaptable[])currentElements.toArray(new IAdaptable[currentElements.size()]));
			}
		}
	}

	private Map/*<List<IWorkingSet>>*/ groupByWorkingSets(TreePath[] paths) {
		Map result= new HashMap();
		for (int i= 0; i < paths.length; i++) {
			TreePath path= paths[i];
			IWorkingSet ws= (IWorkingSet)path.getSegment(0);
			List l= (List)result.get(ws);
			if (l == null) {
				l= new ArrayList();
				result.put(ws, l);
			}
			l.add(path.getSegment(1));
		}
		return result;
	}
	
	//---- test methods for JUnit test since DnD is hard to simulate
	
	public int internalTestValidateTarget(Object target, int operation) {
		return validateTarget(target, operation);
	}
	
	public void internalTestDrop(Object target, int eventDetail) {
		if (isWorkingSetSelection()) {
			performWorkingSetReordering();
		} else {
			performElementRearrange(eventDetail);
		}
	}
}
