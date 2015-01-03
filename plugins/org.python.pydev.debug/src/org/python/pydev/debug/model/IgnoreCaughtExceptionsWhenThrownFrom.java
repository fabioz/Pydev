/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class IgnoreCaughtExceptionsWhenThrownFrom {

    private PyExceptionBreakPointManager manager;

    /*default*/IgnoreCaughtExceptionsWhenThrownFrom(PyExceptionBreakPointManager manager) {
        this.manager = manager;
    }

    private static final String IGNORE_EXCEPTIONS_FILE_NAME = "ignore_exceptions.prefs";

    /**
     * Helper class to hold info on ignored exceptions.
     */
    public static class IgnoredExceptionInfo {

        public final String filename;
        public final int line;
        public final String contents;

        public IgnoredExceptionInfo(String s) {
            List<String> split = StringUtils.split(s, '|', 3);
            this.filename = split.get(0);
            this.line = Integer.parseInt(split.get(1));
            if (split.size() > 2) {
                this.contents = split.get(2);
            } else {
                this.contents = "";
            }
        }
    }

    /**
     * Public API to enable a forced refresh on the exceptions to be ignored.
     */
    public void updateIgnoreThrownExceptions() {
        for (IExceptionsBreakpointListener listener : this.manager.listeners.getListeners()) {
            listener.onUpdateIgnoreThrownExceptions();
        }
    }

    /**
     * @return the path with the file containing the information on the exceptions to be ignored.
     */
    public IPath getIgnoreThrownExceptionsPath() {
        return ConfigureExceptionsFileUtils.getFilePathFromMetadata(IGNORE_EXCEPTIONS_FILE_NAME);
    }

    /**
     * @return a list with the information on the ignored caught exceptions.
     */
    public Collection<IgnoredExceptionInfo> getIgnoreThrownExceptionsForEdition() {
        String metadataFile = ConfigureExceptionsFileUtils.readFromMetadataFile(IGNORE_EXCEPTIONS_FILE_NAME);
        List<String> lines = StringUtils.splitInLines(metadataFile, false);
        TreeSet<String> linesAsSet = new TreeSet<>(lines);

        List<IgnoredExceptionInfo> ret = new ArrayList<>(linesAsSet.size());
        for (String s : linesAsSet) {
            try {
                ret.add(new IgnoredExceptionInfo(s));
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return ret;
    }

    /**
     * Gets a collection of file|lineNumber with exceptions to be ignored.
     */
    public Collection<String> getIgnoreThrownExceptions() {
        Set<String> set = new TreeSet<>();
        String metadataFile = ConfigureExceptionsFileUtils.readFromMetadataFile(IGNORE_EXCEPTIONS_FILE_NAME);
        List<String> lines = StringUtils.splitInLines(metadataFile, false);
        TreeSet<String> linesAsSet = new TreeSet<>(lines);

        FastStringBuffer temp = new FastStringBuffer();
        for (Iterator<String> it = linesAsSet.iterator(); it.hasNext();) {
            String s = it.next().trim();
            if (s.length() == 0) {
                it.remove();
                continue;
            }

            if (StringUtils.count(s, '|') < 2) { //i.e.: the line itself could have more than one if the code has it...
                Log.log("Unexpected line in thrown exceptions file: " + s);
                continue;
            }
            List<String> split = StringUtils.split(s, '|');
            String file = split.get(0);
            int line;
            try {
                line = Integer.parseInt(split.get(1));
            } catch (NumberFormatException e) {
                Log.log("Unexpected line number in thrown exceptions file: " + s);
                continue;
            }

            temp.clear().append(file).append('|').append(line);
            String string = temp.toString();
            set.add(string);
        }

        //I.e.: something changed: rewrite it.
        if (linesAsSet.size() != lines.size()) {
            ConfigureExceptionsFileUtils.writeToFile(IGNORE_EXCEPTIONS_FILE_NAME, StringUtils.join("\n", linesAsSet)
                    + "\n",
                    false);
        }
        return set;
    }

    /**
     * We create a file where each line has an entry and each entry contains:
     * filename | lineNumber | trimmed line contents.
     */
    public void addIgnoreThrownExceptionIn(File file, int lineNumber) {
        boolean isAppend = false;
        IPath path = ConfigureExceptionsFileUtils.getFilePathFromMetadata(IGNORE_EXCEPTIONS_FILE_NAME);
        if (path.toFile().exists()) {
            isAppend = true;
        }
        String fileAbsolutePath = FileUtils.getFileAbsolutePath(file);
        String line;
        try {
            line = FileUtils.getLineFromFile(file, lineNumber);
        } catch (Exception e) {
            Log.log(StringUtils.format("Unable to ignore thrown exception in file: %s, line: %s", file, lineNumber), e);
            return;
        }

        FastStringBuffer buf = new FastStringBuffer(fileAbsolutePath, 20 + line.length());
        buf.append('|').append(lineNumber).append('|').append(line).append('\n');
        ConfigureExceptionsFileUtils.writeToFile(IGNORE_EXCEPTIONS_FILE_NAME, buf.toString(), isAppend);

        for (IExceptionsBreakpointListener listener : this.manager.listeners.getListeners()) {
            listener.onAddIgnoreThrownExceptionIn(file, lineNumber);
        }
    }
}
