/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_interactive_console.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

/**
 * Storage wrapper for the global history for the interactive console.
 */
public enum ScriptConsoleGlobalHistory {
    /** The singleton instance to use */
    INSTANCE;

    private static final String HISTORY_PY = "history.py";
    private final LinkedList<String> lines;

    private ScriptConsoleGlobalHistory() {
        lines = new LinkedListWarningOnSlowOperations<String>();
        load();
    }

    /**
     * Get the current contents of the global history.
     * @return a copy of the history. The returned value can be modified.
     */
    public synchronized List<String> get() {
        return new ArrayList<String>(lines);
    }

    /**
     * Append and store some new history.
     * @param history to store
     */
    public synchronized void append(List<String> history) {
        int historyMaxEntries = getHistoryMaxEntries();
        if (history.size() > historyMaxEntries) {
            //Get only the last entries
            history = history.subList(history.size() - historyMaxEntries, history.size());
        }

        if (history.size() >= historyMaxEntries) {
            lines.clear(); //We already know the current history will be larger than the available space
        }

        for (String line : history) {
            lines.add(line);
        }

        //If we ended with more than we could, remove the additional entries (using a LinkedList so that this can be fast).
        while (lines.size() > historyMaxEntries) {
            lines.removeFirst();
        }

        store();
    }

    /**
     * Erase and store the global history.
     */
    public synchronized void clear() {
        lines.clear();
        store();
    }

    private synchronized void load() {
        File history = getHistoryFile();

        if (history != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(history));
                try {
                    String line = br.readLine();
                    while (line != null) {
                        lines.add(line);
                        line = br.readLine();
                    }
                } catch (IOException e) {
                    Log.log("Failed reading existing console history.py", e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.log("Failed closing existing console history.py", e);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.logInfo("No existing console history at: " + history, e);
            }
        }
    }

    private synchronized void store() {
        File history = getHistoryFile();

        if (history != null) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(history));
                try {
                    for (String line : lines) {
                        bw.write(line);
                        bw.write("\n");
                    }
                } catch (IOException e) {
                    Log.log("Failed writing console history.py", e);
                } finally {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        Log.log("Failed closing console history.py", e);
                    }
                }
            } catch (IOException e) {
                Log.log("Failed creating console history.py", e);
            }
        }
    }

    private int getHistoryMaxEntries() {
        if (SharedCorePlugin.inTestMode()) {
            return ScriptConsoleUIConstants.DEFAULT_INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES;
        }

        InteractiveConsolePlugin plugin = InteractiveConsolePlugin.getDefault();
        IPreferenceStore store = plugin.getPreferenceStore();
        int historyMaxEntries = store
                .getInt(ScriptConsoleUIConstants.INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES);

        if (historyMaxEntries < 0) {
            historyMaxEntries = 0;
        }

        return historyMaxEntries;
    }

    private File getHistoryFile() {
        if (SharedCorePlugin.inTestMode()) {
            return null;
        }

        InteractiveConsolePlugin plugin = InteractiveConsolePlugin.getDefault();
        IPath location = plugin.getStateLocation();
        IPath path = location.append(HISTORY_PY);
        return path.toFile();
    }

}
