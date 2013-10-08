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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

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
                    throw new RuntimeException(org.python.pydev.shared_core.string.StringUtils.format(
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
}
