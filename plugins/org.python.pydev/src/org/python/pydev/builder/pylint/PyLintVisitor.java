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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.consoles.MessageConsoles;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils.MarkerInfo;

/**
 * 
 * Check lint.py for options.
 * 
 * @author Fabio Zadrozny
 */
public class PyLintVisitor extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public static final String PYLINT_PROBLEM_MARKER = "org.python.pydev.pylintproblemmarker";

    public static final List<PyLintThread> pyLintThreads = new ArrayList<PyLintThread>();

    private static Object lock = new Object();

    /**
     * This class runs as a thread to get the markers, and only stops the IDE when the markers are being added.
     * 
     * @author Fabio Zadrozny
     */
    public static class PyLintThread extends Thread {

        IResource resource;
        ICallback0<IDocument> document;
        IPath location;

        List<Object[]> markers = new ArrayList<Object[]>();

        public PyLintThread(IResource resource, ICallback0<IDocument> document, IPath location) {
            setName("PyLint thread");
            this.resource = resource;
            this.document = document;
            this.location = location;
        }

        /**
         * @return
         */
        private boolean canPassPyLint() {
            if (pyLintThreads.size() < PyLintPrefPage.getMaxPyLintDelta()) {
                pyLintThreads.add(this);
                return true;
            }
            return false;
        }

        /**
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                if (canPassPyLint()) {

                    IOConsoleOutputStream out = getConsoleOutputStream();

                    final IDocument doc = document.call();
                    passPyLint(resource, out, doc);

                    new Job("Adding markers") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {

                            ArrayList<MarkerInfo> lst = new ArrayList<PyMarkerUtils.MarkerInfo>();

                            for (Iterator<Object[]> iter = markers.iterator(); iter.hasNext();) {
                                Object[] el = iter.next();

                                String tok = (String) el[0];
                                int priority = ((Integer) el[1]).intValue();
                                String id = (String) el[2];
                                int line = ((Integer) el[3]).intValue();

                                lst.add(new PyMarkerUtils.MarkerInfo(doc, "ID:" + id + " " + tok,
                                        PYLINT_PROBLEM_MARKER, priority, false, false, line, 0, line, 0, null));
                            }

                            PyMarkerUtils.replaceMarkers(lst, resource, PYLINT_PROBLEM_MARKER, true, monitor);

                            return PydevPlugin.makeStatus(Status.OK, "", null);
                        }
                    }.schedule();
                }

            } catch (final Exception e) {
                new Job("Error reporting") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        Log.log(e);
                        return PydevPlugin.makeStatus(Status.OK, "", null);
                    }
                }.schedule();
            } finally {
                try {
                    pyLintThreads.remove(this);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        private IOConsoleOutputStream getConsoleOutputStream() throws MalformedURLException {
            if (PyLintPrefPage.useConsole()) {
                return MessageConsoles.getConsoleOutputStream("PyLint", UIConstants.PY_LINT_ICON);
            } else {
                return null;
            }
        }

        /**
         * @param tok
         * @param type
         * @param priority
         * @param id
         * @param line
         */
        private void addToMarkers(String tok, int priority, String id, int line) {
            markers.add(new Object[] { tok, priority, id, line });
        }

        /**
         * @param resource
         * @param out 
         * @param doc 
         * @param document
         * @param location
         * @throws CoreException
         * @throws MisconfigurationException 
         * @throws PythonNatureWithoutProjectException 
         */
        private void passPyLint(IResource resource, IOConsoleOutputStream out, IDocument doc) throws CoreException,
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
                cmdList.add(tokenizer2.nextToken());
            }
            // target file to be linted
            cmdList.add(target);
            String[] cmdArray = cmdList.toArray(new String[0]);

            // run pylint in project location
            IProject project = resource.getProject();
            File workingDir = project.getLocation().toFile();

            Tuple<String, String> outTup;
            if (isPyScript) {
                // run Python script (lint.py) with the interpreter of current project
                PythonNature nature = PythonNature.getPythonNature(project);
                if (nature == null) {
                    Throwable e = new RuntimeException("PyLint ERROR: Nature not configured for: " + project);
                    Log.log(e);
                    return;
                }
                String interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                write("PyLint: Executing command line:", out, script, cmdArray);
                outTup = new SimplePythonRunner().runAndGetOutputFromPythonScript(
                        interpreter, script, cmdArray, workingDir, project);
            } else {
                // run executable command (pylint or pylint.bat or pylint.exe)
                write("PyLint: Executing command line:", out, (Object) cmdArray);
                outTup = new SimpleRunner().runAndGetOutput(
                        cmdArray, workingDir, PythonNature.getPythonNature(project), null, null);
            }
            String output = outTup.o1;
            String errors = outTup.o2;

            write("PyLint: The stdout of the command line is:", out, output);
            write("PyLint: The stderr of the command line is:", out, errors);

            StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");

            boolean useW = PyLintPrefPage.useWarnings();
            boolean useE = PyLintPrefPage.useErrors();
            boolean useF = PyLintPrefPage.useFatal();
            boolean useC = PyLintPrefPage.useCodingStandard();
            boolean useR = PyLintPrefPage.useRefactorTips();

            //Set up local values for severity
            int wSeverity = PyLintPrefPage.wSeverity();
            int eSeverity = PyLintPrefPage.eSeverity();
            int fSeverity = PyLintPrefPage.fSeverity();
            int cSeverity = PyLintPrefPage.cSeverity();
            int rSeverity = PyLintPrefPage.rSeverity();

            //System.out.println(output);
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

                try {
                    boolean found = false;
                    int priority = 0;

                    //W0611:  3: Unused import finalize
                    //F0001:  0: Unable to load module test.test2 (list index out of range)
                    //C0321: 25:fdfd: More than one statement on a single line
                    int indexOfDoublePoints = tok.indexOf(":");
                    if (indexOfDoublePoints != -1) {

                        if (tok.startsWith("C") && useC) {
                            found = true;
                            //priority = IMarker.SEVERITY_WARNING;
                            priority = cSeverity;
                        } else if (tok.startsWith("R") && useR) {
                            found = true;
                            //priority = IMarker.SEVERITY_WARNING;
                            priority = rSeverity;
                        } else if (tok.startsWith("W") && useW) {
                            found = true;
                            //priority = IMarker.SEVERITY_WARNING;
                            priority = wSeverity;
                        } else if (tok.startsWith("E") && useE) {
                            found = true;
                            //priority = IMarker.SEVERITY_ERROR;
                            priority = eSeverity;
                        } else if (tok.startsWith("F") && useF) {
                            found = true;
                            //priority = IMarker.SEVERITY_ERROR;
                            priority = fSeverity;
                        } else {
                            continue;
                        }

                    } else {
                        continue;
                    }

                    try {
                        if (found) {
                            int line = -1;
                            String id = "";
                            if (tok.indexOf(':') == 1) {
                                // PyLint >= 1.0 has symbolic id at end of line, enclosed in parentheses
                                Pattern p = PYLINT_MATCH_PATTERN;
                                Matcher m = p.matcher(tok);
                                if (m.matches()) {
                                    line = Integer.parseInt(tok.substring(m.start(1), m.end(1)));
                                    id = tok.substring(m.start(4), m.end(4)).trim();
                                    tok = tok.substring(m.start(3), m.end(3)).trim();
                                } else {
                                    continue;
                                }
                            } else {
                                // PyLint < 1.0 has 'Axxxx' alphanumeric id before first colon
                                id = tok.substring(0, tok.indexOf(":")).trim();

                                int i = tok.indexOf(":");
                                if (i == -1) {
                                    continue;
                                }

                                tok = tok.substring(i + 1);

                                i = tok.indexOf(":");
                                if (i == -1) {
                                    continue;
                                }

                                final String substring = tok.substring(0, i).trim();
                                //On PyLint 0.24 it started giving line,col (and not only the line).
                                line = Integer.parseInt(StringUtils.split(substring, ',').get(0));

                                i = tok.indexOf(":");
                                if (i == -1) {
                                    continue;
                                }

                                tok = tok.substring(i + 1);
                            }
                            IRegion region = null;
                            try {
                                region = doc.getLineInformation(line - 1);
                            } catch (Exception e) {
                                region = doc.getLineInformation(line);
                            }
                            String lineContents = doc.get(region.getOffset(), region.getLength());

                            int pos = -1;
                            if ((pos = lineContents.indexOf("IGNORE:")) != -1) {
                                String lintW = lineContents.substring(pos + "IGNORE:".length());
                                if (lintW.startsWith(id)) {
                                    continue;
                                }
                            }
                            addToMarkers(tok, priority, id, line - 1);
                        }
                    } catch (RuntimeException e2) {
                        Log.log(e2);
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }
            }
        }

        private static Pattern PYLINT_MATCH_PATTERN = Pattern
                .compile("\\A[CRWEF]:\\s*(\\d+)(,\\s*\\d+)?:(.*)\\((.*)\\)\\s*\\Z");

    }

    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        if (document == null) {
            return;
        }
        //Whenever PyLint is passed, the markers will be deleted.
        try {
            resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
        } catch (CoreException e3) {
            Log.log(e3);
        }
        if (PyLintPrefPage.usePyLint() == false) {
            return;
        }

        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        try {
            //pylint can only be used for jython projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                return;
            }
            //must be in a source folder (not external)
            if (!isResourceInPythonpathProjectSources(resource, pythonNature, false)) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (project != null && resource instanceof IFile) {

            IFile file = (IFile) resource;
            IPath location = file.getRawLocation();
            if (location != null) {
                PyLintThread thread = new PyLintThread(resource, document, location);
                thread.start();
            }
        }
    }

    public static void write(String cmdLineToExe, IOConsoleOutputStream out, Object... args) {
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

    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#maxResourcesToVisit()
     */
    @Override
    public int maxResourcesToVisit() {
        int i = PyLintPrefPage.getMaxPyLintDelta();
        if (i < 0) {
            i = 0;
        }
        return i;
    }
}
