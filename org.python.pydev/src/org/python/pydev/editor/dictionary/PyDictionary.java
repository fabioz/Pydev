/*
 * Author: atotic
 * Created on Mar 26, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import java.util.ArrayList;
import java.util.Iterator;


/**
 *
 * TODO Comment this class
 */
public class PyDictionary {
		
	ArrayList classes;	
	ArrayList functions;
	ArrayList locals;
	ArrayList methods;
	
	public PyDictionary() {
		classes = new ArrayList();
		functions = new ArrayList();
		locals = new ArrayList();
	}
	
	public void addClass(PyDClassItem newClass) {
		classes.add(newClass);
	}
	
	public void addFunction(PyDFunctionItem newFunction) {
		functions.add(newFunction);
	}

	public void addLocal(PyDLocalItem newLocal) {
		// eliminate dupes
		if (!hasLocal(newLocal))
			locals.add(newLocal);
	}
	
	boolean hasLocal(PyDLocalItem local) {
		for (Iterator i = locals.iterator();i.hasNext();)
			if (local.getName().equals(((PyDLocalItem)i.next()).getName()))
				return true;
		return false;
	}
	/**
	 * For debugging, pretty print the dictionary
	 */
	public String toString() {
		StringBuffer buf =  new StringBuffer();
		if (!classes.isEmpty()) {
			buf.append("Classes: ");
			Iterator i = classes.iterator();
			while (i.hasNext())
				buf.append(i.next().toString());
//			buf.append("\n");
		}
		if (!functions.isEmpty()) {
			buf.append("Functions: ");
			Iterator i = functions.iterator();
			while (i.hasNext())
				buf.append(i.next().toString());
//			buf.append("\n");
		}
		if (!locals.isEmpty()) {
			buf.append("Locals: ");
			Iterator i = locals.iterator();
			while (i.hasNext()) {
				buf.append(i.next().toString());
				buf.append(",");
			}
//			buf.append("\n");
		}
		return buf.toString();
	}
}
