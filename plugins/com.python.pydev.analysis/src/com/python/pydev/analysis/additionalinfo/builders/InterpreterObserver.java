/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo.builders;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterObserver;

import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;

public class InterpreterObserver implements IInterpreterObserver {

    /**
     * Received when the user changes the interpreter PYTHONPATH.
     * 
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyDefaultPythonpathRestored(org.python.pydev.ui.interpreters.AbstractInterpreterManager, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void notifyDefaultPythonpathRestored(IInterpreterManager manager, String interpreter,
            IProgressMonitor monitor) {
        AdditionalSystemInterpreterInfo.recreateAllInfo(manager, interpreter, monitor);
    }

    public void notifyProjectPythonpathRestored(final PythonNature nature, IProgressMonitor monitor) {
        AdditionalProjectInterpreterInfo.recreateAllInfo(nature, monitor);
    }

    /**
     * Received when the interpreter manager is recreated (i.e.: starting up eclipse).
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyInterpreterManagerRecreated(org.python.pydev.ui.interpreters.AbstractInterpreterManager)
     */
    public void notifyInterpreterManagerRecreated(final IInterpreterManager iManager) {
        //no-op: the additional info is recreated lazily now (so, the first time it's asked for, it's restored).
    }

    public void notifyNatureRecreated(final PythonNature nature, IProgressMonitor monitor) {
        //no-op: the additional info is recreated lazily now (so, the first time it's asked for, it's restored).
    }

}
