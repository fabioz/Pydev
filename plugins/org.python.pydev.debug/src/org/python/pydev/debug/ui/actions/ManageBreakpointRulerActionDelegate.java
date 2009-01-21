/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Breakpoints delegate
 */
public class ManageBreakpointRulerActionDelegate
    extends AbstractRulerActionDelegate {

    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        return new BreakpointRulerAction(editor, rulerInfo);
    }
}
