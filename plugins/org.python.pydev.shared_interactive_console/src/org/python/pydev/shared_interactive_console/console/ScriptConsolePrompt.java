/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
/**
 *
 */
package org.python.pydev.shared_interactive_console.console;

/**
 * Shows the prompt to the user (using the #toString()) method (e.g.: shows the >>> or ... )
 */
public class ScriptConsolePrompt {

    /**
     * String to be shown when a new command is requested
     */
    private final String newCommand;

    /**
     * String to be shown when the command still needs input to finish (e.g.: start class declaration)
     */
    private final String continueCommand;

    private boolean commandComplete;

    private boolean needInput;

    public ScriptConsolePrompt(String newCommand, String appendCommand) {
        this.newCommand = newCommand;
        this.continueCommand = appendCommand;
        this.commandComplete = true;
    }

    /**
     * Sets the mode for the prompt.
     *
     * @param mode if true, a new command prompt will be returned, if false, the 'continue' command prompt will be shown.
     */
    public void setMode(boolean mode) {
        this.commandComplete = mode;
    }

    @Override
    public String toString() {
        if (needInput) {
            return "";
        }
        return commandComplete ? newCommand : continueCommand;
    }

    /**
     * Sets whether the user is waiting for input. If it's, don't show the prompt.
     */
    public void setNeedInput(boolean needInput) {
        this.needInput = needInput;
    }

    public boolean getNeedInput() {
        return this.needInput;
    }

    public boolean getNeedMore() {
        return !commandComplete;
    }
}