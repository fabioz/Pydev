/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.debug.ui.IDebugPreferencesPageParticipant;
import org.python.pydev.utils.ComboFieldEditor;

import com.python.pydev.debug.DebugPluginPrefsInitializer;

public class DebugPreferencesPageExt implements IDebugPreferencesPageParticipant {

    private static final String[][] ENTRIES_AND_VALUES = new String[][] {
            { "Platform Default (Run/Debug Preferences)",
                    Integer.toString(DebugPluginPrefsInitializer.FORCE_SHOW_SHELL_ON_BREAKPOINT_MAKE_NOTHING) },

            { "Force Bring to Front (windows)",
                    Integer.toString(DebugPluginPrefsInitializer.FORCE_SHOW_SHELL_ON_BREAKPOINT_MAKE_ACTIVE) },

            {
                    "Show Progress on Taskbar (windows 7) ",
                    Integer.toString(DebugPluginPrefsInitializer.FORCE_SHOW_SHELL_ON_BREAKPOINT_SHOW_INDETERMINATE_PROGRESS) },
    };

    public void createFieldEditors(DebugPrefsPage page, Composite parent) {
        page.addField(new IntegerFieldEditor(DebugPluginPrefsInitializer.PYDEV_REMOTE_DEBUGGER_PORT,
                "Port for remote debugger:", parent, 10));

        BooleanFieldEditor editor = new BooleanFieldEditor(DebugPluginPrefsInitializer.DEBUG_SERVER_ALWAYS_ON,
                "Remote debugger server always on?", BooleanFieldEditor.SEPARATE_LABEL, parent);
        page.addField(editor);
        editor.getLabelControl(parent)
                .setToolTipText(
                        "Checking this option will initialize the remote debugger on plugin activation and will restart it when it's stopped.");

        ComboFieldEditor comboEditor = new ComboFieldEditor(DebugPluginPrefsInitializer.FORCE_SHOW_SHELL_ON_BREAKPOINT,
                "On breakpoint hit: ", ENTRIES_AND_VALUES, parent);

        page.addField(comboEditor);
        comboEditor.getLabelControl(parent)
                .setToolTipText(
                        "Checking this option will force Eclipse to have focus when a PyDev breakpoint is hit.");

    }

}
