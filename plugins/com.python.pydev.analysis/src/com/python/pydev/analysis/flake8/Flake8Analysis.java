/**
 * Copyright (c) 2021 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.flake8;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.external.ExternalAnalizerProcessWatchDoc;
import com.python.pydev.analysis.external.IExternalAnalyzer;
import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;
import com.python.pydev.analysis.external.WriteToStreamHelper;

/**
 * Helper class which will start a process to collect Flake8 information and process it.
 */
/*default*/ final class Flake8Analysis implements IExternalAnalyzer {

    private final IResource resource;
    private final IDocument fDocument;
    private final IPath location;

    private final Map<IResource, List<PyMarkerUtils.MarkerInfo>> fileToMarkers = new HashMap<>();
    private final IProgressMonitor monitor;
    private Thread processWatchDoc;
    private final File flake8Location;

    public Flake8Analysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File flake8Location) {
        this.resource = resource;
        this.fDocument = document;
        this.location = location;
        this.monitor = monitor;

        // When null we do: python -m flake8 ...
        this.flake8Location = flake8Location;
    }

    /**
     * Creates the flake8 process and starts getting its output.
     */
    void createFlake8Process(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String target = location.toOSString();

        ArrayList<String> cmdList = new ArrayList<String>();
        String userArgs = StringUtils.replaceNewLines(
                Flake8Preferences.getFlake8Args(resource), " ");
        List<String> userArgsAsList = new ArrayList<>(Arrays.asList(ProcessUtils.parseArguments(userArgs)));
        // run flake8 in project location
        IProject project = resource.getProject();
        if (project == null || !project.isAccessible()) {
            // If the project is no longer valid, we can't do much.
            Log.log("Unable to run flake8 in: " + target + ". Project not available (" + project + ").");
            return;
        }
        File workingDir = project.getLocation().toFile();
        for (String s : userArgsAsList) {
            if (s.startsWith("--format=")) {
                continue; // ignore that as we'll add the '--format' as needed ourselves.
            }
            cmdList.add(s);
        }
        cmdList.add("--format=default");
        cmdList.add(target);

        IPythonNature nature = PythonNature.getPythonNature(project);
        ICallback0<Process> launchProcessCallback;
        if (flake8Location == null) {
            // use python -m flake8
            launchProcessCallback = () -> {
                String interpreter;
                try {
                    interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cmdList.add(0, "flake8");
                String[] args = cmdList.toArray(new String[0]);
                WriteToStreamHelper.write("Flake8: Executing command line:", out, "python", "-m", args);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, "-m", args);

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor);
                return r.o1;
            };

        } else {
            launchProcessCallback = () -> {
                SimpleRunner simpleRunner = new SimpleRunner();

                String flake8Executable = FileUtils.getFileAbsolutePath(flake8Location);
                cmdList.add(flake8Executable);

                String[] args = cmdList.toArray(new String[0]);

                // run executable command (flake8 or flake8.bat or flake8.exe)
                WriteToStreamHelper.write("Flake8: Executing command line:", out, (Object) args);

                final Tuple<Process, String> r = simpleRunner.run(args, workingDir, nature,
                        null, null);
                Process process = r.o1;
                return process;
            };
        }
        this.processWatchDoc = new ExternalAnalizerProcessWatchDoc(out, monitor, this, launchProcessCallback, project,
                true);
        this.processWatchDoc.start();
    }

    @Override
    public void afterRunProcess(String output, String errors, IExternalCodeAnalysisStream out) {
        boolean resourceIsContainer = resource instanceof IContainer;
        IProject project = null;
        IFile moduleFile = null;
        if (resourceIsContainer) {
            project = resource.getProject();
            if (project == null) {
                Log.log("Expected resource to have project for Flake8Analysis.");
                return;
            }
            if (!project.isAccessible()) {
                Log.log("Expected project to be accessible for Flake8Analysis.");
                return;
            }
        } else if (resource instanceof IFile) {
            moduleFile = (IFile) resource;
        } else {
            return;
        }

        output = output.trim();
        errors = errors.trim();
        if (!output.isEmpty()) {
            WriteToStreamHelper.write("Flake8: The stdout of the command line is:\n", out, output);
        }
        if (!errors.isEmpty()) {
            WriteToStreamHelper.write("Flake8: The stderr of the command line is:\n", out, errors);
        }

        if (output.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Flake8 ERROR: \n" + output);
            Log.log(e);
            return;
        }
        if (errors.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Flake8 ERROR: \n" + errors);
            Log.log(e);
            return;
        }

        FastStringBuffer fileNameBuf = new FastStringBuffer();
        String loc = this.location != null ? location.toString().toLowerCase() : null;
        String res = null;
        if (this.resource != null && resource.getFullPath() != null) {
            res = this.resource.getFullPath().toString().toLowerCase();
        }

        Map<IResource, IDocument> resourceToDocCache = new HashMap<>();

        Map<String, Tuple<Set<Tuple<Integer, Integer>>, Integer>> codeSeverities = null;
        if (!SharedCorePlugin.inTestMode()) {
            codeSeverities = Flake8CodesConfigHandler.getCodeSeveritiesFromConfig(resource);
        }

        for (String outputLine : StringUtils.iterLines(output)) {
            if (monitor.isCanceled()) {
                return;
            }
            try {
                outputLine = outputLine.trim();
                Matcher m = FLAKE8_MATCH_PATTERN.matcher(outputLine);
                if (m.matches()) {
                    IDocument document;
                    if (resourceIsContainer) {
                        IPath filePath = new Path(outputLine.substring(m.start(1), m.end(1)));
                        filePath = filePath.makeRelativeTo(project.getLocation());
                        try {
                            moduleFile = project.getFile(filePath);
                        } catch (Exception e) {
                            Log.log(e);
                            continue;
                        }
                        document = resourceToDocCache.get(moduleFile);
                        if (document == null) {
                            document = FileUtils.getDocFromResource(moduleFile);
                            if (document == null) {
                                continue;
                            }
                            resourceToDocCache.put(moduleFile, document);
                        }
                    } else {
                        document = fDocument;
                        // Must match the current file
                        fileNameBuf.clear();
                        fileNameBuf.append(outputLine.substring(m.start(1), m.end(1))).trim().replaceAll('\\', '/');
                        if (fileNameBuf.startsWith("./")) {
                            // Just in case the flake8 executable is in the same folder that is being analyzed
                            fileNameBuf.deleteFirstChars(2);
                        }
                        String fileName = fileNameBuf.toString().toLowerCase(); // Make all comparisons lower-case.
                        if (loc == null && res == null) {
                            // Proceed
                        } else if (loc != null && loc.contains(fileName)) {
                            // Proceed
                        } else if (res != null && res.contains(fileName)) {
                            // Proceed
                        } else {
                            continue; // Bail out: it doesn't match the current file.
                        }
                    }
                    String code = outputLine.substring(m.start(4), m.end(4)).trim();

                    Tuple<String, Tuple<Integer, Integer>> codeTup = Flake8CodesConfigHandler.getCodeTuple(code);
                    String prefix = codeTup.o1;
                    Tuple<Integer, Integer> range = codeTup.o2;

                    int priority = getPriorityFromCodeSeverityMap(prefix, range, codeSeverities);

                    if (priority > -1) {
                        int line = Integer.parseInt(outputLine.substring(m.start(2), m.end(2)));
                        int column = Integer.parseInt(outputLine.substring(m.start(3), m.end(3))) - 1;
                        String message = outputLine.substring(m.start(5), m.end(5));

                        IRegion region = null;
                        try {
                            region = document.getLineInformation(line - 1);
                        } catch (Exception e) {
                            region = document.getLineInformation(line);
                        }
                        String lineContents = document.get(region.getOffset(), region.getLength());

                        if (CheckAnalysisErrors.isFlake8ErrorHandledAtLine(lineContents, code)) {
                            continue;
                        }

                        addToMarkers(message + " (" + code + ")", priority, code, line - 1, column, lineContents,
                                moduleFile, document);
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    private int getPriorityFromCodeSeverityMap(String prefix, Tuple<Integer, Integer> range,
            Map<String, Tuple<Set<Tuple<Integer, Integer>>, Integer>> codeSeverities) {
        if (codeSeverities != null) {
            Tuple<Set<Tuple<Integer, Integer>>, Integer> valueTup = codeSeverities.get(prefix);
            if (valueTup != null) {
                Set<Tuple<Integer, Integer>> set = valueTup.o1;
                int severity = valueTup.o2;
                for (Tuple<Integer, Integer> baseRange : set) {
                    boolean overlappedRanges = Flake8CodesConfigHandler.checkRangeOverlap(range, baseRange);
                    if (overlappedRanges) {
                        return severity;
                    }
                }
            }
        }
        // default
        return IMarker.SEVERITY_WARNING;
    }

    private static Pattern FLAKE8_MATCH_PATTERN = Pattern
            .compile("\\A" // start of input
                    + "\\s*(.*)" // filename (1)
                    + "\\s*\\:\\s*(\\d+)" // line (2)
                    + "\\s*\\:\\s*(\\d+)" // col (3)
                    + "\\s*\\:\\s*(\\w+\\d+\\s(?:\\(.*\\))?)" // error code (4)
                    + "\\s*(.*)" // message (5)
                    + "\\Z" // end of input
            );

    private void addToMarkers(String message, int priority, String id, int line, int column, String lineContents,
            IFile moduleFile, IDocument document) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(Flake8Visitor.FLAKE8_MESSAGE_ID, id);
        List<MarkerInfo> list = fileToMarkers.get(moduleFile);
        if (list == null) {
            list = new ArrayList<>();
            fileToMarkers.put(moduleFile, list);
        }
        int colStart = column;
        int colEnd = lineContents.length();
        if (colStart == colEnd) {
            colStart = 0;
        }
        list.add(new PyMarkerUtils.MarkerInfo(document, "Flake8: " + message,
                Flake8Visitor.FLAKE8_PROBLEM_MARKER, priority, false, false, line, colStart, line, colEnd,
                additionalInfo));
    }

    public List<MarkerInfo> getMarkers(IResource resource) {
        List<MarkerInfo> ret = fileToMarkers.get(resource);
        if (ret == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ret); // Return a copy
    }

    /**
     * Waits for the Flake8 processing to finish (note that canceling the monitor should also
     * stop the analysis/kill the related process).
     */
    @Override
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