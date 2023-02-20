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
import java.util.Objects;
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
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
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

    private final IResource resource;
    private final IDocument fDocument;
    private final IPath location;

    private static class ModuleLineCol {

        private IFile moduleFile;
        private final int line;
        private final int col;

        public ModuleLineCol(IFile moduleFile, int line, int col) {
            super();
            this.moduleFile = moduleFile;
            this.line = line;
            this.col = col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(col, line, moduleFile);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ModuleLineCol other = (ModuleLineCol) obj;
            return col == other.col && line == other.line && Objects.equals(moduleFile, other.moduleFile);
        }

    }

    private static class MessageInfo {
        private final FastStringBuffer message = new FastStringBuffer();
        private final int markerSeverity;
        private final String messageId;
        private final int line;
        private final int column;
        private final String docLineContents;
        private final IFile moduleFile;
        private final IDocument document;

        public MessageInfo(String message, int markerSeverity, String messageId, int line, int column,
                String docLineContents, IFile moduleFile, IDocument document) {
            super();
            this.message.append(message);
            this.markerSeverity = markerSeverity;
            this.messageId = messageId;
            this.line = line;
            this.column = column;
            this.docLineContents = docLineContents;
            this.moduleFile = moduleFile;
            this.document = document;
        }

        public void addMessageLine(String message) {
            this.message.append('\n').append(message);
        }
    }

    private final Map<IResource, List<PyMarkerUtils.MarkerInfo>> fileToMarkers = new HashMap<>();
    private final IProgressMonitor monitor;
    private Thread processWatchDoc;
    private final File mypyLocation;

    public MypyAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File mypyLocation) {
        this.resource = resource;
        this.fDocument = document;
        this.location = location;
        this.monitor = monitor;

        // May be null when we should do 'python -m mypy ...'.
        this.mypyLocation = mypyLocation;
    }

    /**
     * Creates the mypy process and starts getting its output.
     */
    void createMypyProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String target = location.toOSString();

        ArrayList<String> cmdList = new ArrayList<String>();
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
        if (project == null || !project.isAccessible()) {
            // If the project is no longer valid, we can't do much.

            Log.log("Unable to run mypy in: " + target + ". Project not available (" + project + ").");
            return;
        }
        File workingDir = project.getLocation().toFile();
        if (!foundCacheDir) {
            // Set a cache dir if one is not given.
            userArgsAsList.add("--cache-dir=" + new File(workingDir, ".mypy_cache").toString());
        }

        cmdList.addAll(userArgsAsList);
        cmdList.add(target);

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

        final ICallback<String[], String[]> finalUpdateEnv = updateEnv;

        ICallback0<Process> launchProcessCallback;
        if (mypyLocation == null) {
            // use python -m mypy

            launchProcessCallback = () -> {
                String interpreter;
                try {
                    interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cmdList.add(0, "mypy");
                String[] args = cmdList.toArray(new String[0]);
                WriteToStreamHelper.write("MyPy: Executing command line:", out, "python", "-m", args);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, "-m", args);

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor, finalUpdateEnv);
                return r.o1;
            };

        } else {
            String mypyExecutable = FileUtils.getFileAbsolutePath(mypyLocation);
            cmdList.add(0, mypyExecutable);

            launchProcessCallback = () -> {
                String[] args = cmdList.toArray(new String[0]);

                // run executable command (mypy or mypy.bat or mypy.exe)
                WriteToStreamHelper.write("Mypy: Executing command line:", out, (Object) args);

                SimpleRunner simpleRunner = new SimpleRunner();
                final Tuple<Process, String> r = simpleRunner.run(args, workingDir, nature,
                        null, finalUpdateEnv);
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
                Log.log("Expected resource to have project for MyPyAnalysis.");
                return;
            }
            if (!project.isAccessible()) {
                Log.log("Expected project to be accessible for MyPyAnalysis.");
                return;
            }
        } else if (resource instanceof IFile) {
            moduleFile = (IFile) resource;
        } else {
            return;
        }

        output = output.trim();
        errors = errors.trim();
        Map<ModuleLineCol, MessageInfo> moduleLineColToMessage = new HashMap<>();
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

        FastStringBuffer fileNameBuf = new FastStringBuffer();
        String loc = this.location != null ? location.toString().toLowerCase() : null;
        String res = null;
        if (this.resource != null && resource.getFullPath() != null) {
            res = this.resource.getFullPath().toString().toLowerCase();
        }

        Map<IResource, IDocument> resourceToDocCache = new HashMap<>();

        for (String outputLine : StringUtils.iterLines(output)) {
            if (monitor.isCanceled()) {
                return;
            }

            try {
                outputLine = outputLine.trim();
                int column = -1;
                Matcher m = MYPY_MATCH_PATTERN1.matcher(outputLine);
                if (m.matches()) {
                    column = Integer.parseInt(outputLine.substring(m.start(3), m.end(3))) - 1;
                } else {
                    m = MYPY_MATCH_PATTERN2.matcher(outputLine);
                    if (m.matches()) {
                        column = 0;
                    } else {
                        m = null;
                    }
                }

                if (m != null) {
                    IDocument document;
                    if (resourceIsContainer) {
                        String relativeFilename = outputLine.substring(m.start(1), m.end(1));
                        try {
                            moduleFile = project.getFile(new Path(relativeFilename));
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

                    int line = -1;
                    String messageId = "";
                    String message = "";
                    int markerSeverity = -1;
                    String severityFound = outputLine.substring(m.start(4), m.end(4)).trim();

                    if (severityFound.equals("error")) {
                        markerSeverity = IMarker.SEVERITY_ERROR;
                    }
                    if (severityFound.equals("warning")) {
                        markerSeverity = IMarker.SEVERITY_WARNING;
                    }
                    if (severityFound.equals("note")) {
                        markerSeverity = IMarker.SEVERITY_INFO;
                    }
                    if (markerSeverity != -1) {
                        line = Integer.parseInt(outputLine.substring(m.start(2), m.end(2))) - 1;
                        String lineContents = PySelection.getLine(document, line);
                        if (CheckAnalysisErrors.isCodeAnalysisErrorHandled(lineContents, null)) {
                            continue;
                        }
                        messageId = "mypy";
                        message = outputLine.substring(m.start(5), m.end(5)).trim();

                        IRegion region = null;
                        try {
                            region = document.getLineInformation(line);
                        } catch (Exception e) {
                        }
                        if (region != null && document != null) {
                            ModuleLineCol moduleLineCol = new ModuleLineCol(moduleFile, line, column);
                            MessageInfo messageInfo = moduleLineColToMessage.get(moduleLineCol);
                            if (messageInfo == null) {
                                messageInfo = new MessageInfo(message, markerSeverity, messageId, line, column,
                                        document.get(region.getOffset(), region.getLength()), moduleFile, document);
                                moduleLineColToMessage.put(moduleLineCol, messageInfo);
                            } else {
                                messageInfo.addMessageLine(message);
                            }
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        for (MessageInfo messageInfo : moduleLineColToMessage.values()) {
            addToMarkers(messageInfo.message.toString(), messageInfo.markerSeverity, messageInfo.messageId,
                    messageInfo.line,
                    messageInfo.column,
                    messageInfo.docLineContents, messageInfo.moduleFile, messageInfo.document);
        }
    }

    private static Pattern MYPY_MATCH_PATTERN1 = Pattern
            .compile("\\A" // start of input
                    + "\\s*(.*)" // filename (1)
                    + "\\s*\\:\\s*(\\d+)" // line (2)
                    + "\\s*\\:\\s*(\\d+)" // col (3)
                    + "\\s*\\:\\s*(\\w+)" // error|note (4)
                    + "\\s*\\:\\s*(.*)" // message (5)
                    + "\\Z" // end of input
            );

    private static Pattern MYPY_MATCH_PATTERN2 = Pattern // get the same group organization, but (3) is empty
            .compile("\\A" // start of input
                    + "\\s*(.*)" // filename (1)
                    + "\\s*\\:\\s*(\\d+)" // line (2)
                    + "()" // (3)
                    + "\\s*\\:\\s*(\\w+)" // error|note (4)
                    + "\\s*\\:\\s*(.*)" // message (5)
                    + "\\Z" // end of input
            );

    private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents,
            IFile moduleFile, IDocument document) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(MypyVisitor.MYPY_MESSAGE_ID, id);
        List<MarkerInfo> list = fileToMarkers.get(moduleFile);
        if (list == null) {
            list = new ArrayList<>();
            fileToMarkers.put(moduleFile, list);
        }

        list.add(new PyMarkerUtils.MarkerInfo(document, "Mypy: " + tok,
                MypyVisitor.MYPY_PROBLEM_MARKER, priority, false, false, line, column, line, lineContents.length(),
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