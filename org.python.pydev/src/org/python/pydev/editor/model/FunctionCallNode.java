/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.eclipse.core.runtime.IStatus;
import org.python.parser.ast.Call;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Represents a function call.
 */
public class FunctionCallNode extends AbstractNode {

	Call astNode;
	/**
	 * @param parent
	 */
	public FunctionCallNode(AbstractNode parent, Call astNode, String lineText) {
		super(parent);
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine - 1, astNode.beginColumn - 1));
		LengthEstimator estimate = new LengthEstimator();
		try {
			astNode.traverse(estimate);
		} catch (Exception e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error estimating length of function call", e);
		}
		
		this.setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn + estimate.getLength()));
		fixColumnLocation(start, lineText);
		fixColumnLocation(end, lineText);
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return null;
	}
}
