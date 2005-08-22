/*
 * Created on Apr 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

/**
 * @author Fabio Zadrozny
 */
public interface IPyCompletionProposal {

    int PRIORITY_LOCALS = -1;
    int PRIORITY_DEFAULT = 10;
    int PRIORITY_PACKAGES = 100;
    
    /**
     * @return the priority for showing this completion proposal, so that lower priorities are
     * shown earlier in the list.
     */
    public int getPriority();
}
