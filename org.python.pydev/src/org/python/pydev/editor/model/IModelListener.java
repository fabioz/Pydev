/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

/**
 * PyEdit will broadcast model changes to IModelListeners.
 * 
 * modelChanged is generated every time document is parsed successfully
 */
public interface IModelListener {
	/**
	 * every time document gets parsed, it generates a new parse tree
	 * @param root - the root of the new model
	 */
	void modelChanged(AbstractNode root);
}
