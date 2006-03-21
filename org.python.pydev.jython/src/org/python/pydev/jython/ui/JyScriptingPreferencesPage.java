package org.python.pydev.jython.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.jython.JythonPlugin;

public class JyScriptingPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

	public static final String SHOW_SCRIPTING_OUTPUT = "SHOW_SCRIPTING_OUTPUT";
	public static final boolean DEFAULT_SHOW_SCRIPTING_OUTPUT = false;
	
	public static final String LOG_SCRIPTING_ERRORS = "LOG_SCRIPTING_ERRORS";
	public static final boolean DEFAULT_LOG_SCRIPTING_ERRORS = true;


	public JyScriptingPreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
        setPreferenceStore(JythonPlugin.getDefault().getPreferenceStore());      
	}
	
    public void init(IWorkbench workbench) {
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new BooleanFieldEditor(SHOW_SCRIPTING_OUTPUT, "Show the output given from the scripting to some console?", p));
        addField(new BooleanFieldEditor(LOG_SCRIPTING_ERRORS, "Show errors from scripting in the Error Log?", p));
    }
    
    /**
     * @return if we should show the scripting output in a shell.
     */
    public static boolean getShowScriptingOutput(){
        return JythonPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_SCRIPTING_OUTPUT);
    }
    
    /**
     * @return if we should show the scripting output in a shell.
     */
    public static boolean getLogScriptingErrors(){
    	return JythonPlugin.getDefault().getPreferenceStore().getBoolean(LOG_SCRIPTING_ERRORS);
    }
}
