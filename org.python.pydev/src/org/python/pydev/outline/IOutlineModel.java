/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * all the models in the outline view need to implement this interface
 */
public interface IOutlineModel {
	
	void dispose();
	
	/**
	 * @return topmost object in the tree model
	 * this object will be referenced in ContentProvider::getElements
	 */
	ParsedItem getRoot();
	
	/**
	 * this will be called in response to selection event
	 * @param sel new selection
	 * @return Point that contains line/column, or item to be selected
	 */
	SimpleNode[] getSelectionPosition(StructuredSelection sel);
}
