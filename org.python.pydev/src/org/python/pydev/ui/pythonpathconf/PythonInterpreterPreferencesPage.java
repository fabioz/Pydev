/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IInterpreterManager;

public class PythonInterpreterPreferencesPage extends InterpreterPreferencesPage{

    public String getTitle() {
        return "Python Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "Python interpreters (e.g.: python.exe)";
    }

    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    protected InterpreterEditor getInterpreterEditor(Composite p) {
        return new InterpreterEditor (getInterpretersTitle(), p, PydevPlugin.getPythonInterpreterManager());
    }
    

    /**
     * @param defaultSelectedInterpreter this is the path to the default selected file (interpreter)
     * @param monitor a monitor to display the progress to the user.
     */
    protected void doRestore(final String defaultSelectedInterpreter, IProgressMonitor monitor) {
        monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
        IInterpreterManager iMan = PydevPlugin.getPythonInterpreterManager();
        final InterpreterInfo info = iMan.getInterpreterInfo(defaultSelectedInterpreter, monitor);
        info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager
        
        monitor.done();
    }


}
