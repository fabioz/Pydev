/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.python.parser.SimpleNode;
import org.python.parser.ast.*;

/**
 * PopulateDictionary creates a hierarchy of dictionaries.
 * 
 * Every dictionary includes: classes, methods, and local variables.
 * Each class/method have their own dictionaries.
 * 
 * The whole tree except for classes and nodes is traversed recursively by this visitor.
 * Classes and function definitions create their own dictionaries and traverse
 * their own portions of the tree. 
 * 
 * TODO
 * Detecting locals inside the AST is problematic. There is no clear way to detect them,
 * in Python locals need not be declared, so they appear automatically when used. This means
 * that detecting locals will be a perpetual hack. I'll mark the local detection code inside
 * this file "LOCALS" so that you can grep for it.
 * 
 * My current strategy is to trap visits to Name token. All locals manifestations have to involve
 * this token. But some Name's are not locals. The only way to trap this occurence is
 * higher up in the visitor pattern:
 * 
 * For example:
 * TryExcept::exceptTypeHandler::node.type is a Name.
 * To intercept this, I need to override:
 * exceptHandlerType::traverse so that it does not visit node.type
 * TryExcept::traverse to trap exceptHandlerType::traverse
 * 
 * Current list of overrides:
 * TryExcept: for exceptHandlerType::node
 * FunctionDef
 * Grepping through org.python.parser.TreeBuilder.java is useful. Here you can see how Name
 * is created and used.
 * TreeBuilder name usage:
 * JJTDOT_OP: creates an Attribute with embedded name
 * JJTFUNCDEF: Name is functon name
 * JJTEXTRAARGLIST: Part of ExtraArg
 * JJTEXTRAKEYWORDLIST: Part of ExtraArg
 * JJTCLASSDEF: Part of ClassDef
 * JJTGLOBAL: Part of Global
 * JJTKEYWORD: Part of keywordType
 * JJTIMPORTFROM: Part of ImportFrom
 * JJTDOTTED_NAME: creates the whole dotted name with Name.Load opearator?
 * JJTDOTTED_AS_NAME, JJTIMPORT_AS_NAME: aliasType...
 * 
 */

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
		node.traverse(this);
	}

	public Object visitAttribute(Attribute node) throws Exception {
		System.out.println("visitAttribute " + node.attr);
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
	
	/* Locals muck */

	/* Every name we reach is a local */
	public Object visitName(Name node) throws Exception {
		dict.addLocal(new PyDLocalItem(parent, node));
		node.traverse(this);
		return null;
	}

	/**
	 * Calls have function as a Name.
	 * do not traverse.
	 * Note: we might have to traverse Call's keywords and starargs
	 * Python example:
	 * printSomething() # printSomething is not a local
	 */ 
	public Object visitCall(Call node) throws Exception {
		return null;
	}
	
	/**
	 * ExceptHandler name declaration avoidance.
	 * To avoid:
	    try:
            import readline
        except ImportError:
     * ImportError being declared in a dictionary, we need to not
     * traverse TODO
	 */
	public Object visitTryExcept(TryExcept node) throws Exception {
		tryExceptTraverse(node, this);
		return null;
	}

	/**
	 * ExceptHandler name declaration avoidance.
	 * Only difference is lines commented out
	 * @see visitTryExcept
	 */
	public void excepthandlerTypeTraverse(excepthandlerType node, VisitorIF visitor) throws Exception{
//		if (node.type != null)
//			node.type.accept(visitor);
		if (node.name != null)
			node.name.accept(visitor);
		if (node.body != null) {
			for (int i = 0; i < node.body.length; i++) {
				if (node.body[i] != null)
					node.body[i].accept(visitor);
			}
		}
	}
	
	/** 
	 * Copy of TryExcept::traverse.
	 * The only difference is that I call excepthandlerTypeTraverse
	 * @see visitTryExcept
	 */
	public void tryExceptTraverse(TryExcept node, VisitorIF visitor) throws Exception {
		if (node.body != null) {
			for (int i = 0; i < node.body.length; i++) {
				if (node.body[i] != null)
					node.body[i].accept(visitor);
			}
		}
		if (node.handlers != null) {
			for (int i = 0; i < node.handlers.length; i++) {
				if (node.handlers[i] != null)
					excepthandlerTypeTraverse(node.handlers[i],this); // modified
			}
		}
		if (node.orelse != null) {
			for (int i = 0; i < node.orelse.length; i++) {
				if (node.orelse[i] != null)
					node.orelse[i].accept(visitor);
			}
		}
	}

	static void argumentsTypeTraverse(PopulateDictionary visitor, argumentsType node) throws Exception {
		if (node.args != null) {
			for (int i = 0; i < node.args.length; i++) {
				if (node.args[i] != null)
					node.args[i].accept(visitor);
			}	
		}
//		if (defaults != null) {
//			for (int i = 0; i < defaults.length; i++) {
//				if (defaults[i] != null)
//					defaults[i].accept(visitor);
//			}
//		}
		
	}

	static void FunctionDefTraverse(PopulateDictionary visitor, FunctionDef node) throws Exception {
		if (node.args != null)
			argumentsTypeTraverse(visitor, node.args);
//			node.args.traverse(visitor);
		if (node.body != null) {
			for (int i = 0; i < node.body.length; i++) {
				if (node.body[i] != null)
					node.body[i].accept(visitor);
			}
		}
	}
}