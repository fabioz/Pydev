/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.pep8;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.analysis.messages.Message;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.pep8.Pep8Runner;
import org.python.pydev.shared_core.jython.JythonPep8Core;
import org.python.pydev.shared_core.string.StringUtils;

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
                String output = Pep8Runner.runWithPep8BaseScript(document, parameters, "pycodestyle.py");
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

}
