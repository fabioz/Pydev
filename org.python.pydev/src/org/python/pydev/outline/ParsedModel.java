/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import java.util.ArrayList;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.python.parser.SimpleNode;
import org.python.pydev.parser.IParserListener;
import org.python.pydev.parser.PyParser;

/**
 * ParsedModel represents a python file, parsed for OutlineView display
 * It takes PyParser, and converts it into a tree of ParsedItems
 */
public class ParsedModel implements IOutlineModel {

	PyParser parser;
	PyOutlinePage outline;
	IParserListener parserListener;
	
	ParsedItem root = null;	// A list of top nodes in this document. Used as a tree root

	public ParsedModel(PyOutlinePage outline, PyParser parser) {
		this.parser = parser;
		this.outline = outline;

		// Tell parser that we want to know about all the changes
		// make sure that the changes are propagated on the main thread
		parserListener = new IParserListener() {
			public void parserChanged(SimpleNode pyRoot) {
				final SimpleNode myRoot = pyRoot;
				Display.getDefault().syncExec( new Runnable() {
					public void run() {
						if (myRoot != null)
							setRoot(new ParsedItem(null, myRoot));
					}
				});
			}
			public void parserError(Throwable error) {
				final Throwable myError = error;
				Display.getDefault().syncExec(new Runnable() {
				public void run() {
					setError(myError);
				}
			});
				setError(error);
			}
		};
		parser.addParseListener(parserListener);

		root = new ParsedItem(null, parser.getRoot());
	}

	public void dispose() {		
		parser.removeParseListener(parserListener);
	}
	
	public Object getRoot() {
		return root;
	}

	// patchRootHelper makes oldItem just like the newItem
	//   the differnce between the two is 
	private void patchRootHelper(ParsedItem oldItem, ParsedItem newItem,
		ArrayList itemsToRefresh, ArrayList itemsToUpdate, boolean okToRefresh) {
		Object[] newChildren = newItem.getChildren();
		Object[] oldChildren = oldItem.getChildren();
		
		// stuctural change, different number of children, can stop recursion
		if (newChildren.length != oldChildren.length) {
			oldItem.token = newItem.token;
			oldItem.children = null; // forces reinitialization
			if (okToRefresh)
				itemsToRefresh.add(oldItem);
		}
		else {
		// Number of children is the same, fix up all the children
			for (int i=0; i<oldChildren.length; i++) {
				patchRootHelper((ParsedItem)oldChildren[i], (ParsedItem)newChildren[i], 
								itemsToRefresh, itemsToUpdate, okToRefresh);
			}
		// see if the node needs redisplay
			String oldTitle = oldItem.toString();
			String newTitle = newItem.toString();
			if (okToRefresh && !oldTitle.equals(newTitle))
				itemsToUpdate.add(oldItem);
			oldItem.token = newItem.token;
		}
	}
	/*
	 * Replaces current root
	 */
	public void setRoot(ParsedItem newRoot) {
//		System.out.println(newRoot.token.toString());
		// We'll try to do the 'least flicker replace'
		// compare the two root structures, and tell outline what to refresh
		if (root != null) {
			ArrayList itemsToRefresh = new ArrayList();
			ArrayList itemsToUpdate = new ArrayList();
			patchRootHelper(root, newRoot, itemsToRefresh, itemsToUpdate, true);
			outline.updateItems(itemsToUpdate.toArray());
			outline.refreshItems(itemsToRefresh.toArray());
		}
		else
		{
			System.out.println("No old root in ParsedModel!");
		}
	}
	
	public void setError(Throwable error) {
		// TODO put some error note on the items?
	}
	
	/*
	 */
	public IOutlineModel.SelectThis selectionChanged(StructuredSelection sel) {
		IOutlineModel.SelectThis position = null;
		if(sel.size() == 1) { // only sync the editing view if it is a single-selection
			ParsedItem p = (ParsedItem)sel.getFirstElement();
			position = p.getPosition();
		}
		return position;
	}

	/* (non-Javadoc)
	 *
	 */
	public int compare(Object e1, Object e2) {
		return ((ParsedItem)e1).compareTo((ParsedItem)e2);
	}
}
