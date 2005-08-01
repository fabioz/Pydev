/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferenceInitializer;
import com.python.pydev.analysis.IAnalysisPreferences;

public class AnalysisPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AnalysisPreferencesPage() {
        super(FLAT);
        setDescription("PyDev Analysis");
        setPreferenceStore(null);
    }
    
    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return AnalysisPlugin.getDefault().getPreferenceStore();
    }
    
    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        String[][] values = new String[][]{
                {"Error"  , String.valueOf(IMarker.SEVERITY_ERROR)},
                {"Warning", String.valueOf(IMarker.SEVERITY_WARNING)},
                {"Ignore" , String.valueOf(IAnalysisPreferences.SEVERITY_IGNORE)}
        };

        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT, "Unused import severity", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE, "Unused variable severity", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE, "Undefined variable severity", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE, "Duplicated signature severity", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_REIMPORT, "Import redefinition severity", 3,values,p, true));

    }

    public void init(IWorkbench workbench) {
    }

}
