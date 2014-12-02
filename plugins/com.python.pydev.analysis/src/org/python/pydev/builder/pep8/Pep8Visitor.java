/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pep8;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.plugin.JythonModules;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.messages.Message;
import com.python.pydev.analysis.ui.AnalysisPreferencesPage;

/**
 * @author Fabio
 *
 */
public class Pep8Visitor {

    private static final String EXECUTE_PEP8 = "import sys\n"
            + "argv = ['pep8.py', r'%s'%s]\n"
            + "sys.argv=argv\n"
            //It always accesses sys.argv[0] in process_options, so, it must be set.
            + "\n"
            + "\n"
            + "pep8style = pep8.StyleGuide(parse_argv=True, config_file=False)\n"
            + "\n"
            + "checker = pep8.Checker(options=pep8style.options, filename='%s', lines=lines)\n"
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
                Log.log("Unable to get pep8 module.");
                return messages;
            }

            IAdaptable projectAdaptable = prefs.getProjectAdaptable();
            if (AnalysisPreferencesPage.useSystemInterpreter(projectAdaptable)) {
                String parameters = AnalysisPreferencesPage.getPep8CommandLineAsStr(projectAdaptable);
                String output = PyFormatStd.runWithPep8BaseScript(document.get(), parameters, "pep8.py", "");
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

            String[] pep8CommandLine = AnalysisPreferencesPage.getPep8CommandLine(projectAdaptable);
            FastStringBuffer args = new FastStringBuffer(pep8CommandLine.length * 20);
            for (String string : pep8CommandLine) {
                args.append(',').append("r'").append(string).append('\'');
            }

            //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
            //it may be that the output ends up being shared, which is not what we want.)
            boolean useConsole = AnalysisPreferencesPage.useConsole(projectAdaptable);
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
            interpreter.set("pep8", pep8Module);

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
            if (line.indexOf(messageToIgnore) != -1) {
                //keep going... nothing to see here...
                return;
            }
        }

        messages.add(new Message(IAnalysisPreferences.TYPE_PEP8, text, lineNumber, lineNumber, offset + 1, len, prefs));
    }

}
