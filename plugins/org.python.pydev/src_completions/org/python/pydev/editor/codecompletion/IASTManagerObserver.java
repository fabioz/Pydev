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
