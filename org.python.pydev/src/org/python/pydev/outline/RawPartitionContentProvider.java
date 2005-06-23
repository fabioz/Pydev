/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


/* 
 * Raw partitioning is flat, no children, so implementation of children
 * can be ignored
 */
class RawPartitionContentProvider implements ITreeContentProvider {
	
	public Object[] getElements(Object inputElement) {
		return ((RawPartitionModel)inputElement).getPositions();
	}

	public void dispose() {}
	public Object[] getChildren(Object parentElement) {return null;}
	public Object getParent(Object element) {return null;}
	public boolean hasChildren(Object element) {return false;}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
}