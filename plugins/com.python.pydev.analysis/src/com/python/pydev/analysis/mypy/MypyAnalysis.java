/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

import com.python.pydev.analysis.external.ExternalAnalizerProcessWatchDoc;
import com.python.pydev.analysis.external.IExternalAnalyzer;
import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;
import com.python.pydev.analysis.external.WriteToStreamHelper;

/**
 * Helper class which will start a process to collect Mypy information and process it.
 */
/*default*/ final class MypyAnalysis implements IExternalAnalyzer {

    private IResource resource;
    private IDocument document;
    private IPath location;

    List<PyMarkerUtils.MarkerInfo> markers = new ArrayList<PyMarkerUtils.MarkerInfo>();
    private IProgressMonitor monitor;
    private Thread processWatchDoc;
    private File mypyLocation;

    public MypyAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File mypyLocation) {
        this.resource = resource;
        this.document = document;
        this.location = location;
        this.monitor = monitor;
        this.mypyLocation = mypyLocation;
    }

    /**
     * Creates the mypy process and starts getting its output.
     */
    void createMypyProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String mypyExecutable = FileUtils.getFileAbsolutePath(mypyLocation);
        String target = location.toOSString();

        ArrayList<String> cmdList = new ArrayList<String>();
        cmdList.add(mypyExecutable);
        String userArgs = StringUtils.replaceNewLines(
                MypyPreferences.getMypyArgs(resource), " ");
        List<String> userArgsAsList = new ArrayList<>(Arrays.asList(ProcessUtils.parseArguments(userArgs)));
        if (!userArgsAsList.contains("--show-column-numbers")) {
            userArgsAsList.add("--show-column-numbers");
        }
        boolean foundFollowImports = false;
        boolean foundCacheDir = false;
        for (String arg : userArgsAsList) {
            if (arg.startsWith("--follow-imports=silent")) {
                foundFollowImports = true;
            }
            if (arg.startsWith("--cache-dir")) {
                foundCacheDir = true;
            }
        }
        if (!foundFollowImports) {
            // We just want warnings for the current file.
            userArgsAsList.add("--follow-imports=silent");
        }
        // run mypy in project location
        IProject project = resource.getProject();
        File workingDir = project.getLocation().toFile();
        if (!foundCacheDir) {
            // Set a cache dir if one is not given.
            userArgsAsList.add("--cache-dir=" + new File(workingDir, ".mypy_cache").toString());
        }

        cmdList.addAll(userArgsAsList);
        cmdList.add(target);
        String[] args = cmdList.toArray(new String[0]);

        Process process;

        // run executable command (mypy or mypy.bat or mypy.exe)
        WriteToStreamHelper.write("Mypy: Executing command line:", out, (Object) args);
        SimpleRunner simpleRunner = new SimpleRunner();

        IPythonNature nature = PythonNature.getPythonNature(project);
        ICallback<String[], String[]> updateEnv = null;

        if (MypyPreferences.getAddProjectFoldersToMyPyPath(resource)) {
            Collection<String> addToMypyPath = new HashSet<String>();
            IModulesManager[] managersInvolved = nature.getAstManager().getModulesManager().getManagersInvolved(false);
            for (IModulesManager iModulesManager : managersInvolved) {
                for (String s : StringUtils
                        .split(iModulesManager.getNature().getPythonPathNature().getOnlyProjectPythonPathStr(true),
                                "|")) {
                    if (!s.isEmpty()) {
                        addToMypyPath.add(s);
                    }
                }
            }
            if (addToMypyPath.size() > 0) {
                updateEnv = new ICallback<String[], String[]>() {

                    @Override
                    public String[] call(String[] arg) {
                        for (int i = 0; i < arg.length; i++) {
                            // Update var
                            if (arg[i].startsWith("MYPYPATH=")) {
                                arg[i] = arg[i] + SimpleRunner.getPythonPathSeparator()
                                        + StringUtils.join(SimpleRunner.getPythonPathSeparator(), addToMypyPath);
                                return arg;
                            }
                        }

                        // Create new var.
                        return ArrayUtils.concatArrays(arg, new String[] {
                                "MYPYPATH=" + StringUtils.join(SimpleRunner.getPythonPathSeparator(), addToMypyPath) });
                    }
                };
            }
        }
        Tuple<Process, String> r = simpleRunner.run(args, workingDir, nature,
                null, updateEnv);
        process = r.o1;
        this.processWatchDoc = new ExternalAnalizerProcessWatchDoc(out, monitor, process, this);
        this.processWatchDoc.start();
    }

    @Override
    public void afterRunProcess(String output, String errors, IExternalCodeAnalysisStream out) {
        output = output.trim();
        errors = errors.trim();
        if (!output.isEmpty()) {
            WriteToStreamHelper.write("Mypy: The stdout of the command line is:\n", out, output);
        }
        if (!errors.isEmpty()) {
            WriteToStreamHelper.write("Mypy: The stderr of the command line is:\n", out, errors);
        }

        if (output.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Mypy ERROR: \n" + output);
            Log.log(e);
            return;
        }
        if (errors.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Mypy ERROR: \n" + errors);
            Log.log(e);
            return;
        }

        for (String outputLine : StringUtils.iterLines(output)) {
            if (monitor.isCanceled()) {
                return;
            }
            try {
                outputLine = outputLine.trim();
                int line = -1;
                int column = -1;
                String messageId = "";
                Matcher m = MYPY_MATCH_PATTERN.matcher(outputLine);
                String message = "";
                int markerSeverity = -1;
                if (m.matches()) {
                    String severityFound = outputLine.substring(m.start(4), m.end(4)).trim();

                    if (severityFound.equals("error")) {
                        markerSeverity = IMarker.SEVERITY_ERROR;
                    }
                    if (severityFound.equals("warning")) {
                        markerSeverity = IMarker.SEVERITY_WARNING;
                    }
                    if (markerSeverity != -1) {
                        line = Integer.parseInt(outputLine.substring(m.start(2), m.end(2)));
                        column = Integer.parseInt(outputLine.substring(m.start(3), m.end(3))) - 1;
                        messageId = "mypy";
                        message = outputLine.substring(m.start(5), m.end(5)).trim();
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                IRegion region = null;
                try {
                    region = document.getLineInformation(line - 1);
                } catch (Exception e) {
                }
                if (region != null && document != null) {
                    String docLineContents = document.get(region.getOffset(), region.getLength());
                    addToMarkers(message, markerSeverity, messageId, line - 1, column, docLineContents);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    private static Pattern MYPY_MATCH_PATTERN = Pattern
            .compile("\\A" // start of input
                    + "\\s*(.*)" // filename (1)
                    + "\\s*\\:\\s*(\\d+)" // line (2)
                    + "\\s*\\:\\s*(\\d+)" // col (3)
                    + "\\s*\\:\\s*(\\w+)" // error|note (4)
                    + "\\s*\\:\\s*(.*)" // message (5)
                    + "\\Z" // end of input
            );

    private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(MypyVisitor.MYPY_MESSAGE_ID, id);
        markers.add(new PyMarkerUtils.MarkerInfo(document, "Mypy: " + tok,
                MypyVisitor.MYPY_PROBLEM_MARKER, priority, false, false, line, column, line, lineContents.length(),
                additionalInfo));
    }

    /**
     * Waits for the Mypy processing to finish (note that canceling the monitor should also
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