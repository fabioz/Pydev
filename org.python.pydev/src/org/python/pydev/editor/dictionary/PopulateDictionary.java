/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.python.parser.SimpleNode;
import org.python.parser.ast.*;

class PopulateDictionary extends VisitorBase {

	PyDictionary dict;
	PyDictionaryItem parent;
	
	PopulateDictionary(PyDictionaryItem parent, PyDictionary dict) {
		this.parent = parent;
		this.dict = dict;
	}

	protected Object unhandled_node(SimpleNode node) throws Exception {
		return null;
	}

	public void traverse(SimpleNode node) throws Exception {
		System.out.println("Traverse?");
	}
	
	public Object visitAssign(Assign node) throws Exception {
		dict.addLocal(new PyDLocalItem(parent, node));
		return null;
	}


	public Object visitClassDef(ClassDef node) throws Exception {
		dict.addClass(new PyDClassItem(parent, node));
		return null;
	}

	public Object visitFunctionDef(FunctionDef node) throws Exception {
		dict.addFunction(new PyDFunctionItem(parent, node));
		return null;
	}
}