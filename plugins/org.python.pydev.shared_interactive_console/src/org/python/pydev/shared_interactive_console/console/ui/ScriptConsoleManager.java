/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Helper to manage the consoles created.
 */
public class ScriptConsoleManager {

    private static ScriptConsoleManager instance;

    /**
     * @return the singleton for the script console manager.
     */
    public static synchronized ScriptConsoleManager getInstance() {
        if (instance == null) {
            instance = new ScriptConsoleManager();
        }

        return instance;
    }

    /**
     * Reference to the console manager singleton from eclipse.
     */
    private IConsoleManager manager;

    protected ScriptConsoleManager() {
        this.manager = ConsolePlugin.getDefault().getConsoleManager();
    }

    /**
     * Terminates the execution of the given console and removes it from the list of available consoles.
     * @param console the console to be terminated and removed.
     */
    public void close(ScriptConsole console) {
        console.terminate();
        manager.removeConsoles(new IConsole[] { console });
    }

    /**
     * Closes all the script consoles available.
     */
    public void closeAll() {
        IConsole[] consoles = manager.getConsoles();
        for (int i = 0; i < consoles.length; ++i) {
            IConsole console = consoles[i];
            if (console instanceof ScriptConsole) {
                close((ScriptConsole) console);
            }
        }
    }

    /**
     * Adds a given console to the console view.
     * 
     * @param console the console to be added to the console view
     * @param show whether it should be shown or not.
     */
    public void add(ScriptConsole console, boolean show) {
        manager.addConsoles(new IConsole[] { console });
        if (show) {
            manager.showConsoleView(console);
        }
    }

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     */
    public void linkWithDebugSelection(ScriptConsole console, boolean isChecked) {
        console.linkWithDebugSelection(isChecked);
    }

}
