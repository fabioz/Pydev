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
import org.python.pydev.ast.codecompletion.revisited.DefaultSyncSystemModulesManagerScheduler;
import org.python.pydev.core.preferences.InterpreterGeneralPreferences;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.shared_ui.field_editors.ButtonFieldEditor;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * Preferences page for general PyDev interpreter (or interpreter-related) settings.
 *
 * Created on Sep 3, 2013
 * @author Andrew Ferrazzutti
 */
public class InterpreterGeneralPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public InterpreterGeneralPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PyDevUiPrefs.getPreferenceStore());
        setDescription("Interpreters: General Preferences");
    }

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_PY,
                "Notify when a Python project has no interpreter?",
                p));
        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_JY,
                "Notify when a Jython project has no interpreter?",
                p));
        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_IP,
                "Notify when an IronPython project has no interpreter?", p));

        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.USE_TYPESHED,
                "Use Typeshed for type inference (EXPERIMENTAL)?", p));

        SelectionListener selectionListener = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                DefaultSyncSystemModulesManagerScheduler.get().checkAllNow();
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
                        + "-- Consider unchecking only if the startup time is too slow.",
                p));
        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.CHECK_CONSISTENT_ON_STARTUP,
                "Check initial consistency (in 1 minute) after startup?", p));
        addField(new BooleanFieldEditor(InterpreterGeneralPreferences.UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
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
                        + "The internal state of the interpreters will also be checked for corruption.\n",
                null));
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

}
