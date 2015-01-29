package org.python.pydev.debug.newconsole;

import org.python.pydev.debug.model.PyStackFrame;

public interface IPyStackFrameProvider {

    public PyStackFrame getLastSelectedFrame();

    public void linkWithDebugSelection(boolean isLinkedWithDebug);
}
