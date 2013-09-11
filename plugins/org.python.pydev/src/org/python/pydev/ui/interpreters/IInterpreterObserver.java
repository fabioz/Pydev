/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package org.python.pydev.ui.interpreters;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.nature.PythonNature;

public interface IInterpreterObserver {

    /**
     * Notifies observers that the pythonpath has just been restored with the given interpreter information (the information
     * has been re-set by the user)
     * 
     * @param manager the manager that had its default system information just restored
     * @param defaultSelectedInterpreter 
     * @param monitor the monitor used in the restore
     */
    void notifyDefaultPythonpathRestored(IInterpreterManager manager, String defaultSelectedInterpreter,
            IProgressMonitor monitor);

    /**
     * Notifies observers that the given interpreter manager has just been recreated (this is due to restarting the plugin)
     *  
     * @param interpreterManager the manager that has just been recreated
     */
    void notifyInterpreterManagerRecreated(IInterpreterManager interpreterManager);

    /**
     * Notifies observers that the given nature has just had its pythonpath restored (the information was changed by the user)
     *  
     * @param nature the nature that had its pythonpath recreated
     * @param monitor 
     */
    void notifyProjectPythonpathRestored(PythonNature nature, IProgressMonitor monitor);

    /**
     * Notifies observers that the given nature has just been recreated (after the plugin restarted)
     * 
     * @param nature the recreated nature
     * @param monitor 
     */
    void notifyNatureRecreated(PythonNature nature, IProgressMonitor monitor);

}
