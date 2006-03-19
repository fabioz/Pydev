/*
 * Created on Mar 18, 2006
 */
package com.python.pydev.interactiveconsole;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.python.pydev.PydevPlugin;

public class InteractiveConsolePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public static final String SHOW_CONSOLE_INPUT = "SHOW_CONSOLE_INPUT";
    public static final boolean DEFAULT_SHOW_CONSOLE_INPUT = false;

    public static final String EVAL_ON_NEW_LINE = "EVAL_ON_NEW_LINE";
    public static final boolean DEFAULT_EVAL_ON_NEW_LINE = false;
    
    public InteractiveConsolePreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());      
        
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(SHOW_CONSOLE_INPUT, "Show the input given to the console?", p));
        addField(new BooleanFieldEditor(EVAL_ON_NEW_LINE, "Evaluate on console on each new line (or only on request)?", p));
    }

    public void init(IWorkbench workbench) {
    }

    /**
     * should we show the inputs that are given to the console?
     */
    public static boolean showConsoleInput() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_CONSOLE_INPUT);
    }
    
    /**
     * should we evaluate on each new line or only on request?
     */
    public static boolean evalOnNewLine() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(EVAL_ON_NEW_LINE);
    }

}
