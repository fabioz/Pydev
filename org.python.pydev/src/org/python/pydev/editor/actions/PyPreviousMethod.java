/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.ModelUtils;


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
