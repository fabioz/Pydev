/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.python.parser.ast.Name;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.plugin.PydevPlugin;

/**
 *
 * TODO Comment this class
 */
public class PyDLocalItem extends PyDictionaryItem {

	Name node;
	
	public PyDLocalItem(PyDictionaryItem parent, Name node) {
		super(parent);
		this.node = node;
		PopulateDictionary populator = new PopulateDictionary(this, subItems);
		try {
			node.traverse(populator);
		} catch (Exception e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error populating ClassDef", e);
			e.printStackTrace();
		}
	}

	public String toString() {
		return node.id;
	}

	/**
	 * get the name of the local variable
	 */
	public String getName() {
		return node.id;
	}
}
