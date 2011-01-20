/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.interpreters;

import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

/**
 * On a number of cases, we may want to do some action that relies on the python nature, but we are uncertain
 * on which should actually be used (python or jython).
 * 
 * So, this class helps in giving a choice for the user.
 *
 * @author Fabio
 */
public class ChooseInterpreterManager {

    public static IInterpreterManager chooseInterpreterManager(){
        return chooseInterpreterManager(true);
    }
    
    /**
     * 
     * @param showWarningIfNotConfigured if true, a warning will be shown to the user
     * @return an interpreter manager for the only configured manager (either python or jython) or if both are available,
     * it chooses for python.
     * 
     * May return null if unable to choose an interpreter.
     * 
     * TODO: Instead of choosing always python as default if both are available, ask the user (and save that info).
     */
    public static IInterpreterManager chooseInterpreterManager(boolean showWarningIfNotConfigured){
        IInterpreterManager pyManager = PydevPlugin.getPythonInterpreterManager();
        IInterpreterManager jyManager = PydevPlugin.getJythonInterpreterManager();
        IInterpreterManager useManager = null;
        if(pyManager.isConfigured()){
            //default is python, so that's it
            useManager = pyManager;
        }else if(jyManager.isConfigured()){
            //ok, no python... go for jython
            useManager = jyManager;
        }
        
        if(useManager == null && showWarningIfNotConfigured){
            RunInUiThread.async(new Runnable(){

                public void run() {
                    MessageDialog.openError(PyAction.getShell(), "No configured manager", 
                    "Neither the python nor the jython\ninterpreter is configured.");
                }});
        }
        
        return useManager;

    }
}
