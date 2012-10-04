/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.utils.MultiStringFieldEditor;

import com.aptana.js.interactive_console.JsInteractiveConsolePlugin;
import com.aptana.js.interactive_console.console.JSConsoleConstants;

public class InteractiveConsolePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFERENCES_ID = "com.aptana.js.interactive_console.console.prefs.InteractiveConsolePrefs";

    public InteractiveConsolePrefs() {
        super(FLAT);
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        ColorFieldEditor sysout = new ColorFieldEditor(JSConsoleConstants.CONSOLE_OUTPUT_COLOR, "Stdout color", p);
        ColorFieldEditor syserr = new ColorFieldEditor(JSConsoleConstants.CONSOLE_ERROR_COLOR, "Stderr color", p);
        ColorFieldEditor sysin = new ColorFieldEditor(JSConsoleConstants.CONSOLE_INPUT_COLOR, "Stdin color", p);
        ColorFieldEditor prompt = new ColorFieldEditor(JSConsoleConstants.CONSOLE_PROMPT_COLOR, "Prompt color", p);
        ColorFieldEditor background = new ColorFieldEditor(JSConsoleConstants.CONSOLE_BACKGROUND_COLOR,
                "Background color", p);
        ColorFieldEditor debugBackground = new ColorFieldEditor(JSConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR,
                "Debug console background color", p);

        addField(sysout);
        addField(syserr);
        addField(sysin);
        addField(prompt);
        addField(background);
        addField(debugBackground);

        addField(new MultiStringFieldEditor(JSConsoleConstants.INITIAL_INTERPRETER_CMDS,
                "Initial\ninterpreter\ncommands:\n", p));

        addField(new StringFieldEditor(JSConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS,
                "Vm Args for Rhino\n(used only on external\nprocess option):", p));

        addField(new IntegerFieldEditor(JSConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS,
                "Maximum connection attempts\nfor initial communication:", p));

        addField(new BooleanFieldEditor(JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START,
                "Focus console when it's started?", BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(
                JSConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR,
                "When creating console send\ncurrent selection/editor\ncontents for execution?",
                BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND,
                "Focus console when an evaluate\ncommand is sent from the editor?", BooleanFieldEditor.SEPARATE_LABEL,
                p));

    }

    public void init(IWorkbench workbench) {
        setDescription("JS interactive console preferences.");
        setPreferenceStore(JsInteractiveConsolePlugin.getDefault().getPreferenceStore());
    }

    public static String getInitialInterpreterCmds() {
        JsInteractiveConsolePlugin plugin = JsInteractiveConsolePlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getString(
                    JSConsoleConstants.INITIAL_INTERPRETER_CMDS);
        } else {
            return "";
        }
    }

    public static int getMaximumAttempts() {
        JsInteractiveConsolePlugin plugin = JsInteractiveConsolePlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getInt(
                    JSConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);
        } else {
            return JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS;
        }
    }

    public static boolean getFocusConsoleOnStartup() {
        JsInteractiveConsolePlugin plugin = JsInteractiveConsolePlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);
        } else {
            return JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START;
        }
    }

    public static boolean getFocusConsoleOnSendCommand() {
        JsInteractiveConsolePlugin plugin = JsInteractiveConsolePlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);
        } else {
            return JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND;
        }
    }

    public static boolean getSendCommandOnCreationFromEditor() {
        JsInteractiveConsolePlugin plugin = JsInteractiveConsolePlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    JSConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);
        } else {
            return JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR;
        }
    }

}
