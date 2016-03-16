/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.actions.ShiftLeftAction;

/**
 * This action was created so that we can make the shift left even if there are less characters in the line than
 * the expected indent (the default shift left won't do the dedent in that case).
 */
public class PyShiftLeft extends PyAction {

    /**
     * Grabs the selection information and performs the action.
     * 
     * Note that setting the rewrite session and undo/redo must be done from the caller.
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = (PyEdit) getTextEditor();
            IIndentPrefs indentPrefs = pyEdit.getIndentPrefs();
            PySelection ps = new PySelection(pyEdit);
            perform(ps, indentPrefs);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @param indentPrefs 
     * @return the new selection
     * @throws BadLocationException 
     */
    public void perform(PySelection ps, IIndentPrefs indentPrefs) throws BadLocationException {
        ShiftLeftAction.perform(ps, indentPrefs.getTabWidth());
    }
}
