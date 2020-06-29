package com.python.pydev.analysis.pylint;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
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
    private IDocument document;
    private IPath location;

    List<PyMarkerUtils.MarkerInfo> markers = new ArrayList<PyMarkerUtils.MarkerInfo>();
    private IProgressMonitor monitor;
    private Thread processWatchDoc;
    private File pyLintLocation;

    public PyLintAnalysis(IResource resource, IDocument document, IPath location,
            IProgressMonitor monitor, File pyLintLocation) {
        this.resource = resource;
        this.document = document;
        this.location = location;
        this.monitor = monitor;
        this.pyLintLocation = pyLintLocation;
    }

    /**
     * Creates the pylint process and starts getting its output.
     */
    void createPyLintProcess(IExternalCodeAnalysisStream out)
            throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        String script = FileUtils.getFileAbsolutePath(pyLintLocation);
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
                PyLintPreferences.getPyLintArgs(), " ");
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
        Process process;
        if (isPyScript) {
            // run Python script (lint.py) with the interpreter of current project
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature == null) {
                Throwable e = new RuntimeException("PyLint ERROR: Nature not configured for: " + project);
                Log.log(e);
                return;
            }
            String interpreter = nature.getProjectInterpreter().getExecutableOrJar();
            WriteToStreamHelper.write("PyLint: Executing command line:", out, script, args);
            SimplePythonRunner runner = new SimplePythonRunner();
            String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter, script, args);

            Tuple<Process, String> r = runner.run(parameters, workingDir, nature, monitor);
            process = r.o1;
        } else {
            // run executable command (pylint or pylint.bat or pylint.exe)
            WriteToStreamHelper.write("PyLint: Executing command line:", out, (Object) args);
            SimpleRunner simpleRunner = new SimpleRunner();
            Tuple<Process, String> r = simpleRunner.run(args, workingDir, PythonNature.getPythonNature(project),
                    null);
            process = r.o1;
        }
        this.processWatchDoc = new ExternalAnalizerProcessWatchDoc(out, monitor, process, this);
        this.processWatchDoc.start();
    }

    @Override
    public void afterRunProcess(String output, String errors, IExternalCodeAnalysisStream out) {
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
        int wSeverity = PyLintPreferences.wSeverity();
        int eSeverity = PyLintPreferences.eSeverity();
        int fSeverity = PyLintPreferences.fSeverity();
        int cSeverity = PyLintPreferences.cSeverity();
        int rSeverity = PyLintPreferences.rSeverity();
        int iSeverity = PyLintPreferences.iSeverity();

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

    // Notes:
    // \A = beginning of the input
    // \Z = end of the input

    private static Pattern PYLINT_MATCH_PATTERN = Pattern
            .compile("\\A[CRWEFI]:\\s*(\\d+)\\s*,\\s*(\\d+)\\s*:((.|\\n)*)\\((.*)\\)\\s*\\Z");

    private void addToMarkers(String tok, int priority, String id, int line, int column, String lineContents) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(PyLintVisitor.PYLINT_MESSAGE_ID, id);
        markers.add(new PyMarkerUtils.MarkerInfo(document, "PyLint: " + tok,
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
}