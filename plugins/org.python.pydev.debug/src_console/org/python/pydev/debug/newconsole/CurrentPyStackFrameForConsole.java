package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.console.IConsole;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;

/**
 * Only returns stacks which match the current console (and keeps the last one).
 *
 * A selection always changes it (so, always linked to debug context).
 */
public class CurrentPyStackFrameForConsole extends AnyPyStackFrameSelected {

    private IConsole console;

    public CurrentPyStackFrameForConsole(IConsole console) {
        super();
        Assert.isNotNull(console);
        this.console = console;
        isLinkedWithDebug = true;
    }

    @Override
    protected boolean acceptsSelection(PyStackFrame stackFrame) {
        if (super.acceptsSelection(stackFrame)) {
            AbstractDebugTarget target = (AbstractDebugTarget) stackFrame.getAdapter(IDebugTarget.class);
            if (DebugUITools.getConsole(target.getProcess()) == console) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        // Overridden to do nothing because this one is always linked.
    }
}
