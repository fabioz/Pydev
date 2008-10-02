package org.python.pydev.logging;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;


/**
 * Preferences page for logging -- gives the option to enable logging on some specific feature
 * and show it in the console.
 * 
 * @author Fabio
 */
public class PyLoggingPreferencesPage  extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public static final String DEBUG_CODE_COMPLETION = "DEBUG_CODE_COMPLETION";
    public static final boolean DEFAULT_DEBUG_CODE_COMPLETION = false;
    
    
    public static final String DEBUG_ANALYSIS_REQUESTS = "DEBUG_ANALYSIS_REQUESTS";
    public static final boolean DEFAULT_DEBUG_ANALYSIS_REQUESTS = false;
    
    
    public PyLoggingPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }


    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        
        addField(new BooleanFieldEditor(
                DEBUG_CODE_COMPLETION, "Enable logging for code completion?", p));
        
        
        addField(new BooleanFieldEditor(
                DEBUG_ANALYSIS_REQUESTS, "Enable logging for analysis requests?", p));
        
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    
    public static boolean isToDebugCodeCompletion() {
        if(PydevPlugin.getDefault() == null){//testing
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_CODE_COMPLETION);
    }
    
    public static boolean isToDebugAnalysisRequests() {
        if(PydevPlugin.getDefault() == null){//testing
            return false;
        }
        return PydevPrefs.getPreferences().getBoolean(DEBUG_ANALYSIS_REQUESTS);
    }
    
    
    @Override
    public boolean performOk() {
        boolean ret = super.performOk();
        PyCodeCompletion.DEBUG_CODE_COMPLETION = isToDebugCodeCompletion();
        DebugSettings.DEBUG_ANALYSIS_REQUESTS = isToDebugAnalysisRequests();
        return ret;
    }


    


}
