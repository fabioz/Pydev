/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.StructuredSelection;

/**
 * all the models in the outline view need to implement this interface
 */
public interface IOutlineModel {
	
	void dispose();
	
	/**
	 * @return topmost object in the tree model
	 * this object will be referenced in ContentProvider::getElements
	 */
	Object getRoot();
	
	/**
	 * standard comparasance of two items
	 * @return -1 if e1 < e2, 0 if ==, 1 if e2 > e1
	 */
	int compare(Object e1, Object e2);

	/**
	 * this will be called in response to selection event
	 * @param sel new selection
	 * @return Point that contains line/column, or item to be selected
	 */
	SelectionPosition getSelectionPosition(StructuredSelection sel);
}
