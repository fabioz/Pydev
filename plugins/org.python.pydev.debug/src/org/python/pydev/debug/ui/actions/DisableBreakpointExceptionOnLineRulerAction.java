package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;

public class DisableBreakpointExceptionOnLineRulerAction extends AbstractBreakpointRulerAction {

    private ITextEditor editor;
    private IVerticalRulerInfo rulerInfo;

    public DisableBreakpointExceptionOnLineRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        this.editor = editor;
        this.rulerInfo = rulerInfo;
    }

    @Override
    public void run() {
        System.out.println("Disable: " + rulerInfo.getLineOfLastMouseButtonActivity());
    }

    @Override
    public void update() {
        this.setText("Disable caught breakpoints in line: " + rulerInfo.getLineOfLastMouseButtonActivity());
        this.setEnabled(true);
    }
}