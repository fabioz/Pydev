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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;

/**
 * This class allows console to communicate with python backend by using the existing
 * debug connection.
 *
 * @author hussain.bohra
 * @author Fabio Zadrozny
 */
public class PydevDebugConsoleCommunication implements IScriptConsoleCommunication {

    private int TIMEOUT = PydevConsoleConstants.CONSOLE_TIMEOUT;

    String EMPTY = StringUtils.EMPTY;

    /**
     * Signals that the next command added should be sent as an input to the server.
     */
    private volatile boolean waitingForInput;

    /**
     * Input that should be sent to the server (waiting for raw_input)
     */
    private volatile String inputReceived;

    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock = new Object();

    /**
     * Response that should be sent back to the shell.
     */
    private volatile InterpreterResponse nextResponse;

    private final IPyStackFrameProvider consoleFrameProvider;

    private ICallback<Object, Tuple<String, String>> onContentsReceived;

    private boolean bufferedOutput;

    public void setBufferedOutput(boolean bufferedOutput) {
        this.bufferedOutput = bufferedOutput;
    }

    public boolean getBufferedOutput() {
        return this.bufferedOutput;
    }

    public PydevDebugConsoleCommunication(boolean bufferedOutput, IPyStackFrameProvider consoleFrameProvider) {
        this.consoleFrameProvider = consoleFrameProvider;
        this.bufferedOutput = bufferedOutput;
    }

    @Override
    public boolean isConnected() {
        return this.consoleFrameProvider.getLastSelectedFrame() != null;
    }

    @Override
    public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
        this.onContentsReceived = onContentsReceived;
    }

    @Override
    public void interrupt() {
        //can't interrupt in the debug console for now...
    }

    public void execInterpreter(final String command, final ICallback<Object, InterpreterResponse> onResponseReceived) {

        nextResponse = null;
        if (waitingForInput) {
            inputReceived = command;
            waitingForInput = false;
            // the thread that we started in the last exec is still alive if we were waiting for an input.
        } else {
            // create a thread that'll keep locked until an answer is received from the server.
            Job job = new Job("PyDev Debug Console Communication") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    PyStackFrame frame = consoleFrameProvider.getLastSelectedFrame();
                    if (frame == null) {
                        if (onContentsReceived != null) {
                            onContentsReceived.call(new Tuple<String, String>(EMPTY,
                                    "[Invalid Frame]: Please select frame to connect the console.\n"));
                        }
                        nextResponse = new InterpreterResponse(false, false);
                        return Status.CANCEL_STATUS;
                    }
                    final EvaluateDebugConsoleExpression evaluateDebugConsoleExpression = new EvaluateDebugConsoleExpression(
                            frame);
                    evaluateDebugConsoleExpression.executeCommand(command, bufferedOutput);
                    String result = evaluateDebugConsoleExpression.waitForCommand();
                    try {
                        if (result.length() == 0) {
                            //timed out
                            if (onContentsReceived != null) {
                                onContentsReceived.call(new Tuple<String, String>(result, EMPTY));
                            }
                            nextResponse = new InterpreterResponse(false, false);
                            return Status.CANCEL_STATUS;

                        } else {
                            EvaluateDebugConsoleExpression.PydevDebugConsoleMessage consoleMessage = XMLUtils
                                    .getConsoleMessage(result);
                            if (onContentsReceived != null) {
                                onContentsReceived.call(new Tuple<String, String>(
                                        consoleMessage.getOutputMessage().toString(),
                                        consoleMessage.getErrorMessage().toString()));
                            }
                            nextResponse = new InterpreterResponse(consoleMessage.isMore(), false);
                        }
                    } catch (CoreException e) {
                        Log.log(e);
                        if (onContentsReceived != null) {
                            onContentsReceived.call(new Tuple<String, String>(result, EMPTY));
                        }
                        nextResponse = new InterpreterResponse(false, false);
                        return Status.CANCEL_STATUS;
                    }

                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }

        int timeOut = TIMEOUT; //only get contents each 500 millis...
        // busy loop until we have a response
        while (nextResponse == null) {
            synchronized (lock) {
                try {
                    lock.wait(20);
                } catch (InterruptedException e) {
                }
            }
            timeOut -= 20;

            if (timeOut <= 0 && nextResponse == null) {
                timeOut = TIMEOUT / 2; // after the first, get it each 250 millis
            }
        }
        onResponseReceived.call(nextResponse);
    }

    public ICompletionProposal[] getCompletions(String text, String actTok, int offset, boolean showForTabCompletion)
            throws Exception {
        ICompletionProposal[] receivedCompletions = {};
        if (waitingForInput) {
            return new ICompletionProposal[0];
        }

        PyStackFrame frame = consoleFrameProvider.getLastSelectedFrame();
        if (frame == null) {
            return new ICompletionProposal[0];
        }

        final EvaluateDebugConsoleExpression evaluateDebugConsoleExpression = new EvaluateDebugConsoleExpression(frame);
        String result = evaluateDebugConsoleExpression.getCompletions(actTok, offset);
        if (result.length() > 0) {
            List<Object[]> fromServer = XMLUtils.convertXMLcompletionsFromConsole(result);
            List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
            PydevConsoleCommunication.convertConsoleCompletionsToICompletions(text, actTok, offset, fromServer, ret, false);
            receivedCompletions = ret.toArray(new ICompletionProposal[ret.size()]);
        }
        return receivedCompletions;
    }

    public String getDescription(String text) throws Exception {
        return null;
    }

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        consoleFrameProvider.linkWithDebugSelection(isLinkedWithDebug);
    }

    public void close() throws Exception {
        //Do nothing on console close.
    }

}
