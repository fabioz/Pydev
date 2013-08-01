/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.log.Log;

/**
 * Handles the history so that the user can do Ctrl+up / Ctrl+down
 */
public class ScriptConsoleHistory {

    /**
     * Holds the history in an easy way to handle it.
     */
    private final List<String> lines;

    /**
     * Index of the starting point of local history
     */
    private int localHistoryStart;

    /**
     * Holds the position of the current line in the history.
     */
    private int currLine;

    /**
     * Holds the history as a document (for requests such as the indent)
     */
    private Document historyAsDoc;

    /**
     * When getting previous or next, this string must be matched
     */
    private String matchStart = "";

    /**
     * Set to true once history has been closed
     */
    private volatile boolean closed = false;

    public ScriptConsoleHistory() {
        this.lines = ScriptConsoleGlobalHistory.INSTANCE.get();
        StringBuilder globalHistory = new StringBuilder();
        for (String line : this.lines) {
            globalHistory.append(line);
            globalHistory.append("\n");
        }

        if (this.lines.size() == 0 || this.lines.get(this.lines.size() - 1).length() != 0) {
            this.lines.add(""); //$NON-NLS-1$
        }

        localHistoryStart = this.lines.size() - 1;
        this.currLine = this.lines.size() - 1;

        this.historyAsDoc = new Document(globalHistory.toString());
    }

    /**
     * Updates the current line in the buffer for the history (but it can still be changed later)
     * 
     * @param line contents to be added to the top of the command history.
     */
    public void update(String line) {
        lines.set(lines.size() - 1, line);
    }

    /**
     * Commits the currently added line (last called in update) to the history and keeps it there.
     */
    public void commit() {
        String lineToAddToHistory = getBufferLine();
        try {
            historyAsDoc.replace(historyAsDoc.getLength(), 0, lineToAddToHistory + "\n");
        } catch (BadLocationException e) {
            Log.log(e);
        }

        if (lineToAddToHistory.length() == 0) {
            currLine = lines.size() - 1;
            return;
        }

        lines.set(lines.size() - 1, lineToAddToHistory);

        lines.add(""); //$NON-NLS-1$
        currLine = lines.size() - 1;
    }

    /**
     * @return true if we've been able to go to a previous line (and false if there's no previous command in the history).
     */
    public boolean prev() {
        int initialCurrLine = currLine;
        while (true) {
            if (currLine <= 0) {
                break;
            }
            --currLine;
            String curr = get();
            if (curr.startsWith(this.matchStart)) {
                return true;
            }
        }
        currLine = initialCurrLine; //don't change if we weren't able to find a match.
        return false;
    }

    /**
     * @return true if we've been able to go to a next line (and false if there's no next command in the history).
     */
    public boolean next() {
        int initialCurrLine = currLine;
        while (true) {
            if (currLine >= lines.size() - 2) { //we don't want to add the 'current' line here
                break;
            }
            ++currLine;
            String curr = get();
            if (curr.startsWith(this.matchStart)) {
                return true;
            }
        }
        currLine = initialCurrLine; //don't change if we weren't able to find a match.

        return false;
    }

    /**
     * @return the document with the contents of this history. Should not be changed externally.
     */
    public IDocument getAsDoc() {
        return historyAsDoc;
    }

    /**
     * @return the contents of the line that's currently in the buffer but still wasn't added to the history.
     */
    public String getBufferLine() {
        return lines.get(lines.size() - 1);
    }

    /**
     * @return the contents of the current line in the history.
     */
    public String get() {
        if (lines.isEmpty()) {
            return "";
        }

        return lines.get(currLine);
    }

    /**
     * @return all the elements from the command history (except the one currently in the buffer).
     */
    public List<String> getAsList() {
        ArrayList<String> list = new ArrayList<String>(lines);
        if (list.size() > 0) {
            list.remove(list.size() - 1); //remove the last on (current)
        }
        return list;
    }

    public void setMatchStart(String string) {
        this.matchStart = string;
    }

    /**
     * Close the current history, appending to the global history any new commands
     */
    public synchronized void close() {
        // synchronized because we can be closed twice from different threads, so we
        // have protection around the read/update/write of INTERACTIVE_CONSOLE_PERSISTENT_HISTORY
        if (closed) {
            return;
        }
        closed = true;

        ScriptConsoleGlobalHistory.INSTANCE.append(lines.subList(localHistoryStart, lines.size() - 1));
    }

    /**
     * Delete the local and current global history.
     */
    public void clear() {
        ScriptConsoleGlobalHistory.INSTANCE.clear();
        lines.clear();
        lines.add("");
        localHistoryStart = 0;
        historyAsDoc.set("");
        currLine = 0;
    }
}
