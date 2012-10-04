/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.utils.MultiStringFieldEditor;

public class InteractiveConsolePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFERENCES_ID = "org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs";

    public InteractiveConsolePrefs() {
        super(FLAT);
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        ColorFieldEditor sysout = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR, "Stdout color", p);
        ColorFieldEditor syserr = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_ERROR_COLOR, "Stderr color", p);
        ColorFieldEditor sysin = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_INPUT_COLOR, "Stdin color", p);
        ColorFieldEditor prompt = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR, "Prompt color", p);
        ColorFieldEditor background = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR,
                "Background color", p);
        ColorFieldEditor debugBackground = new ColorFieldEditor(PydevConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR,
                "Debug console background color", p);

        addField(sysout);
        addField(syserr);
        addField(sysin);
        addField(prompt);
        addField(background);
        addField(debugBackground);

        addField(new MultiStringFieldEditor(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS,
                "Initial\ninterpreter\ncommands:\n", p));

        addField(new StringFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS,
                "Vm Args for jython\n(used only on external\nprocess option):", p));

        addField(new IntegerFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS,
                "Maximum connection attempts\nfor initial communication:", p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START,
                "Focus console when it's started?", BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR,
                "When creating console send\ncurrent selection/editor\ncontents for execution?",
                BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND,
                "Focus console when an evaluate\ncommand is sent from the editor?", BooleanFieldEditor.SEPARATE_LABEL,
                p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW,
                "Connect console to Variables Debug View?", BooleanFieldEditor.SEPARATE_LABEL, p));

    }

    public void init(IWorkbench workbench) {
        setDescription("PyDev interactive console preferences.");
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    public static int getMaximumAttempts() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getInt(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS;
        }
    }

    public static boolean getFocusConsoleOnStartup() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START;
        }
    }

    public static boolean getFocusConsoleOnSendCommand() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND;
        }
    }

    public static boolean getConsoleConnectVariableView() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW;
        }
    }

    public static boolean getSendCommandOnCreationFromEditor() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR;
        }
    }

}
