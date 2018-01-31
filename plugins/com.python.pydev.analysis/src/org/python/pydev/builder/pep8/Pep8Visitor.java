/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pep8;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonModules;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.messages.Message;

/**
 * @author Fabio
 *
 */
public class Pep8Visitor {

    private static final String EXECUTE_PEP8 = "import sys\n"
            + "argv = ['pycodestyle.py', r'%s'%s]\n"
            + "sys.argv=argv\n"
            //It always accesses sys.argv[0] in process_options, so, it must be set.
            + "\n"
            + "\n"
            + "pep8style = pycodestyle.StyleGuide(parse_argv=True, config_file=False)\n"
            + "\n"
            + "checker = pycodestyle.Checker(options=pep8style.options, filename='%s', lines=lines)\n"
            + "\n"
            + "if ReportError is None: #Only redefine if it wasn't defined already\n"
            + "    class ReportError:\n"
            + "\n"
            + "        def __init__(self, checker, pep8style, visitor):\n"
            + "            self.checker = checker\n"
            + "            self.pep8style = pep8style\n"
            + "            self.visitor = visitor\n"
            + "            self.original = checker.report_error\n"
            + "            checker.report_error = self\n"
            + "            if not self.pep8style.excluded(self.checker.filename):\n"
            + "                checker.check_all()\n"
            + "            #Clear references\n"
            + "            self.original = None\n"
            + "            self.checker = None\n"
            + "            self.pep8style = None\n"
            + "            self.visitor = None\n"
            + "            checker.report_error = None\n"
            + "        \n"
            + "        def __call__(self, line_number, offset, text, check):\n"
            + "            code = text[:4]\n"
            + "            if self.pep8style.options.ignore_code(code):\n"
            + "                return\n"
            + "            self.visitor.reportError(line_number, offset, text, check)\n"
            + "            return self.original(line_number, offset, text, check)\n"
            + "\n"
            + "ReportError(checker, pep8style, visitor)\n"
            + "checker = None #Release checker\n"
            + "pep8style = None #Release pep8style\n"
            + "";

    private final List<IMessage> messages = new ArrayList<IMessage>();
    private IAnalysisPreferences prefs;
    private IDocument document;
    private volatile static PyObject reportError;
    private static final Object lock = new Object();
    private String messageToIgnore;

    public List<IMessage> getMessages(SourceModule module, IDocument document, IProgressMonitor monitor,
            IAnalysisPreferences prefs) {
        try {

            if (prefs.getSeverityForType(IAnalysisPreferences.TYPE_PEP8) < IMarker.SEVERITY_WARNING) {
                return messages;
            }
            this.prefs = prefs;
            this.document = document;
            messageToIgnore = prefs.getRequiredMessageToIgnore(IAnalysisPreferences.TYPE_PEP8);
            File pep8Loc = JythonModules.getPep8Location();

            if (pep8Loc == null) {
                Log.log("Unable to get pycodestyle module.");
                return messages;
            }

            IAdaptable projectAdaptable = prefs.getProjectAdaptable();
            if (AnalysisPreferences.useSystemInterpreter(projectAdaptable)) {
                String parameters = AnalysisPreferences.getPep8CommandLineAsStr(projectAdaptable);
                String output = runWithPep8BaseScript(document, parameters, "pycodestyle.py");
                if (output == null) {
                    output = "";
                }
                List<String> splitInLines = StringUtils.splitInLines(output, false);

                for (String line : splitInLines) {
                    try {
                        List<String> lst = StringUtils.split(line, ':', 4);
                        int lineNumber = Integer.parseInt(lst.get(1));
                        int offset = Integer.parseInt(lst.get(2)) - 1;
                        String text = lst.get(3);
                        this.reportError(lineNumber, offset, text, null);
                    } catch (Exception e) {
                        Log.log("Error parsing line: " + line, e);
                    }
                }
                return messages;
            }

            String[] pep8CommandLine = AnalysisPreferences.getPep8CommandLine(projectAdaptable);
            FastStringBuffer args = new FastStringBuffer(pep8CommandLine.length * 20);
            for (String string : pep8CommandLine) {
                args.append(',').append("r'").append(string).append('\'');
            }

            //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
            //it may be that the output ends up being shared, which is not what we want.)
            boolean useConsole = AnalysisPreferences.useConsole(projectAdaptable);
            IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(useConsole, false);
            String file = StringUtils.replaceAllSlashes(module.getFile().getAbsolutePath());
            interpreter.set("visitor", this);

            List<String> splitInLines = StringUtils.splitInLines(document.get());
            interpreter.set("lines", splitInLines);
            PyObject tempReportError = reportError;
            if (tempReportError != null) {
                interpreter.set("ReportError", tempReportError);
            } else {
                interpreter.set("ReportError", Py.None);
            }
            PyObject pep8Module = JythonModules.getPep8Module(interpreter);
            interpreter.set("pycodestyle", pep8Module);

            String formatted = StringUtils.format(EXECUTE_PEP8, file,
                    args.toString(),
                    file);
            interpreter.exec(formatted);
            if (reportError == null) {
                synchronized (lock) {
                    if (reportError == null) {
                        reportError = interpreter.get("ReportError");
                    }
                }
            }

        } catch (Exception e) {
            Log.log("Error analyzing: " + module, e);
        }

        return messages;
    }

