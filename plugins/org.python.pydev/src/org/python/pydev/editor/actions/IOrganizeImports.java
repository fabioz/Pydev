/*
 * Created on 25/09/2005
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;

/**
 * Interface that makes it possible for clients to interact with the organize imports feature, so that they
 * can change how it actually behaves.
 *
 * @author Fabio
 */
public interface IOrganizeImports {

    /**
     * This function is called just before the actual organize imports function is called.
     * 
     * @param ps this is the selection (contains the doc)
     * @param pyEdit this is the edit
     * 
     * @return true if the organize imports should proceed, and false if the organize imports should not proceed
     * (so, false cancels the default organize imports)
     * 
     * @note this function is always called within a write session in the document (because it should seem as a single 
     * operation for the user -- which has a single undo).
     */
    boolean beforePerformArrangeImports(PySelection ps, PyEdit pyEdit);

    /**
     * Called right after the whole import process is done. 
     * 
     * @param ps this is the selection (contains the doc)
     * @param pyEdit this is the edit
     */
    void afterPerformArrangeImports(PySelection ps, PyEdit pyEdit);

}
