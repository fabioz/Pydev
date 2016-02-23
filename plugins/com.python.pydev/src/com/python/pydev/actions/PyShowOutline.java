/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.TreeSelectionDialog;

public class PyShowOutline extends PyAction {

    @Override
    public void run(IAction action) {

        PyEdit pyEdit = getPyEdit();

        TreeSelectionDialog dialog = new PyOutlineSelectionDialog(EditorUtils.getShell(), pyEdit);

        dialog.open(); //The dialog will take care of everything.
    }

}
