/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.python.pydev.editor.model.*;

/**
 * One-trick pony, finds the next method.
 */
public class PyNextMethod extends PyMethodNavigation{

	/**
	 * Gets the next method/class definition
	 */
	public AbstractNode getSelect(AbstractNode me ) {
		AbstractNode current = ModelUtils.getNextNode(me);
		while (current != null &&
			!(current instanceof FunctionNode) &&
			!(current instanceof ClassNode))
			current = ModelUtils.getNextNode(current);
		return current;	
	}
}
