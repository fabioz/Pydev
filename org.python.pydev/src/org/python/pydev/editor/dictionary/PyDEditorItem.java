/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.eclipse.core.runtime.IStatus;
import org.python.parser.SimpleNode;

import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.IParserListener;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Represents a PyEdit item.
 * I wanted to name this one PyDFileItem, but the file item can represent any
 * file, and this class really represents a live editor.
 */
public class PyDEditorItem extends PyDictionaryItem implements IParserListener{

	PyEdit editor;
	
	public PyDEditorItem(PyParser parser) {
		super(null);
		parser.addParseListener(this);
		// TODO Auto-generated constructor stub
	}
	
	public void createDictionary(SimpleNode topNode) {
		subItems = new PyDictionary();
		PopulateDictionary v = new PopulateDictionary(this, subItems);
		try {// traversal fills in the children
			topNode.traverse(v);
		} catch (Exception e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error creating dictionary", e);
			e.printStackTrace();
		}
	}
	
	/*
	 *  Callback from PyParser
	 */	
	 public void parserChanged(SimpleNode root) {
		createDictionary(root);
		System.out.println(toString());
	}

	/*
	 * Callback from PyParser
	 */
	public void parserError(Throwable error) {
		// we'll ignore errors
	}

	public String toString() {
		return "Top node\n" + subItems.toString();
	}
}
