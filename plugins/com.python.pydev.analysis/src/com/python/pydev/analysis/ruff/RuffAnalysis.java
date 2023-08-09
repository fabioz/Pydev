/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ruff;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.python.pydev.ast.location.FindWorkspaceFiles;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.plugin.nature.PythonNature;
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
 * Helper class which will start a process to collect Ruff information and process it.
 */
/*default*/ final class RuffAnalysis implements IExternalAnalyzer {

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
    private final File ruffLocation;

    public RuffAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File ruffLocation) {
        this.resource = resource;
        this.fDocument = document;
        this.location = location;
        this.monitor = monitor;

        // May be null when we should do 'python -m ruff ...'.
        this.ruffLocation = ruffLocation;
    }

    /**
     * Creates the ruff process and starts getting its output.
     */
    void createRuffProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String target = location.toOSString();

        ArrayList<String> cmdList = new ArrayList<String>();
        String userArgs = StringUtils.replaceNewLines(
                RuffPreferences.getRuffArgs(resource), " ");
        List<String> userArgsAsList = new ArrayList<>(Arrays.asList(ProcessUtils.parseArguments(userArgs)));
        if (!userArgsAsList.contains("--format=json")) {
            userArgsAsList.add("--format=json");
        }
        // run ruff in project location
        IProject project = resource.getProject();
        if (project == null || !project.isAccessible()) {
            // If the project is no longer valid, we can't do much.

            Log.log("Unable to run ruff in: " + target + ". Project not available (" + project + ").");
            return;
        }
        File workingDir = project.getLocation().toFile();

        cmdList.addAll(userArgsAsList);
        cmdList.add(target);

        IPythonNature nature = PythonNature.getPythonNature(project);

        ICallback0<Process> launchProcessCallback;
        if (ruffLocation == null) {
            // use python -m ruff

            launchProcessCallback = () -> {
                String interpreter;
                try {
                    interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cmdList.add(0, "ruff");
                String[] args = cmdList.toArray(new String[0]);
                WriteToStreamHelper.write("Ruff: Executing command line:", out, "python", "-m", args);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, "-m", args);

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor, null);
                return r.o1;
            };

        } else {
            String ruffExecutable = FileUtils.getFileAbsolutePath(ruffLocation);
            cmdList.add(0, ruffExecutable);

            launchProcessCallback = () -> {
                String[] args = cmdList.toArray(new String[0]);

                // run executable command (ruff or ruff.bat or ruff.exe)
                WriteToStreamHelper.write("Ruff: Executing command line:", out, (Object) args);

                SimpleRunner simpleRunner = new SimpleRunner();
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
                Log.log("Expected resource to have project for RuffAnalysis.");
                return;
            }
            if (!project.isAccessible()) {
                Log.log("Expected project to be accessible for RuffAnalysis.");
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
            WriteToStreamHelper.write("Ruff: The stdout of the command line is:\n", out, output);
        }
        if (!errors.isEmpty()) {
            WriteToStreamHelper.write("Ruff: The stderr of the command line is:\n", out, errors);
        }

        if (output.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Ruff ERROR: \n" + output);
            Log.log(e);
            return;
        }
        if (errors.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Ruff ERROR: \n" + errors);
            Log.log(e);
            return;
        }

        FastStringBuffer fileNameBuf = new FastStringBuffer();
        String loc = this.location != null ? location.toString().toLowerCase() : null;
        String res = null;
        if (this.resource != null && resource.getFullPath() != null) {
            res = this.resource.getFullPath().toString().toLowerCase();
        }

        if (output.isEmpty()) {
            return;
        }
        JsonValue jsonValue;
        try {

            jsonValue = JsonValue.readFrom(output);
        } catch (Exception e) {
            Log.log(StringUtils.format(
                    "Expected ruff output to be with json format. i.e.: --format=json.\nFile: %s\nOutput:\n%s",
                    resource.getFullPath(), output), e);
            return;
        }

        if (jsonValue == null) {
            Log.log("Ruff output returned null json value!");
            return;
        }
        Map<IResource, IDocument> resourceToDocCache = new HashMap<>();
        if (!jsonValue.isArray()) {
            Log.log("Expected ruff output to be a json array");
            return;
        }

        JsonArray asArray = jsonValue.asArray();
        for (JsonValue problemValue : asArray) {
            if (monitor.isCanceled()) {
                return;
            }
            if (!problemValue.isObject()) {
                Log.log("Ruff: In ruff output, expected value to be an object. Found: " + problemValue);
                continue;
            }
            JsonObject problemObject = problemValue.asObject();
            JsonValue location = problemObject.get("location");
            if (location == null) {
                Log.log("Ruff: Expected to find location in ruff output. Found: " + problemValue);
                continue;
            }
            if (!location.isObject()) {
                Log.log("Ruff: Expected location to be an object in ruff output. Found: " + problemValue);
                continue;
            }
            JsonObject locationObject = location.asObject();
            JsonValue rowJson = locationObject.get("row");
            if (rowJson == null || !rowJson.isNumber()) {
                Log.log("Ruff: Expected row to be a number. Found: " + rowJson);
                continue;
            }
            int line = rowJson.asInt() - 1;
            JsonValue columnJson = locationObject.get("column");
            if (columnJson == null || !columnJson.isNumber()) {
                Log.log("Ruff: Expected column to be a number. Found: " + columnJson);
            }
            int column = columnJson.asInt() - 1;

            JsonValue filename = problemObject.get("filename");
            if (filename == null || !filename.isString()) {
                Log.log("Ruff: Expected filename to be a string. Found: " + filename);
            }

            IDocument document;
            if (resourceIsContainer) {
                try {
                    moduleFile = FindWorkspaceFiles
                            .getFileForLocation(Path.fromOSString(filename.asString()), project);
                    if (moduleFile == null) {
                        Log.log("Ruff: Could not find file for path: " + filename);
                        continue;
                    }
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
                IFile fileForLocation = FindWorkspaceFiles
                        .getFileForLocation(Path.fromOSString(filename.asString()), project);
                if (fileForLocation == null) {
                    Log.log("Ruff: Could not find file for path: " + filename);
                    continue;
                }

                fileNameBuf.clear();
                fileNameBuf.append(filename.asString()).trim().replaceAll('\\', '/');
                String fileName = fileNameBuf.toString().toLowerCase(); // Make all comparisons lower-case.
                if (loc == null && res == null) {
                    // Proceed
                } else if (loc != null && loc.contains(fileName)) {
                    // Proceed
                } else if (res != null && res.contains(fileName)) {
                    // Proceed
                } else {
                    Log.log("Ruff: Skipped file: " + fileName + " because it doesn match the current one: " + res);
                    continue; // Bail out: it doesn't match the current file.
                }
            }
            String messageId = "ruff";
            JsonValue messageJson = problemObject.get("message");
            if (messageJson == null || !messageJson.isString()) {
                Log.log("Ruff: expected message to be a string in: " + problemObject);
                continue;
            }

            JsonValue code = problemObject.get("code");
            if (code == null || !code.isString()) {
                Log.log("Ruff: expected code to be a string in: " + problemObject);
                continue;
            }
            String message = messageJson.asString() + " (code: " + code.asString() + ")";

            int markerSeverity = -1;
            switch (code.asString()) {
                case "F821": // undefined name `name`
                case "E902": // IOError
                case "E999": //SyntaxError
                    markerSeverity = IMarker.SEVERITY_ERROR;
                    break;
                default:
                    markerSeverity = IMarker.SEVERITY_WARNING;
            }

            String lineContents = PySelection.getLine(document, line);
            if (CheckAnalysisErrors.isRuffErrorHandledAtLine(lineContents, code.asString())) {
                continue;
            }
            IRegion region = null;
            try {
                region = document.getLineInformation(line);
                if (region == null || document == null) {
                    continue;
                }
                ModuleLineCol moduleLineCol = new ModuleLineCol(moduleFile, line, column);
                MessageInfo messageInfo = moduleLineColToMessage.get(moduleLineCol);
                if (messageInfo == null) {
                    messageInfo = new MessageInfo(message, markerSeverity, messageId, line, column,
                            document.get(region.getOffset(), region.getLength()), moduleFile, document);
                    moduleLineColToMessage.put(moduleLineCol, messageInfo);
                } else {
                    messageInfo.addMessageLine(message);
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

    private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents,
            IFile moduleFile, IDocument document) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(RuffVisitor.RUFF_MESSAGE_ID, id);
        List<MarkerInfo> list = fileToMarkers.get(moduleFile);
        if (list == null) {
            list = new ArrayList<>();
            fileToMarkers.put(moduleFile, list);
        }

        list.add(new PyMarkerUtils.MarkerInfo(document, "Ruff: " + tok,
                RuffVisitor.RUFF_PROBLEM_MARKER, priority, false, false, line, column, line, lineContents.length(),
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
     * Waits for the Ruff processing to finish (note that canceling the monitor should also
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