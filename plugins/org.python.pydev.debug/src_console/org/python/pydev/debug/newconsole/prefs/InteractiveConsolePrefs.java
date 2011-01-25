/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.utils.MultiStringFieldEditor;

public class InteractiveConsolePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public InteractiveConsolePrefs() {
        super(FLAT);
    }
    
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        
        ColorFieldEditor sysout = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR, "Stdout color", p); 
        ColorFieldEditor syserr = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_ERROR_COLOR, "Stderr color", p); 
        ColorFieldEditor sysin = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_INPUT_COLOR, "Stdin color", p);
        ColorFieldEditor prompt = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_PROMPT_COLOR, "Prompt color", p);
        ColorFieldEditor background = new ColorFieldEditor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR, "Background color", p);
        
        
        addField(sysout);
        addField(syserr);
        addField(sysin);
        addField(prompt);
        addField(background);
        
        addField(new MultiStringFieldEditor(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS, 
                "Initial\ninterpreter\ncommands:\n", p));
        
        addField(new StringFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS, 
                "Vm Args for jython\n(used only on external\nprocess option):", p));
        
        addField(new IntegerFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS, 
                "Maximum connection attempts\nfor initial communication:", p));
    }

    public void init(IWorkbench workbench) {
        setDescription("Pydev interactive console preferences."); 
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    public static int getMaximumAttempts() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if(plugin != null){
            return plugin.getPreferenceStore().getInt(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);
        }else{
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS;
        }
    }

}
