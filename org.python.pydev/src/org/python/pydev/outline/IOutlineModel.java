/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.text.Region;
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
	 * @param sel: new selection
	 * @return Point that contains line/column, or item to be selected
	 */
	SelectThis selectionChanged(StructuredSelection sel);
	
	class SelectThis {
		Region r;

		int line;
		int column;  // use WHOLE_LINE to select the whole line
		int length;
		
		static final int WHOLE_LINE=999;

		SelectThis(Region r) {
			this.r = r;
		}
		SelectThis(int line, int column, int length) {
			this.line = line;
			this.column = column;
			this.length = length;
			this.r = null;
		}
	};
}
