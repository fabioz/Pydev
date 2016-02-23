/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.actions.ScopeSelectionAction;
import org.python.pydev.shared_ui.editor.BaseEditor;

/**
 * Deselects the scope based on a previous scope selection.
 * 
 * @author fabioz
 */
public class PyScopeDeselection extends PyAction {

    @Override
    public void run(IAction action) {
        try {
            BaseEditor pyEdit = getPyEdit();
            ScopeSelectionAction scopeSelectionAction = new ScopeSelectionAction();
            scopeSelectionAction.deselect(pyEdit);

        } catch (Exception e) {
            Log.log(e);
        }
    }

}
