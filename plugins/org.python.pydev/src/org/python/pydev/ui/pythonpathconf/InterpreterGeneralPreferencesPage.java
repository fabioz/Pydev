/******************************************************************************
* Copyright (C) 2013  Andrew Ferrazzutti
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Ecliplse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Andrew Ferrazzutti <aferrazz@redhat.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;

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

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_PY, "Notify when a Python project has no interpreter?", p));
        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_JY, "Notify when a Jython project has no interpreter?", p));
        addField(new BooleanFieldEditor(NOTIFY_NO_INTERPRETER_IP,
                "Notify when an IronPython project has no interpreter?", p));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

}
