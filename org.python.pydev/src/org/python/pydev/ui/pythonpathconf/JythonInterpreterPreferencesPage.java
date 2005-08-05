/*
 * Created on 03/08/2005
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.plugin.PydevPlugin;

public class JythonInterpreterPreferencesPage extends InterpreterPreferencesPage {

    @Override
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
        return new InterpreterEditor (getInterpretersTitle(), p, PydevPlugin.getInterpreterManager());
    }

    /**
     * @param defaultSelectedInterpreter this is the path to the default selected file (interpreter)
     * @param monitor a monitor to display the progress to the user.
     */
    protected void doRestore(final String defaultSelectedInterpreter, IProgressMonitor monitor) {
        
    }

}
