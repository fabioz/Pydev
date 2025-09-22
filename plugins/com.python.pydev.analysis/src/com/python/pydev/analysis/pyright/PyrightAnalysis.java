/**
 * Copyright (c) 2025 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.pyright;

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
 * Helper class which will start a process to collect Pyright information and process it.
 */
/*default*/ final class PyrightAnalysis implements IExternalAnalyzer {

    private final IResource resource;
    private final IDocument fDocument;
    private final IPath location;

    private static class ModuleLineCol {

        private IFile moduleFile;
        private final int line;
        private final int col;
        private final int endLine;
        private final int endCol;

        public ModuleLineCol(IFile moduleFile, int line, int col, int endLine, int endCol) {
            super();
            this.moduleFile = moduleFile;
            this.line = line;
            this.col = col;
            this.endLine = endLine;
            this.endCol = endCol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(col, endCol, endLine, line, moduleFile);
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
            return col == other.col && endCol == other.endCol && endLine == other.endLine && line == other.line
                    && Objects.equals(moduleFile, other.moduleFile);
        }

    }

    private static class MessageInfo {
        private final FastStringBuffer message = new FastStringBuffer();
        private final int markerSeverity;
        private final String messageId;
        private final int line;
        private final int column;
        private final int endLine;
        private final int endColumn;
        private final String docLineContents;
        private final IFile moduleFile;
        private final IDocument document;

        public MessageInfo(String message, int markerSeverity, String messageId, int line, int column,
                int endLine, int endColumn, String docLineContents, IFile moduleFile, IDocument document) {
            super();
            this.message.append(message);
            this.markerSeverity = markerSeverity;
            this.messageId = messageId;
            this.line = line;
            this.column = column;
            this.endLine = endLine;
            this.endColumn = endColumn;
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
    private final File pyrightLocation;

    public PyrightAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File pyrightLocation) {
        this.resource = resource;
        this.fDocument = document;
        this.location = location;
        this.monitor = monitor;

        // May be null when we should do 'python -m pyright ...'.
        this.pyrightLocation = pyrightLocation;
    }

    /**
     * Creates the pyright process and starts getting its output.
     */
    void createPyrightProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String target = location.toOSString();

        ArrayList<String> cmdList = new ArrayList<String>();
        String userArgs = StringUtils.replaceNewLines(
                PyrightPreferences.getPyrightArgs(resource), " ");
        List<String> userArgsAsList = new ArrayList<>(Arrays.asList(ProcessUtils.parseArguments(userArgs)));

        // Ensure JSON output format
        if (!userArgsAsList.contains("--outputjson")) {
            userArgsAsList.add("--outputjson");
        }

        // run pyright in project location
        IProject project = resource.getProject();
        if (project == null || !project.isAccessible()) {
            // If the project is no longer valid, we can't do much.
            Log.log("Unable to run pyright in: " + target + ". Project not available (" + project + ").");
            return;
        }
        File workingDir = project.getLocation().toFile();

        cmdList.addAll(userArgsAsList);
        cmdList.add(target);

        IPythonNature nature = PythonNature.getPythonNature(project);

        ICallback0<Process> launchProcessCallback;
        if (pyrightLocation == null) {
            // use python -m pyright

            launchProcessCallback = () -> {
                String interpreter;
                try {
                    interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cmdList.add(0, "pyright");
                String[] args = cmdList.toArray(new String[0]);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, "-m", args);
                WriteToStreamHelper.write("MyPy: Executing command line:", out, StringUtils.join(" ", parameters));

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor, null);
                return r.o1;
            };

        } else {
            String pyrightExecutable = FileUtils.getFileAbsolutePath(pyrightLocation);
            cmdList.add(0, pyrightExecutable);

            launchProcessCallback = () -> {
                String[] args = cmdList.toArray(new String[0]);

                // run executable command (pyright or pyright.bat or pyright.exe)
                WriteToStreamHelper.write("Pyright: Executing command line:", out, (Object) args);

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
                Log.log("Expected resource to have project for PyrightAnalysis.");
                return;
            }
            if (!project.isAccessible()) {
                Log.log("Expected project to be accessible for PyrightAnalysis.");
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
            WriteToStreamHelper.write("Pyright: The stdout of the command line is:\n", out, output);
        }
        if (!errors.isEmpty()) {
            WriteToStreamHelper.write("Pyright: The stderr of the command line is:\n", out, errors);
        }

        if (output.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Pyright ERROR: \n" + output);
            Log.log(e);
            return;
        }
        if (errors.indexOf("Traceback (most recent call last):") != -1) {
            Throwable e = new RuntimeException("Pyright ERROR: \n" + errors);
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
                    "Expected pyright output to be with json format. i.e.: --outputformat=json.\nFile: %s\nOutput:\n%s",
                    resource.getFullPath(), output), e);
            return;
        }

        if (jsonValue == null) {
            Log.log("Pyright output returned null json value!");
            return;
        }
        Map<IResource, IDocument> resourceToDocCache = new HashMap<>();
        if (!jsonValue.isObject()) {
            Log.log("Expected pyright output to be a json object");
            return;
        }

        JsonObject asObject = jsonValue.asObject();
        JsonValue diagnostics = asObject.get("generalDiagnostics");
        if (diagnostics == null || !diagnostics.isArray()) {
            Log.log("Expected pyright output to have diagnostics array");
            return;
        }

        JsonArray diagnosticsArray = diagnostics.asArray();
        for (JsonValue diagnosticValue : diagnosticsArray) {
            if (monitor.isCanceled()) {
                return;
            }
            if (!diagnosticValue.isObject()) {
                Log.log("Pyright: In diagnostics output, expected value to be an object. Found: " + diagnosticValue);
                continue;
            }
            JsonObject diagnosticObject = diagnosticValue.asObject();

            // Get file path
            JsonValue file = diagnosticObject.get("file");
            if (file == null || !file.isString()) {
                Log.log("Pyright: Expected file to be a string in: " + diagnosticObject);
                continue;
            }

            // Get range information
            JsonValue range = diagnosticObject.get("range");
            if (range == null || !range.isObject()) {
                Log.log("Pyright: Expected range to be an object in: " + diagnosticObject);
                continue;
            }
            JsonObject rangeObject = range.asObject();

            // Get start position
            JsonValue start = rangeObject.get("start");
            if (start == null || !start.isObject()) {
                Log.log("Pyright: Expected start to be an object in: " + rangeObject);
                continue;
            }
            JsonObject startObject = start.asObject();

            JsonValue lineJson = startObject.get("line");
            if (lineJson == null || !lineJson.isNumber()) {
                Log.log("Pyright: Expected line to be a number. Found: " + lineJson);
                continue;
            }
            int line = lineJson.asInt();

            JsonValue columnJson = startObject.get("character");
            if (columnJson == null || !columnJson.isNumber()) {
                Log.log("Pyright: Expected character to be a number. Found: " + columnJson);
                continue;
            }
            int column = columnJson.asInt();

            // Get end position
            JsonValue end = rangeObject.get("end");
            if (end == null || !end.isObject()) {
                Log.log("Pyright: Expected end to be an object in: " + rangeObject);
                continue;
            }
            JsonObject endObject = end.asObject();

            JsonValue endLineJson = endObject.get("line");
            if (endLineJson == null || !endLineJson.isNumber()) {
                Log.log("Pyright: Expected end line to be a number. Found: " + endLineJson);
                continue;
            }
            int endLine = endLineJson.asInt();

            JsonValue endColumnJson = endObject.get("character");
            if (endColumnJson == null || !endColumnJson.isNumber()) {
                Log.log("Pyright: Expected end character to be a number. Found: " + endColumnJson);
                continue;
            }
            int endColumn = endColumnJson.asInt();

            IDocument document;
            if (resourceIsContainer) {
                try {
                    moduleFile = FindWorkspaceFiles
                            .getFileForLocation(Path.fromOSString(file.asString()), project);
                    if (moduleFile == null) {
                        Log.log("Pyright: Could not find file for path: " + file.asString());
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
                        .getFileForLocation(Path.fromOSString(file.asString()), project);
                if (fileForLocation == null) {
                    Log.log("Pyright: Could not find file for path: " + file.asString());
                    continue;
                }

                fileNameBuf.clear();
                fileNameBuf.append(file.asString()).trim().replaceAll('\\', '/');
                String fileName = fileNameBuf.toString().toLowerCase(); // Make all comparisons lower-case.
                if (loc == null && res == null) {
                    // Proceed
                } else if (loc != null && loc.contains(fileName)) {
                    // Proceed
                } else if (res != null && res.contains(fileName)) {
                    // Proceed
                } else {
                    Log.log("Pyright: Skipped file: " + fileName + " because it doesn't match the current one: " + res);
                    continue; // Bail out: it doesn't match the current file.
                }
            }

            // Get message
            JsonValue messageJson = diagnosticObject.get("message");
            if (messageJson == null || !messageJson.isString()) {
                Log.log("Pyright: expected message to be a string in: " + diagnosticObject);
                continue;
            }
            String message = messageJson.asString();

            // Get rule name (if available)
            String messageId = "pyright";
            JsonValue ruleJson = diagnosticObject.get("rule");
            if (ruleJson != null && ruleJson.isString()) {
                messageId = ruleJson.asString();
            }

            // Get severity
            int markerSeverity = IMarker.SEVERITY_WARNING; // Default to warning
            JsonValue severityJson = diagnosticObject.get("severity");
            if (severityJson != null && severityJson.isString()) {
                String severity = severityJson.asString();
                switch (severity.toLowerCase()) {
                    case "error":
                        markerSeverity = IMarker.SEVERITY_ERROR;
                        break;
                    case "warning":
                        markerSeverity = IMarker.SEVERITY_WARNING;
                        break;
                    case "information":
                        markerSeverity = IMarker.SEVERITY_INFO;
                        break;
                    default:
                        markerSeverity = IMarker.SEVERITY_WARNING;
                }
            }

            String lineContents = PySelection.getLine(document, line);
            if (CheckAnalysisErrors.isCodeAnalysisErrorHandled(lineContents, null)) {
                continue;
            }
            IRegion region = null;
            try {
                region = document.getLineInformation(line);
                if (region == null || document == null) {
                    continue;
                }
                ModuleLineCol moduleLineCol = new ModuleLineCol(moduleFile, line, column, endLine, endColumn);
                MessageInfo messageInfo = moduleLineColToMessage.get(moduleLineCol);
                if (messageInfo == null) {
                    messageInfo = new MessageInfo(message, markerSeverity, messageId, line, column,
                            endLine, endColumn, document.get(region.getOffset(), region.getLength()), moduleFile,
                            document);
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
                    messageInfo.endLine,
                    messageInfo.endColumn,
                    messageInfo.docLineContents, messageInfo.moduleFile, messageInfo.document);
        }
    }

    private void addToMarkers(String tok, int priority, String id, int line, int column, int endLine, int endColumn,
            String lineContents,
            IFile moduleFile, IDocument document) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(PyrightVisitor.PYRIGHT_MESSAGE_ID, id);
        List<MarkerInfo> list = fileToMarkers.get(moduleFile);
        if (list == null) {
            list = new ArrayList<>();
            fileToMarkers.put(moduleFile, list);
        }

        list.add(new PyMarkerUtils.MarkerInfo(document, "Pyright: " + tok,
                PyrightVisitor.PYRIGHT_PROBLEM_MARKER, priority, false, false, line, column, endLine,
                endColumn,
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
     * Waits for the Pyright processing to finish (note that canceling the monitor should also
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
