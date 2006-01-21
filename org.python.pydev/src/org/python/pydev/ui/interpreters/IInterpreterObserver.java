/*
 * Created on 07/09/2005
 */
package org.python.pydev.ui.interpreters;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.plugin.nature.PythonNature;

public interface IInterpreterObserver {

    /**
     * Notifies observers that the pythonpath has just been restored with the given interpreter information.
     * 
     * @param manager the manager that had its default system information just restored
     * @param defaultSelectedInterpreter 
     * @param monitor the monitor used in the restore
     */
    void notifyDefaultPythonpathRestored(IInterpreterManager manager, String defaultSelectedInterpreter, IProgressMonitor monitor);

    /**
     * Notifies observers that the given interpreter manager has just been recreated (this is due to restarting the plugin)
     *  
     * @param manager the manager that has just been restored
     */
    void notifyInterpreterManagerRecreated(IInterpreterManager interpreterManager);

    /**
     * Notifies observers that the given nature has just had its pythonpath restored
     *  
     * @param nature the nature that had its pythonpath recreated
     * @param monitor 
     */
    void notifyProjectPythonpathRestored(PythonNature nature, IProgressMonitor monitor);
    
    /**
     * Notifies observers that the given nature has just been recreated
     * 
     * @param nature the recreated nature
     * @param monitor 
     */
    void notifyNatureRecreated(PythonNature nature, IProgressMonitor monitor);


}
