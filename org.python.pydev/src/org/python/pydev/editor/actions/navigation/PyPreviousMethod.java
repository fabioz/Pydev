/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions.navigation;

import org.python.pydev.editor.model.*;


/**
 * @author Fabio Zadrozny
 */
public class PyPreviousMethod extends PyMethodNavigation {

	// me is the last node w
	public AbstractNode getSelect(AbstractNode me) {
		AbstractNode current = ModelUtils.getPreviousNode(me);
		while (current != null &&
			!(current instanceof FunctionNode) &&
			!(current instanceof ClassNode))
			current = ModelUtils.getPreviousNode(current);
		return current;	
	}
}
