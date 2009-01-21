/*
 * Author: atotic
 * Created: Jun 23, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.core;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
/**
 * Pydev preference page.
 * 
 * <p>Uses FieldEditor framework for preference editing.
 * <p>Defaults are declared here as constants.
 * <p>There is a string constant for every prefernce you can use for access
 * <p>Framework takes care of storing of the prefs
 * <p>The meaning of preferences are documented in user docs, for details grep
 * the source for the particular string.
 */
public class PydevDebugPrefs extends FieldEditorPreferencePage 
    implements IWorkbenchPreferencePage{

    // Preferences    
    public static final String GET_VARIABLE_TIMEOUT = "GET_VARIABLE_TIMEOUT";// GetVariable command timeout
    public static final int DEFAULT_GET_VARIABLE_TIMEOUT = 30;
    public static final String HIDE_PYDEVD_THREADS = "HIDE_PYDEVD_THREADS";
    public static final boolean DEFAULT_HIDE_PYDEVD_THREADS = true;
        
    /**
     * Initializer sets the preference store
     */
    public PydevDebugPrefs() {
        super(GRID);
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    static public Preferences getPreferences() {
        return     PydevDebugPlugin.getDefault().getPluginPreferences();
    }
    
    public void init(IWorkbench workbench) {        
    }
    
    /**
     * Creates the editors
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        IntegerFieldEditor ife = new IntegerFieldEditor(
        GET_VARIABLE_TIMEOUT, "Debugger get variable timeout (seconds)", p);
        ife.setValidRange(1, 180);
        // you can't restrict widget width on IntegerFieldEditor for now
        addField(ife);
    }
    
    /**
     * Sets default preference values
     */
    protected static void initializeDefaultPreferences(Preferences prefs) {
        prefs.setDefault(GET_VARIABLE_TIMEOUT, DEFAULT_GET_VARIABLE_TIMEOUT);
        prefs.setDefault(HIDE_PYDEVD_THREADS, DEFAULT_HIDE_PYDEVD_THREADS);
    }
}
