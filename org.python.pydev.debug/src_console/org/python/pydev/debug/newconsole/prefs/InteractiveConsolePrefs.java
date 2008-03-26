package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;

public class InteractiveConsolePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        ColorFieldEditor sysout = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_SYS_OUT_COLOR, "Stdout color", parent); 
        ColorFieldEditor syserr = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_SYS_ERR_COLOR, "Stderr color", parent); 
        ColorFieldEditor sysin = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_SYS_IN_COLOR, "Stdin color", parent);
        ColorFieldEditor prompt = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR, "Prompt color", parent);
        ColorFieldEditor background = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR, "Background color", parent);
        
        addField(sysout);
        addField(syserr);
        addField(sysin);
        addField(prompt);
        addField(background);
    }

    public void init(IWorkbench workbench) {
        setDescription("Pydev console preferences."); 
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());

    }

}
