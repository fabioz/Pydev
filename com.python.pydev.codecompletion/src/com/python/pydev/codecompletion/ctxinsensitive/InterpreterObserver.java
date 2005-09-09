/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.NotConfiguredInterpreterException;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.interpreters.IInterpreterObserver;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class InterpreterObserver implements IInterpreterObserver{

    public static Map<String, AdditionalInterpreterInfo> additionalInfo = new HashMap<String, AdditionalInterpreterInfo>();
    
    /**
     * received when the interpreter info has been restored
     *  
     * this means that we have to create the additional information for the interpreter info
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifySystemPythonpathRestored(org.python.pydev.ui.pythonpathconf.InterpreterInfo, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void notifySystemPythonpathRestored(InterpreterInfo info, String pythonpath, IProgressMonitor monitor) {
        //the pythonpath is ignored
    }

    /**
     * received when the interpreter manager is restored
     *  
     * this means that we have to restore the additional interpreter information we stored
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyInterpreterManagerRecreated(org.python.pydev.ui.interpreters.AbstractInterpreterManager)
     */
    public void notifyInterpreterManagerRecreated(AbstractInterpreterManager manager) {
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            InterpreterInfo defaultInterpreterInfo = manager.getDefaultInterpreterInfo(monitor);
        } catch (NotConfiguredInterpreterException e) {
            //we should ignore that because there is no interpreter configured for us to get additional information.
        }
    }

    public void notifyProjectPythonpathRestored(PythonNature nature) {
    }

    public void notifyNatureRecreated(PythonNature nature) {
    }

}
