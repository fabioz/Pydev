/**
 * Copyright (c) 2015 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

public class PyTabPreferencesPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PyTabPreferencesPage() {
        super(GRID);
        final IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        setDescription("Tab preferences for PyDev.\n\nNote: PyDev ignores the 'Insert spaces for tabs' in the general settings.\n\n");
        setPreferenceStore(store);
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new IntegerFieldEditor(PydevEditorPrefs.TAB_WIDTH, "Tab length:", p));
        addField(new BooleanFieldEditor(PydevEditorPrefs.SUBSTITUTE_TABS, "Replace tabs with spaces when typing?", p));
        addField(new BooleanFieldEditor(PydevEditorPrefs.GUESS_TAB_SUBSTITUTION,
                "Assume tab spacing when files contain tabs?", p));
        addField(new BooleanFieldEditor(PydevEditorPrefs.TAB_STOP_IN_COMMENT, "Allow tab stops in comments?", p));

        addField(new ScopedPreferencesFieldEditor(p, PydevPlugin.DEFAULT_PYDEV_SCOPE, this));
    }
}
