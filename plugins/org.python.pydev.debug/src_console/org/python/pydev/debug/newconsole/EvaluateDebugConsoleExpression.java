/******************************************************************************
* Copyright (C) 2012-2013  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.EvaluateConsoleExpressionCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Class to exectute console command in the debugging context
 *
 * @author hussain.bohra
 * @author Fabio Zadrozny
 */
public class EvaluateDebugConsoleExpression implements ICommandResponseListener {

    String EMPTY = StringUtils.EMPTY;
    private String payload;
    private final PyStackFrame frame;

    public EvaluateDebugConsoleExpression(PyStackFrame frame) {
        Assert.isNotNull(frame);
        this.frame = frame;
    }

    /**
     * This method will get called from AbstractDebugTarget when
     * output arrives for the posted command
     */
    public void commandComplete(AbstractDebuggerCommand cmd) {
        try {
            this.payload = ((EvaluateConsoleExpressionCommand) cmd).getResponse();
        } catch (CoreException e) {
            this.payload = e.getMessage();
        }
    }

    /**
     * Execute the line in selected frame context
     *
     * @param consoleId
     * @param command
     */
    public void executeCommand(String command, boolean bufferedOutput) {
        AbstractDebugTarget target = frame.getTarget();
        String locator = getLocator(frame.getThreadId(), frame.getId(), bufferedOutput ? "EVALUATE"
                : "EVALUATE_UNBUFFERED", command);
        AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(target, locator,
                new ICommandResponseListener() {

                    public void commandComplete(AbstractDebuggerCommand cmd) {
                        frame.forceGetNewVariables();
                        EvaluateDebugConsoleExpression.this.commandComplete(cmd);
                    }
                });
        target.postCommand(cmd);
    }

    /**
     * Post the completions command
     *
     * @param consoleId
     * @param actTok
     * @param offset
     */
    public String getCompletions(String actTok, int offset) {
        AbstractDebugTarget target = frame.getTarget();
        String locator = getLocator(frame.getThreadId(), frame.getId(), "GET_COMPLETIONS", actTok);
        AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(target, locator, this);
        target.postCommand(cmd);
        return waitForCommand();
    }

    /**
     * Keeps in a loop for 3 seconds or until the completions are found. If no
     * completions are found in that time, returns an empty array.
     */
    public String waitForCommand() {
        int timeout = PydevConsoleConstants.CONSOLE_TIMEOUT; // wait up to 3 seconds
        while (--timeout > 0 && payload == null) {
            try {
                Thread.sleep(10); // 10 millis
            } catch (InterruptedException e) {
                // ignore
            }
        }

        String temp = this.payload;
        this.payload = null;
        if (temp == null) {
            Log.logInfo("Timeout for waiting for debug completions elapsed (3 seconds).");
            return EMPTY;
        }
        return temp;
    }

    /**
     * join and return all locators with '\t'
     *
     * @param locators
     * @return
     */
    private String getLocator(String... locators) {
        return StringUtils.join("\t", locators);
    }

    /**
     * This class represent the console message to be displayed in the debug console.
     *
     * @author hussain.bohra
     *
     */
    public static class PydevDebugConsoleMessage {

        private boolean more;
        private StringBuilder outputMessage = new StringBuilder();
        private StringBuilder errorMessage = new StringBuilder();

        public boolean isMore() {
            return more;
        }

        public void setMore(boolean more) {
            this.more = more;
        }

        public void appendMessage(String output, boolean isError) {
            if (!isError) {
                outputMessage.append(output);
                outputMessage.append("\n");
            } else {
                errorMessage.append(output);
                errorMessage.append("\n");
            }
        }

        public StringBuilder getOutputMessage() {
            return outputMessage;
        }

        public StringBuilder getErrorMessage() {
            return errorMessage;
        }
    }
}
