/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Utility functions: querying/conversion
 */
public class ModelUtils {

	public static AbstractNode getElement(AbstractNode root, int offset, IDocument doc, int properties) {
		try {
			int line = doc.getLineOfOffset(offset);
			IRegion r = doc.getLineInformation(line);
			return getElement(root, new Location(line, offset - r.getOffset()), properties);
		} catch (BadLocationException e) {
			PydevPlugin.log(IStatus.WARNING, "getElementByOffset failed", e);
		}
		return null;		
	}

	/**
	 * Depth-first search for a node that spans given location
	 * @param root: node to start the search with
	 * @param loc: location we are looking for
	 * @param properties: properties node must match. Pass in PROP_ANY for all nodes
	 * @return
	 */	
	public static AbstractNode getElement(AbstractNode root, Location loc, int properties) {
		if (root == null)
			return null;
		Iterator i = root.getChildren().iterator();
		while (i.hasNext()) {
			AbstractNode next = (AbstractNode)i.next();
			AbstractNode retVal = getElement(next, loc, properties);
			if (retVal != null)
				return retVal;
		}
		if (loc.contained(root.getStart(), root.getEnd())
			&& ((root.getProperties() & properties) == properties)) {
				System.out.println(root.toString());
				return root;
			}
		return null;
	}
	
	/**
	 * @return the closest node whose node.start() <= offset
	 */
	public static AbstractNode getLessOrEqualNode(AbstractNode root, int offset, IDocument doc) {
		try {
			int line = doc.getLineOfOffset(offset);
			IRegion r = doc.getLineInformation(line);
			return getLessOrEqualNode(root, new Location(line, offset - r.getOffset()));
		} catch (BadLocationException e) {
			PydevPlugin.log(IStatus.WARNING, "getScopeByOffset failed", e);
		}
		return null;
	}
	
	/**
	 * @return the closest node whose node.start() <= location
	 */
	public static AbstractNode getLessOrEqualNode(AbstractNode root, Location loc) {
		AbstractNode smallestSoFar = null;
		AbstractNode current = root;
		boolean greater = false;
		while (current != null && !greater) {
			if (current.getStart().compareTo(loc) <= 0)
				smallestSoFar = current;
			else
				greater = true;
			current = getNextNode(current);
		}
		return smallestSoFar;		
	}
	
	/**
	 * Gets the largest child of my ancestor that is smaller than me.
	 */
	private static AbstractNode getPreviousNodeHelper(AbstractNode node) {
		if (node == null)
			return null;
		ArrayList children = node.getChildren();
		if (children.size() == 0)
			return node;
		else
			return getPreviousNodeHelper((AbstractNode)children.get(children.size() -1));
	}

	/**
	 * This gets the previous node by position. The nodes are ordered,
	 * and each parent is smaller than its children.
	 * The siblings after me are greater, the ones before are smaller
	 * @return
	 */
	public static AbstractNode getPreviousNode(AbstractNode node) {
		if (node == null)
			return null;
		AbstractNode parent = node.getParent();
		if (parent == null)
			return null;
		ArrayList children = parent.getChildren();
		int pos = children.indexOf(node);
		if (pos == -1) {
			PydevPlugin.log(IStatus.ERROR, "Error traversing model tree", null);
			return null;
		}
		else if (pos > 0)
			return getPreviousNodeHelper((AbstractNode)children.get(pos-1));
		else 
			return parent;	
	}
	
	/**
	 * gets the smallest child of my ancestor that larger than me
	 */
	private static AbstractNode getNextNodeHelper(AbstractNode parent, AbstractNode child) {
		if (parent == null)
			return null;
		ArrayList children = parent.getChildren();
		int pos = children.indexOf(child);
		if (pos == -1) {
			PydevPlugin.log(IStatus.ERROR, "Error traversing model tree", null);
			return null;
		}
		else if (pos == children.size() - 1) 
			// last element, must traverse my parents
			//      /\
			return getNextNodeHelper(parent.getParent(), parent);
		else {
			return (AbstractNode)children.get(pos + 1);
		}
	}
	
	public static AbstractNode getNextNode(AbstractNode node) {
		if (node == null)
			return null;
		ArrayList children = node.getChildren();
		if (children.size() > 0)
			return (AbstractNode)children.get(0);
		else
			return getNextNodeHelper(node.getParent(), node);
	}
}
