/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Interface for the console communication.
 *
 * This interface is meant to be the way to communicate with the shell.
 */
public interface IScriptConsoleCommunication {

    /**
     * Executes a given command in the interpreter (push a line)
     *
     * @param command the command to be executed
     * @param onContentsReceived
     * @return the response from the interpreter (contains the stdout, stderr, etc).
     * @throws Exception
     */
    void execInterpreter(String command, ICallback<Object, InterpreterResponse> onResponseReceived);

    /**
     * Creates the completions to be applied in the interpreter.
     *
     * @param text the full line
     * @param actTok the text with what should be completed (e.g.: xxx.bar.foo)
     * @param offset the offset where the completion was requested in the console document
     * @return a list of proposals that can be applied for the given text.
     * @throws Exception
     */
    public ICompletionProposal[] getCompletions(String text, String actTok, int offset, boolean showForTabCompletion)
            throws Exception;

    /**
     * Gets the description to be shown on hover to the user
     *
     * @param text the text representing the completion to be applied
     * @return the description to be shown to the user
     * @throws Exception
     */
    public String getDescription(String text) throws Exception;

    /**
     * Stops the communication with the server. Should ask the server to terminate at this point.
     * @throws Exception
     */
    void close() throws Exception;

    /**
     * Link pydev debug console with the suspended frame
     *
     * @param isLinkedWithDebug
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug);

    void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived);

    void interrupt();

    /**
     * I.e.: a debug console that doesn't have a frame is not connected.
     */
    boolean isConnected();

}
