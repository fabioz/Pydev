/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.consoles.MessageConsoles;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.correctionassist.CheckAnalysisErrors;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils.MarkerInfo;

/**
 * Check lint.py for options.
 *
 * @author Fabio Zadrozny
 */
/*default*/ final class PyLintVisitor extends OnlyRemoveMarkersPyLintVisitor {

    private static Object lock = new Object();
    private IDocument document;
    private IProgressMonitor monitor;

    /*default*/ PyLintVisitor(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor monitor) {
        super(resource);
        this.document = document;
        this.monitor = monitor;
    }

    /**
     * Helper class which will start a process to collect PyLint information and process it.
     */
    private static final class PyLintAnalysis {

        private IResource resource;
        private IDocument document;
        private IPath location;

        List<PyMarkerUtils.MarkerInfo> markers = new ArrayList<PyMarkerUtils.MarkerInfo>();
        private IProgressMonitor monitor;
        private Process process;
        private Thread processWatchDoc;

        public PyLintAnalysis(IResource resource, IDocument document, IPath location,
                IProgressMonitor monitor) {
            this.resource = resource;
            this.document = document;
            this.location = location;
            this.monitor = monitor;
        }

        /**
         * Creates the pylint process and starts getting its output.
         */
        private void createPyLintProcess(IOConsoleOutputStream out)
                throws CoreException,
                MisconfigurationException, PythonNatureWithoutProjectException {
            String script = FileUtils.getFileAbsolutePath(new File(PyLintPrefPage.getPyLintLocation()));
            String target = FileUtils.getFileAbsolutePath(new File(location.toOSString()));

            // check whether lint.py module or pylint executable has been specified
            boolean isPyScript = script.endsWith(".py") || script.endsWith(".pyw");

            ArrayList<String> cmdList = new ArrayList<String>();
            // pylint executable
            if (!isPyScript) {
                cmdList.add(script);
            }
            //user args
            String userArgs = StringUtils.replaceNewLines(
                    PyLintPrefPage.getPyLintArgs(), " ");
            StringTokenizer tokenizer2 = new StringTokenizer(userArgs);
            while (tokenizer2.hasMoreTokens()) {
                String token = tokenizer2.nextToken();
                if (token.equals("--output-format=parseable")) {
                    continue;
                }
                if (token.startsWith("--msg-template=")) {
                    continue;
                }
                if (token.startsWith("--output-format=")) {
                    continue;
                }
                cmdList.add(token);
            }
            cmdList.add("--output-format=text");
            cmdList.add("--msg-template='{C}:{line:3d},{column:2d}: {msg} ({symbol})'");
            // target file to be linted
            cmdList.add(target);
            String[] args = cmdList.toArray(new String[0]);

            // run pylint in project location
            IProject project = resource.getProject();
            File workingDir = project.getLocation().toFile();

            if (isPyScript) {
                // run Python script (lint.py) with the interpreter of current project
                PythonNature nature = PythonNature.getPythonNature(project);
                if (nature == null) {
                    Throwable e = new RuntimeException("PyLint ERROR: Nature not configured for: " + project);
                    Log.log(e);
                    return;
                }
                String interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                write("PyLint: Executing command line:", out, script, args);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, script, args);

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor);
                this.process = r.o1;
            } else {
                // run executable command (pylint or pylint.bat or pylint.exe)
                write("PyLint: Executing command line:", out, (Object) args);
                SimpleRunner simpleRunner = new SimpleRunner();
                Tuple<Process, String> r = simpleRunner.run(args, workingDir, PythonNature.getPythonNature(project),
                        null);
                this.process = r.o1;
            }
            this.processWatchDoc = new Thread() {
                @Override
                public void run() {
                    //No need to synchronize as we'll waitFor() the process before getting the contents.
                    ThreadStreamReader std = new ThreadStreamReader(process.getInputStream(), false, null);
                    ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream(), false, null);

                    std.start();
                    err.start();

                    while (process.isAlive()) {
                        if (monitor.isCanceled()) {
                            std.stopGettingOutput();
                            err.stopGettingOutput();
                            return;
                        }
                        synchronized (this) {
                            try {
                                this.wait(20);
                            } catch (InterruptedException e) {
                                // Just proceed to another check.
                            }
                        }
                    }

                    if (monitor.isCanceled()) {
                        std.stopGettingOutput();
                        err.stopGettingOutput();
                        return;
                    }

                    // Wait for the other threads to finish getting the output
                    try {
                        std.join();
                    } catch (InterruptedException e) {
                    }
                    try {
                        err.join();
                    } catch (InterruptedException e) {
                    }

                    if (monitor.isCanceled()) {
                        std.stopGettingOutput();
                        err.stopGettingOutput();
                        return;
                    }

                    String output = std.getAndClearContents();
                    String errors = err.getAndClearContents();
                    afterRunProcess(output, errors, out);
                }
            };
            this.processWatchDoc.start();
        }

        public void afterRunProcess(String output, String errors, IOConsoleOutputStream out) {
            write("PyLint: The stdout of the command line is:", out, output);
            write("PyLint: The stderr of the command line is:", out, errors);

            StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");

            //Set up local values for severity
            int wSeverity = PyLintPrefPage.wSeverity();
            int eSeverity = PyLintPrefPage.eSeverity();
            int fSeverity = PyLintPrefPage.fSeverity();
            int cSeverity = PyLintPrefPage.cSeverity();
            int rSeverity = PyLintPrefPage.rSeverity();

            if (monitor.isCanceled()) {
                return;
            }

            if (output.indexOf("Traceback (most recent call last):") != -1) {
                Throwable e = new RuntimeException("PyLint ERROR: \n" + output);
                Log.log(e);
                return;
            }
            if (errors.indexOf("Traceback (most recent call last):") != -1) {
                Throwable e = new RuntimeException("PyLint ERROR: \n" + errors);
                Log.log(e);
                return;
            }
            while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                if (monitor.isCanceled()) {
                    return;
                }

                try {
                    int priority = 0;

                    int indexOfDoublePoints = tok.indexOf(":");
                    if (indexOfDoublePoints != 1) {
                        continue;
                    }
                    char c = tok.charAt(0);
                    switch (c) {
                        case 'C':
                            priority = cSeverity;
                            break;
                        case 'R':
                            priority = rSeverity;
                            break;
                        case 'W':
                            priority = wSeverity;
                            break;
                        case 'E':
                            priority = eSeverity;
                            break;
                        case 'F':
                            priority = fSeverity;
                            break;
                    }

                    if (priority > -1) { // priority == -1: ignore, 0=info, 1=warning, 2=error.
                        try {
                            int line = -1;
                            int column = -1;
                            String messageId = "";
                            Matcher m = PYLINT_MATCH_PATTERN.matcher(tok);
                            if (m.matches()) {
                                line = Integer.parseInt(tok.substring(m.start(1), m.end(1)));
                                column = Integer.parseInt(tok.substring(m.start(2), m.end(2)));
                                messageId = tok.substring(m.start(4), m.end(4)).trim();
                                tok = tok.substring(m.start(3), m.end(3)).trim();
                            } else {
                                continue;
                            }
                            IRegion region = null;
                            try {
                                region = document.getLineInformation(line - 1);
                            } catch (Exception e) {
                                region = document.getLineInformation(line);
                            }
                            String lineContents = document.get(region.getOffset(), region.getLength());

                            if (CheckAnalysisErrors.isPyLintErrorHandledAtLine(lineContents, messageId)) {
                                continue;
                            }

                            addToMarkers(tok, priority, messageId, line - 1, column, lineContents);
                        } catch (RuntimeException e2) {
                            Log.log(e2);
                        }
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }
            }
        }

        private static Pattern PYLINT_MATCH_PATTERN = Pattern
                .compile("\\A[CRWEF]:\\s*(\\d+)\\s*,\\s*(\\d+)\\s*:(.*)\\((.*)\\)\\s*\\Z");

        private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents) {
            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put(PYLINT_MESSAGE_ID, id);
            markers.add(new PyMarkerUtils.MarkerInfo(document, "PyLint: " + tok,
                    PYLINT_PROBLEM_MARKER, priority, false, false, line, column, line, lineContents.length(),
                    additionalInfo));
        }

        /**
         * Waits for the PyLint processing to finish (note that canceling the monitor should also
         * stop the analysis/kill the related process).
         */
        public void join() {
            if (processWatchDoc != null) {
                try {
                    processWatchDoc.join();
                } catch (InterruptedException e) {
                    // If interrrupted, log and got through with it.
                    Log.log(e);
                }
            }
        }
    }

    private static void write(String cmdLineToExe, IOConsoleOutputStream out, Object... args) {
        try {
            if (out != null) {
                synchronized (lock) {
                    if (args != null) {
                        for (Object arg : args) {
                            if (arg instanceof String) {
                                cmdLineToExe += " " + arg;
                            } else if (arg instanceof String[]) {
                                String[] strings = (String[]) arg;
                                for (String string : strings) {
                                    cmdLineToExe += " " + string;
                                }
                            }
                        }
                    }
                    out.write(cmdLineToExe + "\n");
                }
            }
        } catch (IOException e) {
            Log.log(e);
        }
    }

    /**
     * Helper class to monitor the cancel state of another monitor.
     */
    private static class NullProgressMonitorWrapper extends NullProgressMonitor {

        private IProgressMonitor wrap;

        public NullProgressMonitorWrapper(IProgressMonitor monitor) {
            this.wrap = monitor;
        }

        @Override
        public boolean isCanceled() {
            return super.isCanceled() || this.wrap.isCanceled();
        }

    }

    private PyLintAnalysis pyLintRunnable;

    /**
     * When we start visiting some resource, we create the process which will do the PyLint analysis.
     */
    @Override
    public void startVisit() {
        if (document == null || resource == null || PyLintPrefPage.usePyLint() == false) {
            deleteMarkers();
            return;
        }

        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        try {
            // PyLint can only be used for python projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                deleteMarkers();
                return;
            }
        } catch (Exception e) {
            deleteMarkers();
            return;
        }
        if (project != null && resource instanceof IFile) {
            IFile file = (IFile) resource;
            IPath location = file.getRawLocation();
            if (location != null) {
                pyLintRunnable = new PyLintAnalysis(resource, document, location,
                        new NullProgressMonitorWrapper(monitor));

                try {
                    IOConsoleOutputStream out = getConsoleOutputStream();
                    pyLintRunnable.createPyLintProcess(out);
                } catch (final Exception e) {
                    Log.log(e);
                }
            }
        }
    }

    @Override
    public void join() {
        if (pyLintRunnable != null) {
            pyLintRunnable.join();
        }
    }

    @Override
    public List<MarkerInfo> getMarkers() {
        if (pyLintRunnable == null) {
            return null;
        }
        return pyLintRunnable.markers;
    }

    private static IOConsoleOutputStream getConsoleOutputStream() throws MalformedURLException {
        if (PyLintPrefPage.useConsole()) {
            return MessageConsoles.getConsoleOutputStream("PyLint", UIConstants.PY_LINT_ICON);
        } else {
            return null;
        }
    }

}
