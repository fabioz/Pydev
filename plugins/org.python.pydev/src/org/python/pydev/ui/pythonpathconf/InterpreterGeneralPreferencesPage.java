/******************************************************************************
* Copyright (C) 2013  Andrew Ferrazzutti
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Andrew Ferrazzutti <aferrazz@redhat.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.field_editors.ButtonFieldEditor;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * Preferences page for general PyDev interpreter (or interpreter-related) settings.
 *
 * Created on Sep 3, 2013
 * @author Andrew Ferrazzutti
 *
 *
 *
 */
public class InterpreterGeneralPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public InterpreterGeneralPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Interpreters: General Preferences");
    }

    public static final String NOTIFY_NO_INTERPRETER = "NOTIFY_NO_INTERPRETER_";
    public static final String NOTIFY_NO_INTERPRETER_PY = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_PYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_PY = true;

    public static final String NOTIFY_NO_INTERPRETER_JY = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_JYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_JY = true;

    public static final String NOTIFY_NO_INTERPRETER_IP = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_IRONPYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_IP = true;

    public static final String CHECK_CONSISTENT_ON_STARTUP = "CHECK_CONSISTENT_ON_STARTUP";
    public final static boolean DEFAULT_CHECK_CONSISTENT_ON_STARTUP = true;

    public static final String UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES = "UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES";
    public final static boolean DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES = true;

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_PY, "Notify when a Python project has no interpreter?", p));
        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_JY, "Notify when a Jython project has no interpreter?", p));
        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_IP,
                "Notify when an IronPython project has no interpreter?", p));

        SelectionListener selectionListener = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                PydevPlugin.getDefault().syncScheduler.checkAllNow();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };
        addField(new LabelFieldEditor(
                "UNUSED",
                "Important: changes below only take place after a restart.\n"
                        + "\n"
                        + "If disabling those preferences, remember to manually add/remove\n"
                        + "PYTHONPATH entries from the interpreter (or click the button below)\n"
                        + "when the PYTHONPATH changes on the system.\n"
                        + "\n"
                        + "Note that PYTHONPATH changes are gotten only if .pth files are changed.\n"
                        + "If the PYTHONPATH environment variable changes, the shell that started\n"
                        + "Eclipse has to be restarted so that the change can be detected.\n"
                        + "\n"
                        + "-- Consider unchecking only if the startup time is too slow.", p));
        addField(new BooleanFieldEditor(CHECK_CONSISTENT_ON_STARTUP,
                "Check initial consistency (in 1 minute) after startup?", p));
        addField(new BooleanFieldEditor(UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
                "Check consistency when related files (i.e.: .py, .pth) in the PYTHONPATH change?",
                p));
        addField(new ButtonFieldEditor(
                "NOT_USED",
                "Check if interpreters are synchronized with environment.",
                p,
                selectionListener,
                "This action will execute a job which will run in background and\n"
                        + "will check if the PYTHONPATH for the interpreters match the paths\n"
                        + "that the interpreter reports as its PYTHONPATH (i.e.: for when a new library is added).\n"
                        + "\n"
                        + "All previous selections related to ignoring the changes will be cleared in this process.\n"
                        + "\n"
                        + "The internal state of the interpreters will also be checked for corruption.\n"
                ,
                null));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    public static boolean getCheckConsistentOnStartup() {
        return PydevPrefs.getPreferences().getBoolean(CHECK_CONSISTENT_ON_STARTUP);
    }

    public static boolean getReCheckOnFilesystemChanges() {
        return PydevPrefs.getPreferences().getBoolean(UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES);
    }

}
