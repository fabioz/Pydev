package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

public class EnableDisableBreakpointRulerActionDelegate extends
        AbstractRulerActionDelegate {

    @Override
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        return new EnableDisableBreakpointRulerAction(editor, rulerInfo);
    }

}
