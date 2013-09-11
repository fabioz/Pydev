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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.pydev.core.NullOutputStream;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;

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
            + //It always accesses sys.argv[0] in process_options, so, it must be set.
            "\n"
            + "if pep8 == None:\n"
            + //Optimization: if possible don't import pep8 (the import was the slowest thing in this code).
            "    add_to_pythonpath = '%s'\n"
            + "    if add_to_pythonpath not in sys.path:\n"
            + "        sys.path.append(add_to_pythonpath)\n"
            + "    import pep8\n"
            + "\n"
            + "options, args = pep8.process_options(argv[1:])\n"
            + //don't use sys.argv (it seems it doesn't get updated as it should).
              //"print options\n" + uncomment for debugging options
            "checker = pep8.Checker(options, '%s', lines)\n" +
            "\n"
            + "def report_error(line_number, offset, text, check):\n" +
            "    code = text[:4]\n"
            + "    if pep8.ignore_code(checker.options, code) or code in checker.expected:\n" +
            "        return\n"
            + "    visitor.reportError(line_number, offset, text, check)\n"
            + "    return original(line_number, offset, text, check)\n" +
            "\n" +
            "\n"
            + "original = checker.report_error\n" +
            "checker.report_error = report_error\n" +
            "\n"
            + "checker.check_all()\n" +
            "\n" +
            "";

    private final List<IMessage> messages = new ArrayList<IMessage>();
    private IAnalysisPreferences prefs;
    private IDocument document;
    private volatile static PyObject pep8;
    private static final Object lock = new Object();
    private String messageToIgnore;

    public List<IMessage> getMessages(SourceModule module, IDocument document, IProgressMonitor monitor,
            IAnalysisPreferences prefs) {
        try {

            if (prefs.getSeverityForType(IAnalysisPreferences.TYPE_PEP8) < IMarker.SEVERITY_WARNING) {
                return messages;
            }
            messageToIgnore = prefs.getRequiredMessageToIgnore(IAnalysisPreferences.TYPE_PEP8);

            String[] pep8CommandLine = AnalysisPreferencesPage.getPep8CommandLine();

            FastStringBuffer args = new FastStringBuffer(pep8CommandLine.length * 20);
            for (String string : pep8CommandLine) {
                args.append(',').append("r'").append(string).append('\'');
            }

            String pep8Location = AnalysisPreferencesPage.getPep8Location();

            File pep8Loc = new File(pep8Location);

            if (!pep8Loc.exists()) {
                Log.log("Specified location for pep8.py does not exist (" + pep8Location + ").");
                return messages;
            }

            this.prefs = prefs;
            this.document = document;

            //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
            //it may be that the output ends up being shared, which is not what we want.)
            boolean useConsole = AnalysisPreferencesPage.useConsole();
            IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(useConsole, false);
            if (!useConsole) {
                interpreter.setErr(NullOutputStream.singleton);
                interpreter.setOut(NullOutputStream.singleton);
            }
            String file = StringUtils.replaceAllSlashes(module.getFile().getAbsolutePath());
            interpreter.set("visitor", this);

            List<String> splitInLines = StringUtils.splitInLines(document.get());
            interpreter.set("lines", splitInLines);
            PyObject tempPep8 = pep8;
            if (tempPep8 != null) {
                interpreter.set("pep8", tempPep8);
            } else {
                interpreter.set("pep8", Py.None);
            }

            String formatted = org.python.pydev.shared_core.string.StringUtils.format(EXECUTE_PEP8, file, args.toString(),
                    StringUtils.replaceAllSlashes(pep8Loc.getParentFile().getAbsolutePath()), //put the parent dir of pep8.py in the pythonpath.
                    file);
            interpreter.exec(formatted);
            if (pep8 == null) {
                synchronized (lock) {
                    if (pep8 == null) {
                        pep8 = interpreter.get("pep8");
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
