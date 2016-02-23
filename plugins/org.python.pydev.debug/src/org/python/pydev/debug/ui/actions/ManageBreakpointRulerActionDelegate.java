/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 30, 2004
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Breakpoints delegate
 */
public class ManageBreakpointRulerActionDelegate extends AbstractRulerActionDelegate {

    @Override
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        return new PyBreakpointRulerAction(editor, rulerInfo);
    }
}
