/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.python.pydev.core.ICodeCompletionASTManager;

/**
 * An extension point notified when ASTManager is created and assigned to 
 * a project.
 * 
 * @author radim@kubacki.cz (Radim Kubacki)
 */
public interface IASTManagerObserver {

    /**
     *  Called when AST manager is created and associated with a project.
     *   
     * @param manager
     */
    void notifyASTManagerAttached(ICodeCompletionASTManager manager);
}
