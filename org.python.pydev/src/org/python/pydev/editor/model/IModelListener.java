/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.editor.ErrorDescription;
import org.python.pydev.parser.jython.SimpleNode;

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
	void modelChanged(SimpleNode root);

    /**
     * Every time the document changes its error state, it generates this notification
     */
	void errorChanged(ErrorDescription errorDesc);

}
