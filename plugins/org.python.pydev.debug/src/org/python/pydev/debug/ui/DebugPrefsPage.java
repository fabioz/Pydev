/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jun 23, 2003
 */
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;

/**
 * Debug preferences.
 *
 * <p>Simple 1 page debug preferences page.
 * <p>Prefeernce constants are defined in Constants.java
 */
public class DebugPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Initializer sets the preference store
     */
    public DebugPrefsPage() {
        super("Debug", GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    /**
     * Creates the editors
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new IntegerFieldEditor(PyDevEditorPreferences.CONNECT_TIMEOUT, "Connect timeout for debugger (ms)", p,
                10));

        BooleanFieldEditor editor = new BooleanFieldEditor(PyDevEditorPreferences.RELOAD_MODULE_ON_CHANGE,
                "When file is changed, automatically reload module?", BooleanFieldEditor.SEPARATE_LABEL, p);
        Control c = editor.getDescriptionControl(p);
        c.setToolTipText(
                "The debugger will automatically reload a module,\n"
                        + "when a file is saved if this setting is on.\n\n"
                        + "See pydevd_reload.py for details, limitations and which hooks\n"
                        + "are provided so that your own classes act upon this change.");
        addField(editor);

        editor = new BooleanFieldEditor(PyDevEditorPreferences.DONT_TRACE_ENABLED,
                "On a step in, skip over methods which have a @DontTrace comment?", BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText(
                "When a comment: # @DontTrace is found after a method, it's skipped by the debugger if this setting is on.\n\n"
                        + "Use Ctrl+1 in a method line to add such a comment.");
        addField(editor);

        editor = new BooleanFieldEditor(PyDevEditorPreferences.SHOW_RETURN_VALUES,
                "Show return values for methods?", BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText("When this option is enabled, upon returning from a method, its return value is shown.");
        addField(editor);

        List<IDebugPreferencesPageParticipant> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_DEBUG_PREFERENCES_PAGE);
        for (IDebugPreferencesPageParticipant participant : participants) {
            participant.createFieldEditors(this, p);
        }

        editor = new BooleanFieldEditor(PyDevEditorPreferences.DEBUG_MULTIPROCESSING_ENABLED,
                "Attach to subprocess automatically while debugging?", BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText("Enabling this option will patch the functions related to launching a new process\n"
                + "and will attempt to automatically connect new launched processes to the debugger.");
        addField(editor);

        editor = new BooleanFieldEditor(PyDevEditorPreferences.GEVENT_DEBUGGING,
                "Gevent compatible debugging?", BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText("When this option is turned on, the debugger will be able to debug GEvent programs.");
        addField(editor);

        editor = new BooleanFieldEditor(PyDevEditorPreferences.DEBUG_JUST_MY_CODE,
                "Debug just my code (i.e.: only source code in source folders in PyDev)?",
                BooleanFieldEditor.SEPARATE_LABEL,
                p);
        c = editor.getDescriptionControl(p);
        c.setToolTipText(
                "When this option is turned on, the debugger will ignore any files which are not under a PyDev source folder.");
        addField(editor);

        ComboFieldEditor comboEditor = new ComboFieldEditor(PyDevEditorPreferences.QT_THREADS_DEBUG_MODE, "Qt Threads:",
                PyDevEditorPreferences.ENTRIES_VALUES_QT_THREADS_DEBUG_MODE, p);
        Label labelControl = comboEditor.getLabelControl(p);
        labelControl.setToolTipText("Specify whether the debugger should patch Qt to debug inside QThreads.");
        addField(comboEditor);
    }

    public static boolean getReloadModuleOnChange() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.RELOAD_MODULE_ON_CHANGE,
                PyDevEditorPreferences.DEFAULT_RELOAD_MODULE_ON_CHANGE);
    }

    public static boolean getDontTraceEnabled() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.DONT_TRACE_ENABLED,
                PyDevEditorPreferences.DEFAULT_DONT_TRACE_ENABLED);
    }

    public static boolean getShowReturnValuesEnabled() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.SHOW_RETURN_VALUES,
                PyDevEditorPreferences.DEFAULT_SHOW_RETURN_VALUES);
    }

    public static boolean getDebugMultiprocessingEnabled() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.DEBUG_MULTIPROCESSING_ENABLED,
                PyDevEditorPreferences.DEFAULT_DEBUG_MULTIPROCESSING_ENABLED);
    }

    public static boolean getGeventDebugging() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.GEVENT_DEBUGGING,
                PyDevEditorPreferences.DEFAULT_GEVENT_DEBUGGING);
    }

    public static boolean getJustMyCode() {
        return PydevPrefs.getEclipsePreferences().getBoolean(PyDevEditorPreferences.DEBUG_JUST_MY_CODE,
                PyDevEditorPreferences.DEFAULT_DEBUG_JUST_MY_CODE);
    }

    public static String getQtThreadsDebugMode() {
        return PydevPrefs.getEclipsePreferences().get(PyDevEditorPreferences.QT_THREADS_DEBUG_MODE,
                PyDevEditorPreferences.DEFAULT_QT_THREADS_DEBUG_MODE);
    }

    /**
     * Make it available for extensions
     */
    @Override
    public void addField(FieldEditor editor) {
        super.addField(editor);
    }

}
