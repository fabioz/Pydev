/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;

/**
 * Preferences page for logging -- gives the option to enable logging on some specific feature
 * and show it in the console.
 *
 * @author Fabio
 */
public class PyLoggingPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String DEBUG_CODE_COMPLETION = "DEBUG_CODE_COMPLETION";
    public static final boolean DEFAULT_DEBUG_CODE_COMPLETION = false;

    public static final String DEBUG_ANALYSIS_REQUESTS = "DEBUG_ANALYSIS_REQUESTS";
    public static final boolean DEFAULT_DEBUG_ANALYSIS_REQUESTS = false;

    public static final String DEBUG_INTERPRETER_AUTO_UPDATE = "DEBUG_INTERPRETER_UPDATE";
    public static final boolean DEFAULT_DEBUG_INTERPRETER_AUTO_UPDATE = false;

    public PyLoggingPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new LabelFieldEditor(
                "UNUSED",
                "Note: to log, check the items for which it should be enabled.\n\n"
                        + "The log output should appear in a console view tab\nand in the file with the link below.\n\n"
                        + "Afterwards, remember to turn it off again, as things\n"
                        + "may be slower when logging is enabled.\n\n", p));

        addField(new BooleanFieldEditor(DEBUG_CODE_COMPLETION, "Enable logging for code completion?", p));

        addField(new BooleanFieldEditor(DEBUG_ANALYSIS_REQUESTS, "Enable logging for analysis requests?", p));

        addField(new BooleanFieldEditor(DEBUG_INTERPRETER_AUTO_UPDATE, "Enable logging for interpreter auto update?", p));

        String logOutputFile = Log.getLogOutputFile();
        if (logOutputFile != null) {
            final File f = new File(logOutputFile);
            addField(new LinkFieldEditor("UNUSED 2",
                    "\nDirectory containing log file: " + f.getName() + "<a>\n"
                            + f.getParent().toString() + "</a>", p,
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            FileUtils.openDirectory(f.getParentFile());
                        }
                    }));
        }

    }

    public void init(IWorkbench workbench) {
    }

    public static boolean isToDebugCodeCompletion() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_CODE_COMPLETION);
    }

    public static boolean isToDebugAnalysisRequests() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_ANALYSIS_REQUESTS);
    }

    public static boolean isToDebugInterpreterAutoUpdate() {
        if (SharedCorePlugin.inTestMode()) {
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_INTERPRETER_AUTO_UPDATE);
    }

    @Override
    public boolean performOk() {
        boolean ret = super.performOk();
        DebugSettings.DEBUG_CODE_COMPLETION = isToDebugCodeCompletion();
        DebugSettings.DEBUG_ANALYSIS_REQUESTS = isToDebugAnalysisRequests();
        DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE = isToDebugInterpreterAutoUpdate();
        return ret;
    }

}
