/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.eclipse.core.runtime.IStatus;
import org.python.parser.ast.ClassDef;
import org.python.pydev.plugin.PydevPlugin;

/**
 *
 * TODO Comment this class
 */
public class PyDClassItem extends PyDictionaryItem {

	ClassDef node;

	public PyDClassItem(PyDictionaryItem parent, ClassDef node) {
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
		return "Class " + node.name + "\n" + subItems.toString();
	}
}
