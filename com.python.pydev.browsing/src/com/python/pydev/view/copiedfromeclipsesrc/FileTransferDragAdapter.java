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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Drag support class to allow dragging of files and folder from
 * the packages view to another application.
 */
public class FileTransferDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {
	
	private ISelectionProvider fProvider;
	
	public FileTransferDragAdapter(ISelectionProvider provider) {
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}

	public Transfer getTransfer() {
		return FileTransfer.getInstance();
	}
	
	public void dragStart(DragSourceEvent event) {
		event.doit= isDragable(fProvider.getSelection());
	}
	
	private boolean isDragable(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return false;
		IStructuredSelection selection= (IStructuredSelection)s;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if ( !( element instanceof IFile || element instanceof IFolder ) ) {
				return false;
			}
		}
		List resources= convertIntoResources(selection);
		return resources.size() == selection.size();
	}
	
	public void dragSetData(DragSourceEvent event){
		List elements= getResources();
		if (elements == null || elements.size() == 0) {
			event.data= null;
			return;
		}
		
		event.data= getResourceLocations(elements);
		System.out.println( "dragSetData: " + event.data );
	}

	private static String[] getResourceLocations(List resources) {
		String locations[] = new String[resources.size()];
		for( int i=0; i<resources.size(); i++ ) {
			locations[i] = PydevPlugin.getIResourceOSString( (IResource)resources.get(i) );
		}
		return locations;
	}
	
	public void dragFinished(DragSourceEvent event) {
		if (!event.doit)
			return;
		
		if (event.detail == DND.DROP_MOVE) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=30543
			// handleDropMove(event);
		} else if (event.detail == DND.DROP_NONE || event.detail == DND.DROP_TARGET_MOVE) {
			handleRefresh(event);
		}
	}
	
	/* package */ void handleDropMove(DragSourceEvent event) {
		final List elements= getResources();
		if (elements == null || elements.size() == 0)
			return;
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					String message = "FileTransferDragAdaptr: handleDropMove()";
					monitor.beginTask(message, elements.size()); 
					MultiStatus status= createMultiStatus();
					Iterator iter= elements.iterator();
					while(iter.hasNext()) {
						IResource resource= (IResource)iter.next();
						try {
							monitor.subTask(resource.getFullPath().toOSString());
							resource.delete(true, null);
							
						} catch (CoreException e) {
							status.add(e.getStatus());
						} finally {
							monitor.worked(1);
						}
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		runOperation(op, true, false);
	}
	
	private  void handleRefresh(DragSourceEvent event) {
		final Set roots= collectRoots(getResources());
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				try {
					String message = "FileTransferAdapter: handleRefresh()";
					monitor.beginTask(message, roots.size()); 
					MultiStatus status= createMultiStatus();
					Iterator iter= roots.iterator();
					while (iter.hasNext()) {
						IResource r= (IResource)iter.next();
						try {
							r.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
						} catch (CoreException e) {
							status.add(e.getStatus());
						}	
					}
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} finally {
					monitor.done();
				}
			}
		};
		
		runOperation(op, true, false);
	}

	protected Set collectRoots(final List elements) {
		final Set roots= new HashSet(10);
		
		Iterator iter= elements.iterator();
		while (iter.hasNext()) {
			IResource resource= (IResource)iter.next();
			IResource parent= resource.getParent();
			if (parent == null) {
				roots.add(resource);
			} else {
				roots.add(parent);
			}
		}
		return roots;
	}
	
	private List getResources() {
		ISelection s= fProvider.getSelection();
		if (!(s instanceof IStructuredSelection)) 
			return null;
		
		return convertIntoResources((IStructuredSelection)s);
	}

	private List convertIntoResources(IStructuredSelection selection) {
		List result= new ArrayList(selection.size());
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object o= iter.next();
			IResource r= null;
			if (o instanceof IResource) {
				r= (IResource)o;
			} else if (o instanceof IAdaptable) {
				r= (IResource)((IAdaptable)o).getAdapter(IResource.class);
			}
			if (r != null)
				result.add(r);
		}
		return result;
	}
	
	private MultiStatus createMultiStatus() {
		String message = "FileTransferDragAdaptr: createMultiStatus()";
		return new MultiStatus(PydevPlugin.getPluginID(), 
			IStatus.OK, message, null); 
	}
	
	private void runOperation(IRunnableWithProgress op, boolean fork, boolean cancelable) {
		try {
			//Shell parent= JavaPlugin.getActiveWorkbenchShell();
			Shell parent= PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
			new ProgressMonitorDialog(parent).run(fork, cancelable, op);
		} catch (InvocationTargetException e) {
			//String message= PackagesMessages.DragAdapter_problem; 
			//String title= PackagesMessages.DragAdapter_problemTitle;
			String message = "FileTransferDragAdaptr: runOperation()";
			String title = "FileTransferDragAdaptr: runOperation()";
			//ExceptionHandler.handle(e, title, message);
		} catch (InterruptedException e) {
			// Do nothing. Operation has been canceled by user.
		}
	}
}
