/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.io.File;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.IInteractiveConsoleConstants;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

/**
 * This class will setup the editor so that we can create interactive consoles, send code to it or make an execfile.
 *
 * It is as a 'singleton' for all PyEdit editors.
 */
public class EvaluateActionSetter implements IPyEditListener {

    private class EvaluateAction extends Action {
        private final PyEdit edit;

        private EvaluateAction(PyEdit edit) {
            super();
            this.edit = edit;
        }

        @Override
        public void run() {
            try {
                PySelection selection = new PySelection(edit);

                ScriptConsole console = ScriptConsole.getActiveScriptConsole();

                if (console == null) {
                    //if no console is available, create it (if possible).
                    PydevConsoleFactory factory = new PydevConsoleFactory();
                    String cmd = null;

                    //Check if the current selection should be sent to the editor.
                    if (InteractiveConsolePrefs.getSendCommandOnCreationFromEditor()) {
                        cmd = getCommandToSend(edit, selection);
                        if (cmd != null) {
                            cmd = "\n" + cmd;
                        }
                    }
                    factory.createConsole(cmd);

                } else {
                    if (console instanceof ScriptConsole) {
                        //ok, console available
                        sendCommandToConsole(selection, console, this.edit);
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Sends the current selected text/editor to the console.
     */
    private static void sendCommandToConsole(PySelection selection, ScriptConsole console, PyEdit edit)
            throws BadLocationException {
        ScriptConsole pydevConsole = console;
        IDocument document = pydevConsole.getDocument();

        String cmd = getCommandToSend(edit, selection);
        if (cmd != null) {
            document.replace(document.getLength(), 0, cmd);
        }

        if (InteractiveConsolePrefs.getFocusConsoleOnSendCommand()) {
            ScriptConsoleViewer viewer = pydevConsole.getViewer();
            if (viewer == null) {
                return;
            }
            StyledText textWidget = viewer.getTextWidget();
            if (textWidget == null) {
                return;
            }
            textWidget.setFocus();
        }
    }

    /**
     * Gets the command to send to the console (either the selected text or a runfile with the editor).
     */
    private static String getCommandToSend(PyEdit edit, PySelection selection) {
        String cmd = null;
        String code = selection.getTextSelection().getText();

        if (code.length() != 0) {
            cmd = code + "\n";
        } else {
            //no code available: do a runfile in the current context
            File editorFile = edit.getEditorFile();

            if (editorFile != null) {
                cmd = PythonSnippetUtils.getRunfileCommand(editorFile);
            }
        }
        return cmd;
    }

    /**
     * This method associates Ctrl+new line with the evaluation of commands in the console.
     */
    public void onCreateActions(ListResourceBundle resources, final BaseEditor baseEditor, IProgressMonitor monitor) {
        final PyEdit edit = (PyEdit) baseEditor;
        final EvaluateAction evaluateAction = new EvaluateAction(edit);
        evaluateAction.setActionDefinitionId(IInteractiveConsoleConstants.EVALUATE_ACTION_ID);
        evaluateAction.setId(IInteractiveConsoleConstants.EVALUATE_ACTION_ID);
        Runnable runnable = new Runnable() {
            public void run() {
                if (!edit.isDisposed()) {
                    edit.setAction(IInteractiveConsoleConstants.EVALUATE_ACTION_ID, evaluateAction);
                }
            }
        };
        Display.getDefault().syncExec(runnable);
    }

    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
        //ignore
    }

    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
        //ignore
    }

    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
        //ignore
    }

}
