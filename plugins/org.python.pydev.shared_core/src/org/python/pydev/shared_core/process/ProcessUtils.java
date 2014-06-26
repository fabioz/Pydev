/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class ProcessUtils {
    /**
     * @param process process from where the output should be gotten
     * @param executionString string to execute (only for errors)
     * @param monitor monitor for giving progress
     * @return a tuple with the output of stdout and stderr
     */
    public static Tuple<String, String> getProcessOutput(Process process, String executionString,
            IProgressMonitor monitor, String encoding) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (process != null) {

            try {
                process.getOutputStream().close(); //we won't write to it...
            } catch (IOException e2) {
            }

            monitor.setTaskName("Reading output...");
            monitor.worked(5);
            //No need to synchronize as we'll waitFor() the process before getting the contents.
            ThreadStreamReader std = new ThreadStreamReader(process.getInputStream(), false, encoding);
            ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream(), false, encoding);

            std.start();
            err.start();

            boolean interrupted = true;
            while (interrupted) {
                interrupted = false;
                try {
                    monitor.setTaskName("Waiting for process to finish.");
                    monitor.worked(5);
                    process.waitFor(); //wait until the process completion.
                } catch (InterruptedException e1) {
                    interrupted = true;
                }
            }

            try {
                //just to see if we get something after the process finishes (and let the other threads run).
                Object sync = new Object();
                synchronized (sync) {
                    sync.wait(50);
                }
            } catch (Exception e) {
                //ignore
            }
            return new Tuple<String, String>(std.getContents(), err.getContents());

        } else {
            try {
                throw new CoreException(new Status(IStatus.ERROR, SharedCorePlugin.PLUGIN_ID,
                        "Error creating process - got null process(" + executionString + ")", new Exception(
                                "Error creating process - got null process.")));
            } catch (CoreException e) {
                Log.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return new Tuple<String, String>("", "Error creating process - got null process(" + executionString + ")"); //no output
    }

    /**
     * Passes the commands directly to Runtime.exec (with the passed envp)
     */
    public static Process createProcess(String[] cmdarray, String[] envp, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(getWithoutEmptyParams(cmdarray), getWithoutEmptyParams(envp), workingDir);
    }

    /**
     * @return a new array without any null/empty elements originally contained in the array.
     */
    private static String[] getWithoutEmptyParams(String[] cmdarray) {
        if (cmdarray == null) {
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        for (String string : cmdarray) {
            if (string != null && string.length() > 0) {
                list.add(string);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * @return a tuple with the process created and a string representation of the cmdarray.
     */
    public static Tuple<Process, String> run(String[] cmdarray, String[] envp, File workingDir, IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        String executionString = getArgumentsAsStr(cmdarray);
        monitor.setTaskName("Executing: " + executionString);
        monitor.worked(5);
        Process process = null;
        try {
            monitor.setTaskName("Making pythonpath environment..." + executionString);
            //Otherwise, use default (used when configuring the interpreter for instance).
            monitor.setTaskName("Making exec..." + executionString);
            if (workingDir != null) {
                if (!workingDir.isDirectory()) {
                    throw new RuntimeException(StringUtils.format(
                            "Working dir must be an existing directory (received: %s)", workingDir));
                }
            }
            process = createProcess(cmdarray, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Tuple<Process, String>(process, executionString);
    }

    /**
     * Runs the given command line and returns a tuple with the output (stdout and stderr) of executing it.
     *
     * @param cmdarray array with the commands to be passed to Runtime.exec
     * @param workingDir the working dir (may be null)
     * @param project the project (used to get the pythonpath and put it into the environment) -- if null, no environment is passed.
     * @param monitor the progress monitor to be used -- may be null
     *
     * @return a tuple with stdout and stderr
     */
    public static Tuple<String, String> runAndGetOutput(String[] cmdarray, String[] envp, File workingDir,
            IProgressMonitor monitor,
            String encoding) {
        Tuple<Process, String> r = run(cmdarray, envp, workingDir, monitor);

        return getProcessOutput(r.o1, r.o2, monitor, encoding);
    }

    /**
     * copied from org.eclipse.jdt.internal.launching.StandardVMRunner
     * @param args - other arguments to be added to the command line (may be null)
     * @return
     */
    public static String getArgumentsAsStr(String[] commandLine, String... args) {
        if (args != null && args.length > 0) {
            String[] newCommandLine = new String[commandLine.length + args.length];
            System.arraycopy(commandLine, 0, newCommandLine, 0, commandLine.length);
            System.arraycopy(args, 0, newCommandLine, commandLine.length, args.length);
            commandLine = newCommandLine;
        }

        if (commandLine.length < 1)
        {
            return ""; //$NON-NLS-1$
        }
        FastStringBuffer buf = new FastStringBuffer();
        FastStringBuffer command = new FastStringBuffer();
        for (int i = 0; i < commandLine.length; i++) {
            if (commandLine[i] == null) {
                continue; //ignore nulls (changed from original code)
            }

            buf.append(' ');
            char[] characters = commandLine[i].toCharArray();
            command.clear();
            boolean containsSpace = false;
            for (int j = 0; j < characters.length; j++) {
                char character = characters[j];
                if (character == '\"') {
                    command.append('\\');
                } else if (character == ' ') {
                    containsSpace = true;
                }
                command.append(character);
            }
            if (containsSpace) {
                buf.append('\"');
                buf.append(command.toString());
                buf.append('\"');
            } else {
                buf.append(command.toString());
            }
        }
        return buf.toString();
    }

    public static String getEnvironmentAsStr(String[] envp) {
        return StringUtils.join("\n", envp);
    }

    /**
     * @param env a map that will have its values formatted to xx=yy, so that it can be passed in an exec
     * @return an array with the formatted map
     */
    public static String[] getMapEnvAsArray(Map<String, String> env) {
        List<String> strings = new ArrayList<String>(env.size());
        FastStringBuffer buffer = new FastStringBuffer();
        for (Iterator<Map.Entry<String, String>> iter = env.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> entry = iter.next();
            buffer.clear().append(entry.getKey());
            buffer.append('=').append(entry.getValue());
            strings.add(buffer.toString());
        }

        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Parses the given command line into separate arguments that can be passed to
     * <code>DebugPlugin.exec(String[], File)</code>. Embedded quotes and slashes
     * are escaped.
     *
     * @param args command line arguments as a single string
     * @return individual arguments
     * @since 3.1
     *
     * Gotten from org.eclipse.debug.core.DebugPlugin
     */
    public static String[] parseArguments(String args) {
        if (args == null || args.length() == 0) {
            return new String[0];
        }

        if (PlatformUtils.isWindowsPlatform()) {
            return parseArgumentsWindows(args);
        }

        return parseArgumentsImpl(args);
    }

    /**
     * Gotten from org.eclipse.debug.core.DebugPlugin
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String[] parseArgumentsImpl(String args) {
        // man sh, see topic QUOTING
        List result = new ArrayList();

        final int DEFAULT = 0;
        final int ARG = 1;
        final int IN_DOUBLE_QUOTE = 2;
        final int IN_SINGLE_QUOTE = 3;

        int state = DEFAULT;
        StringBuffer buf = new StringBuffer();
        int len = args.length();
        for (int i = 0; i < len; i++) {
            char ch = args.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (state == DEFAULT) {
                    // skip
                    continue;
                } else if (state == ARG) {
                    state = DEFAULT;
                    result.add(buf.toString());
                    buf.setLength(0);
                    continue;
                }
            }
            switch (state) {
                case DEFAULT:
                case ARG:
                    if (ch == '"') {
                        state = IN_DOUBLE_QUOTE;
                    } else if (ch == '\'') {
                        state = IN_SINGLE_QUOTE;
                    } else if (ch == '\\' && i + 1 < len) {
                        state = ARG;
                        ch = args.charAt(++i);
                        buf.append(ch);
                    } else {
                        state = ARG;
                        buf.append(ch);
                    }
                    break;

                case IN_DOUBLE_QUOTE:
                    if (ch == '"') {
                        state = ARG;
                    } else if (ch == '\\' && i + 1 < len &&
                            (args.charAt(i + 1) == '\\' || args.charAt(i + 1) == '"')) {
                        ch = args.charAt(++i);
                        buf.append(ch);
                    } else {
                        buf.append(ch);
                    }
                    break;

                case IN_SINGLE_QUOTE:
                    if (ch == '\'') {
                        state = ARG;
                    } else {
                        buf.append(ch);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        if (buf.length() > 0 || state != DEFAULT) {
            result.add(buf.toString());
        }

        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
     * Gotten from org.eclipse.debug.core.DebugPlugin
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String[] parseArgumentsWindows(String args) {
        // see http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
        List result = new ArrayList();

        final int DEFAULT = 0;
        final int ARG = 1;
        final int IN_DOUBLE_QUOTE = 2;

        int state = DEFAULT;
        int backslashes = 0;
        StringBuffer buf = new StringBuffer();
        int len = args.length();
        for (int i = 0; i < len; i++) {
            char ch = args.charAt(i);
            if (ch == '\\') {
                backslashes++;
                continue;
            } else if (backslashes != 0) {
                if (ch == '"') {
                    for (; backslashes >= 2; backslashes -= 2) {
                        buf.append('\\');
                    }
                    if (backslashes == 1) {
                        if (state == DEFAULT) {
                            state = ARG;
                        }
                        buf.append('"');
                        backslashes = 0;
                        continue;
                    } // else fall through to switch
                } else {
                    // false alarm, treat passed backslashes literally...
                    if (state == DEFAULT) {
                        state = ARG;
                    }
                    for (; backslashes > 0; backslashes--) {
                        buf.append('\\');
                    }
                    // fall through to switch
                }
            }
            if (Character.isWhitespace(ch)) {
                if (state == DEFAULT) {
                    // skip
                    continue;
                } else if (state == ARG) {
                    state = DEFAULT;
                    result.add(buf.toString());
                    buf.setLength(0);
                    continue;
                }
            }
            switch (state) {
                case DEFAULT:
                case ARG:
                    if (ch == '"') {
                        state = IN_DOUBLE_QUOTE;
                    } else {
                        state = ARG;
                        buf.append(ch);
                    }
                    break;

                case IN_DOUBLE_QUOTE:
                    if (ch == '"') {
                        if (i + 1 < len && args.charAt(i + 1) == '"') {
                            /* Undocumented feature in Windows:
                             * Two consecutive double quotes inside a double-quoted argument are interpreted as
                             * a single double quote.
                             */
                            buf.append('"');
                            i++;
                        } else if (buf.length() == 0) {
                            // empty string on Windows platform. Account for bug in constructor of JDK's java.lang.ProcessImpl.
                            result.add("\"\""); //$NON-NLS-1$
                            state = DEFAULT;
                        } else {
                            state = ARG;
                        }
                    } else {
                        buf.append(ch);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        if (buf.length() > 0 || state != DEFAULT) {
            result.add(buf.toString());
        }

        return (String[]) result.toArray(new String[result.size()]);
    }

    public static Map<String, String> getArrayAsMapEnv(String[] mapEnvAsArray) {
        TreeMap<String, String> map = new TreeMap<>();
        int length = mapEnvAsArray.length;
        for (int i = 0; i < length; i++) {
            String s = mapEnvAsArray[i];

            int iEq = s.indexOf('=');
            if (iEq != -1) {
                map.put(s.substring(0, iEq), s.substring(iEq + 1));
            }

        }
        return map;
    }

    public static String[] addOrReplaceEnvVar(String[] mapEnvAsArray, String nameToReplace, String newVal) {
        int len = mapEnvAsArray.length;
        nameToReplace += "=";
        for (int i = 0; i < len; i++) {
            String string = mapEnvAsArray[i];
            if (string.startsWith(nameToReplace)) {
                mapEnvAsArray[i] = nameToReplace + newVal;
                return mapEnvAsArray;
            }
        }

        return StringUtils.addString(mapEnvAsArray, nameToReplace + newVal);
    }

}
