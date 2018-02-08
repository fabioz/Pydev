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
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.analysis.messages.Message;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.jython.JythonPep8Core;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.AnalysisPreferences;

/**
 * @author Fabio
 *
 */
public class Pep8Visitor {

    private final List<IMessage> messages = new ArrayList<IMessage>();
    private IAnalysisPreferences prefs;
    private IDocument document;
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
            File pep8Loc = CorePlugin.getPep8Location();

            if (pep8Loc == null) {
                Log.log("Unable to get pycodestyle module.");
                return messages;
            }

            IAdaptable projectAdaptable = prefs.getProjectAdaptable();
            if (AnalysisPreferences.useSystemInterpreter(projectAdaptable) || !JythonPep8Core.isAnalyzeCallbackSet()) {
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
            boolean useConsole = AnalysisPreferences.useConsole(projectAdaptable);
            new JythonPep8Core(module.getFile().getAbsolutePath(), document, useConsole, this,
                    pep8CommandLine).analyze();

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