    /**
     *
     */
    public void reportError(int lineNumber, int offset, String text, Object check) {
        int len;
        try {
            len = this.document.getLineLength(lineNumber - 1);
        } catch (BadLocationException e) {
            return; // the document changed in the meanwhile...
        }
        if (messageToIgnore != null) {
            int startLine = lineNumber - 1;
            String line = PySelection.getLine(document, startLine);
            if (CheckAnalysisErrors.isCodeAnalysisErrorHandled(line, messageToIgnore)) {
                //keep going... nothing to see here...
                return;
            }
        }

        messages.add(new Message(IAnalysisPreferences.TYPE_PEP8, text, lineNumber, lineNumber, offset + 1, len, prefs));
    }

    /**
     * @param fileContents the contents to be passed in the stdin.
     * @param parameters the parameters to pass. Note that a '-' is always added to the parameters to signal we'll pass the file as the input in stdin.
     * @param script i.e.: pycodestyle.py, autopep8.py
     * @return null if there was some error, otherwise returns the process stdout output.
     */
    public static String runWithPep8BaseScript(IDocument doc, String parameters, String script) {
        File autopep8File;
        try {
            autopep8File = CorePlugin.getScriptWithinPySrc(new Path("third_party").append("pep8")
                    .append(script).toString());
        } catch (CoreException e) {
            Log.log("Unable to get " + script + " location.");
            return null;
        }
        if (!autopep8File.exists()) {
            Log.log("Specified location for " + script + " does not exist (" + autopep8File + ").");
            return null;
        }

        SimplePythonRunner simplePythonRunner = new SimplePythonRunner();
        IInterpreterManager pythonInterpreterManager = InterpreterManagersAPI.getPythonInterpreterManager();
        IInterpreterInfo defaultInterpreterInfo;
        try {
            defaultInterpreterInfo = pythonInterpreterManager.getDefaultInterpreterInfo(false);
        } catch (MisconfigurationException e) {
            Log.log("No default Python interpreter configured to run " + script);
            return null;
        }
        String[] parseArguments = ProcessUtils.parseArguments(parameters);
        List<String> lst = new ArrayList<>(Arrays.asList(parseArguments));
        lst.add("-");

        String[] cmdarray = SimplePythonRunner.preparePythonCallParameters(
                defaultInterpreterInfo.getExecutableOrJar(), autopep8File.toString(),
                lst.toArray(new String[0]));

        // Try to find the file's encoding, but if none is given or the specified encoding is
        // unsupported, then just default to utf-8
        String pythonFileEncoding = null;
        try {
            pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            if (pythonFileEncoding == null) {
                pythonFileEncoding = "utf-8";
            }
        } catch (UnsupportedEncodingException e) {
            pythonFileEncoding = "utf-8";
        }
        final String encodingUsed = pythonFileEncoding;

        SystemPythonNature nature = new SystemPythonNature(pythonInterpreterManager, defaultInterpreterInfo);
        ICallback<String[], String[]> updateEnv = new ICallback<String[], String[]>() {

            @Override
            public String[] call(String[] arg) {
                if (arg == null) {
                    arg = new String[] { "PYTHONIOENCODING=" + encodingUsed };
                } else {
                    arg = ProcessUtils.addOrReplaceEnvVar(arg, "PYTHONIOENCODING", encodingUsed);
                }
                return arg;
            }
        };

        Tuple<Process, String> r = simplePythonRunner.run(cmdarray, autopep8File.getParentFile(), nature,
                new NullProgressMonitor(), updateEnv);
        try {
            r.o1.getOutputStream().write(doc.get().getBytes(pythonFileEncoding));
            r.o1.getOutputStream().close();
        } catch (IOException e) {
            Log.log("Error writing contents to " + script);
            return null;
        }
        Tuple<String, String> processOutput = SimplePythonRunner.getProcessOutput(r.o1, r.o2,
                new NullProgressMonitor(), pythonFileEncoding);

        if (processOutput.o2.length() > 0) {
            Log.log(processOutput.o2);
        }
        if (processOutput.o1.length() > 0) {
            return processOutput.o1;
        }
        return null;
    }

}
