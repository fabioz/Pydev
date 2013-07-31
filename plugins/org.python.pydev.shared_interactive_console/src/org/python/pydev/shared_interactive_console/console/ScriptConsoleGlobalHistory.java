package org.python.pydev.shared_interactive_console.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

/**
 * Storage wrapper for the global history for the interactive console.
 */
public enum ScriptConsoleGlobalHistory {
    /** The singleton instance to use */
    INSTANCE;

    private static final String HISTORY_PY = "history.py";
    private List<String> lines;

    private ScriptConsoleGlobalHistory() {
        lines = new ArrayList<String>();
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
        for (String line : history) {
            lines.add(line);
        }

        int historyMaxEntries = getHistoryMaxEntries();
        if (lines.size() > historyMaxEntries) {
            lines.subList(lines.size() - historyMaxEntries - 1, lines.size() - 1);
            lines = new ArrayList<String>(lines);
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
                Log.log("No existing console history.py", e);
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
        InteractiveConsolePlugin plugin = InteractiveConsolePlugin.getDefault();
        int historyMaxEntries;
        if (plugin != null) {
            IPreferenceStore store = plugin.getPreferenceStore();
            historyMaxEntries = store
                    .getInt(ScriptConsoleUIConstants.INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES);
        } else {
            historyMaxEntries = ScriptConsoleUIConstants.DEFAULT_INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES;
        }

        if (historyMaxEntries < 0) {
            historyMaxEntries = 0;
        }

        return historyMaxEntries;
    }

    private File getHistoryFile() {
        InteractiveConsolePlugin plugin = InteractiveConsolePlugin.getDefault();

        if (plugin != null) {
            IPath location = plugin.getStateLocation();
            IPath path = location.append(HISTORY_PY);
            return path.toFile();
        } else {
            return null;
        }
    }

}
