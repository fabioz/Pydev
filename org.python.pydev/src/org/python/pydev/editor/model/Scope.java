/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Scope represents scope where local variables belong to.
 * 
 * There is a scope hierarchy:
 * ModuleNode scope
 *   Function scope
 *   Class scope
 *      Function scope
 */
public class Scope {
	private ArrayList locals;
	private AbstractNode start;
	private AbstractNode end;
	private Scope parent;
	private ArrayList children;
	
	public Scope(AbstractNode start) {
		this.start = start;
		AbstractNode startParent = start.getParent();
		if (startParent != null) {
			parent = startParent.getScope();
			parent.addChild(this);
		}
		children = new ArrayList();
	}
	
	private void addChild(Scope scope) {
		children.add(scope);
	}
	
	public ArrayList getChildren() {
		return children;
	}

	public LocalNode getLocalByName(String name) {
		if (locals == null)
			return null;
		Iterator i = locals.iterator();
		while (i.hasNext()) {
			LocalNode l = (LocalNode)i.next();
			if (l.toString().equals(name))
				return l;
		}
		return null;
	}

	void addLocalDefinition(LocalNode newLocal) {
		if (locals == null)
			locals =  new ArrayList();
		if (getLocalByName(newLocal.toString()) == null)
			locals.add(newLocal);
	}

	public Location getStart() {
		return start.getStart();
	}
	
	public AbstractNode getStartNode() {
		return start;
	}
	
	public Scope getParent() {
		return parent;
	}

	public Location getEnd() {
		return end.getEnd();
	}
	/**
	 * Computes where the last child of the node ends
	 */
	public void setEnd(AbstractNode end) {
		ArrayList children = end.getChildren();
		int size = children.size();
		AbstractNode trueEndNode = size > 0 ? (AbstractNode)children.get(size-1) : end;	
		this.end = trueEndNode;
	}
}
