/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.ui.DebugUITools;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.debug.core.IConsoleInputListener;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This is the output stream for the remote debugger.
 * 
 * When a new line is entered in the console for the remote debugger, it will pass that for the
 * debug console input listeners.
 */
public final class ProcessServerOutputStream extends ByteArrayOutputStream {

    final List<IConsoleInputListener> participants;

    @SuppressWarnings("unchecked")
    public ProcessServerOutputStream() {
        participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_DEBUG_CONSOLE_INPUT_LISTENER);
    }

    @Override
    public synchronized void write(int b) {
        super.write(b);
        this.checkFinishedLine();
    }

    @Override
    public synchronized void write(byte b[], int off, int len) {
        super.write(b, off, len);
        this.checkFinishedLine();
    }

    @Override
    public void write(byte b[]) throws IOException {
        super.write(b);
        this.checkFinishedLine();
    }

    /**
     * Checks if the last thing entered was a new line, and if it was, notifies clients about it.
     */
    private void checkFinishedLine() {
        String s = this.toString();
        this.reset();
        char c;
        if (s.length() > 0 && ((c = s.charAt(s.length() - 1)) == '\n' || c == '\r')) {
            IAdaptable context = DebugUITools.getDebugContext();
            if (context != null) {
                s = StringUtils.rightTrim(s);
                Object adapter = context.getAdapter(IDebugTarget.class);
                if (adapter instanceof AbstractDebugTarget) {
                    AbstractDebugTarget target = (AbstractDebugTarget) adapter;

                    for (IConsoleInputListener listener : participants) {
                        listener.newLineReceived(s, target);
                    }
                }
            }
        }
    }
}
