/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 9, 2004
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.model.ErrorDescription;

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
