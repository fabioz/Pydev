/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

public class InteractiveConsolePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFERENCES_ID = "org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs";

    public InteractiveConsolePrefs() {
        super(FLAT);
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new ColorFieldEditor(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR, "Stdout color", p));

        addField(new ColorFieldEditor(PydevConsoleConstants.CONSOLE_ERROR_COLOR, "Stderr color", p));

        addField(new ColorFieldEditor(PydevConsoleConstants.CONSOLE_INPUT_COLOR, "Stdin color", p));

        addField(new ColorFieldEditor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR, "Prompt color", p));

        addField(new ColorFieldEditor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR,
                "Background color", p));

        addField(new ColorFieldEditor(PydevConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR,
                "Debug console background color", p));

        addField(new StringFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS,
                "Vm Args for jython\n(used only on external\nprocess option):", p));

        addField(new StringFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_ENCODING,
                "Encoding for interactive console:", p));

        addField(new IntegerFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS,
                "Maximum connection attempts\nfor initial communication:", p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START,
                "Focus console when it's started?", BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_TAB_COMPLETION,
                "Enable tab completion in interactive console?", BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new IntegerFieldEditor(
                ScriptConsoleUIConstants.INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES,
                "Maximum number of lines to\nstore in global history\n(0 for unlimited):", p) {
            // We are trying to set a preference that is in a different store, but logically lives within this UI
            @Override
            public IPreferenceStore getPreferenceStore() {
                return InteractiveConsolePlugin.getDefault().getPreferenceStore();
            }
        });

        addField(new BooleanFieldEditor(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR,
                "When creating console send\ncurrent selection/editor\ncontents for execution?",
                BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND,
                "Focus console when an evaluate\ncommand is sent from the editor?", BooleanFieldEditor.SEPARATE_LABEL,
                p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION,
                "Connect console to a Debug Session?", BooleanFieldEditor.SEPARATE_LABEL, p));

        addField(new ComboFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP,
                "Enable GUI event loop integration?",
                PydevConsoleConstants.ENTRIES_VALUES_INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP, p));

    }

    public void init(IWorkbench workbench) {
        setDescription("PyDev interactive console preferences.");
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    public static int getMaximumAttempts() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getInt(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);
    }

    public static boolean getFocusConsoleOnStartup() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getBoolean(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);
    }

    public static boolean getFocusConsoleOnSendCommand() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getBoolean(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);
    }

    public static boolean getTabCompletionInInteractiveConsole() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_TAB_COMPLETION);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_TAB_COMPLETION;
        }
    }

    public static boolean getConsoleConnectDebugSession() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getBoolean(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION);
    }

    public static boolean getSendCommandOnCreationFromEditor() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getBoolean(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);
    }

    public static String getEnableGuiOnStartup() {
        if (SharedCorePlugin.inTestMode()) {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        return plugin.getPreferenceStore().getString(
                PydevConsoleConstants.INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP);
    }

}
