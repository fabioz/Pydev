/*
 * Created on 25/09/2005
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;

public interface IOrganizeImports {

    /**
     * This function should organize the imports in the current pyedit.
     * 
     * @param ps this is the selection (contains the doc)
     * @param pyEdit this is the edit
     */
    void performArrangeImports(PySelection ps, PyEdit pyEdit);

}
