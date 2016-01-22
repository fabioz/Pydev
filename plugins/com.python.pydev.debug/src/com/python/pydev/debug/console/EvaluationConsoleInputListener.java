/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.console;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.python.pydev.debug.core.IConsoleInputListener;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.EvaluateExpressionCommand;

public class EvaluationConsoleInputListener implements IConsoleInputListener {

    private static final boolean DEBUG = false;
    private StringBuffer buf = new StringBuffer();

    public void newLineReceived(String lineReceived, AbstractDebugTarget target) {
        boolean evaluateNow = !lineReceived.startsWith(" ") && !lineReceived.startsWith("\t")
                && !lineReceived.endsWith(":") && !lineReceived.endsWith("\\");

        if (DEBUG) {
            System.out.println("line: '" + lineReceived + "'");
        }
        buf.append(lineReceived);
        if (lineReceived.length() > 0) {
            buf.append("@LINE@");
        }

        if (evaluateNow) {
            final String toEval = buf.toString();
            if (toEval.trim().length() > 0) {
                IAdaptable context = DebugUITools.getDebugContext();
                if (DEBUG) {
                    System.out.println("Evaluating:\n" + toEval);
                }
                if (context instanceof PyStackFrame) {
                    final PyStackFrame frame = (PyStackFrame) context;
                    target.postCommand(new EvaluateExpressionCommand(target, toEval, frame
                            .getLocalsLocator().getPyDBLocation(), true) {
                        @Override
                        public void processOKResponse(int cmdCode, String payload) {
                            frame.forceGetNewVariables();
                            super.processOKResponse(cmdCode, payload);
                        }
                    });
                }
            }
            buf = new StringBuffer();
        }

    }

    public void pasteReceived(String text, AbstractDebugTarget target) {
        if (DEBUG) {
            System.out.println("paste: '" + text + "'");
        }
        text = text.replaceAll("\r\n", "@LINE@").replaceAll("\r", "@LINE@").replaceAll("\n", "@LINE@");
        buf.append(text);
        buf.append("@LINE@");
    }

}
