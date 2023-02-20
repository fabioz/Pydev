package com.python.pydev.analysis.pylint;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.external.ExternalAnalizerProcessWatchDoc;
import com.python.pydev.analysis.external.IExternalAnalyzer;
import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;
import com.python.pydev.analysis.external.WriteToStreamHelper;

/**
 * Helper class which will start a process to collect PyLint information and process it.
 */
/*default*/ final class PyLintAnalysis implements IExternalAnalyzer {

    private IResource resource;
    private IDocument fDocument;
    private IPath location;

    private final Map<IResource, List<PyMarkerUtils.MarkerInfo>> fileToMarkers = new HashMap<>();

    private IProgressMonitor monitor;
    private Thread processWatchDoc;
    private File pyLintLocation;

    public PyLintAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File pyLintLocation) {
        this.resource = resource;
        this.fDocument = document;
        this.location = location;
        this.monitor = monitor;

        // If null we must execute with 'python -m pylint ...'
        this.pyLintLocation = pyLintLocation;
    }

    /**
     * Creates the pylint process and starts getting its output.
     */
    void createPyLintProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String target = FileUtils.getFileAbsolutePath(new File(location.toOSString()));

        ArrayList<String> cmdList = new ArrayList<String>();
        //user args
        String userArgs = StringUtils.replaceNewLines(
                PyLintPreferences.getPyLintArgs(resource), " ");
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
        cmdList.add("--msg-template='{C}:{line:3d},{column:2d}: ({symbol}) {msg}'");
        // target file to be linted
        cmdList.add(target);

        // run pylint in project location
        IProject project = resource.getProject();
        File workingDir = project.getLocation().toFile();
        ICallback0<Process> launchProcessCallback;

        if (pyLintLocation == null) {
            // run python -m pylint with the interpreter of current project
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature == null) {
                Throwable e = new RuntimeException("PyLint ERROR: Nature not configured for: " + project);
                Log.log(e);
                return;
            }
            launchProcessCallback = () -> {
                String interpreter;
                try {
                    interpreter = nature.getProjectInterpreter().getExecutableOrJar();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cmdList.add(0, "pylint");
                String[] args = cmdList.toArray(new String[0]);
                WriteToStreamHelper.write("PyLint: Executing command line:", out, "python", "-m", args);
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, "-m", args);

                Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor);
                return r.o1;
            };
        } else {
            String script = FileUtils.getFileAbsolutePath(pyLintLocation);
            cmdList.add(0, script);
            String[] args = cmdList.toArray(new String[0]);

            // run executable command (pylint or pylint.bat or pylint.exe)
            launchProcessCallback = () -> {
                WriteToStreamHelper.write("PyLint: Executing command line:", out, (Object) args);
                SimpleRunner simpleRunner = new SimpleRunner();
                Tuple<Process, String> r = simpleRunner.run(args, workingDir, PythonNature.getPythonNature(project),
                        null);
                return r.o1;
            };
        }
        this.processWatchDoc = new ExternalAnalizerProcessWatchDoc(out, monitor, this, launchProcessCallback, null,
                false);
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
                Log.log("Expected resource to have project for PyLintAnalysis.");
                return;
            }
            if (!project.isAccessible()) {
                Log.log("Expected project to be accessible for PyLintAnalysis.");
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
            WriteToStreamHelper.write("PyLint: The stdout of the command line is:\n", out, output);
        }
        if (!errors.isEmpty()) {
            WriteToStreamHelper.write("PyLint: The stderr of the command line is:\n", out, errors);
        }

        StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");

        //Set up local values for severity
        int wSeverity = PyLintPreferences.wSeverity(resource);
        int eSeverity = PyLintPreferences.eSeverity(resource);
        int fSeverity = PyLintPreferences.fSeverity(resource);
        int cSeverity = PyLintPreferences.cSeverity(resource);
        int rSeverity = PyLintPreferences.rSeverity(resource);
        int iSeverity = PyLintPreferences.iSeverity(resource);

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

        FastStringBuffer moduleNameBuf = new FastStringBuffer();
        IDocument document = fDocument;
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            if (monitor.isCanceled()) {
                return;
            }

            if (resourceIsContainer) {
                if (tok.startsWith("*")) {
                    moduleNameBuf.clear().append(tok);
                    int i = tok.lastIndexOf("* Module ");
                    if (i >= 0) {
                        moduleNameBuf.deleteFirstChars(i + "* Module ".length());
                    } else {
                        continue;
                    }
                    moduleNameBuf.replaceAll('.', '/').append(".py");
                    Path modulePath = new Path(moduleNameBuf.toString());
                    try {
                        moduleFile = project.getFile(modulePath);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                    document = FileUtils.getDocFromResource(moduleFile);
                    continue;
                }

                if (document == null) {
                    continue;
                }
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
                    case 'I':
                        priority = iSeverity;
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
                            messageId = tok.substring(m.start(3), m.end(3)).trim();
                            tok = tok.substring(m.start(4), m.end(4)).trim();
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

                        addToMarkers(tok, priority, messageId, line - 1, column, lineContents, moduleFile, document);
                    } catch (RuntimeException e2) {
                        Log.log(e2);
                    }
                }
            } catch (Exception e1) {
                Log.log(e1);
            }
        }
    }

    // Notes:
    // \A = beginning of the input
    // \Z = end of the input

    private static Pattern PYLINT_MATCH_PATTERN = Pattern
            .compile("\\A[CRWEFI]:\\s*(\\d+)\\s*,\\s*(\\d+)\\s*:\\s*\\((.*?)\\)(.*)\\Z");

    private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents,
            IFile moduleFile, IDocument document) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(PyLintVisitor.PYLINT_MESSAGE_ID, id);
        List<MarkerInfo> list = fileToMarkers.get(moduleFile);
        if (list == null) {
            list = new ArrayList<>();
            fileToMarkers.put(moduleFile, list);
        }
        list.add(new PyMarkerUtils.MarkerInfo(document, "PyLint: " + tok,
                PyLintVisitor.PYLINT_PROBLEM_MARKER, priority, false, false, line, column, line, lineContents.length(),
                additionalInfo));
    }

    /**
     * Waits for the PyLint processing to finish (note that canceling the monitor should also
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

    public List<MarkerInfo> getMarkers(IResource resource) {
        List<MarkerInfo> ret = fileToMarkers.get(resource);
        if (ret == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ret); // Return a copy
    }

}