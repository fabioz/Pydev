/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

public class AnalysisPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AnalysisPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("PyDev Analysis");
        
    }
    private static final String USE_PYDEV_ANALYSIS = "USE_PYDEV_ANALYSIS";
    private static final boolean DEFAULT_USE_PYDEV_ANALYSIS = true;

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_PYDEV_ANALYSIS, "Use PyDev Code Analysis?", p));

    }

    public void init(IWorkbench workbench) {
    }

}
