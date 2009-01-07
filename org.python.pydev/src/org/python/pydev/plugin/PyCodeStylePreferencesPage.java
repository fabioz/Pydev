package org.python.pydev.plugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.preferences.PydevPrefs;

public class PyCodeStylePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_LOCALS_AND_ATTRS_CAMELCASE = "USE_LOCALS_AND_ATTRS_CAMELCASE";

    public static final boolean DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE = true;

    public PyCodeStylePreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_LOCALS_AND_ATTRS_CAMELCASE, "Use locals and attrs in camel case (used for assign quick-assist)?", p));
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean useLocalsAndAttrsCamelCase() {
        return PydevPrefs.getPreferences().getBoolean(USE_LOCALS_AND_ATTRS_CAMELCASE);
    }

}

