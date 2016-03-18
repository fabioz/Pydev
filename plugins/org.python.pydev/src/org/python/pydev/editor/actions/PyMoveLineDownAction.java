/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.ResourceBundle;

import org.python.pydev.editor.PyEdit;

public class PyMoveLineDownAction extends PyMoveLineAction {

    public PyMoveLineDownAction(ResourceBundle bundle, String prefix, PyEdit editor) {
        super(bundle, prefix, editor);
    }

    @Override
    protected boolean getMoveUp() {
        return false;
    }

}
