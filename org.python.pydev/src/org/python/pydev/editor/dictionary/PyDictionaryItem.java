/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.eclipse.jface.text.IRegion;

/**
 * A generic dictionary item.
 * 
 * Subclasses hold additional information, such as class name/file position/etc.
 */
public class PyDictionaryItem {

	protected IRegion position;
	protected PyDictionaryItem parent;
	protected PyDictionary subItems;
	
	public PyDictionaryItem(PyDictionaryItem parent) {
		this.parent = parent;
		this.subItems = new PyDictionary();
	}

	public PyDictionaryItem getParent() {
		return parent;
	}
	
	public void setPosition(IRegion position) {
		this.position = position;
	}
	
	public IRegion getPosition() {
		return position;
	}
	
}
