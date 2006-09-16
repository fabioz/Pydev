/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.plugin.PydevPlugin;

public class JythonInterpreterPreferencesPage extends AbstractInterpreterPreferencesPage{

    public String getTitle() {
        return "Jython Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "Jython interpreters (e.g.: jython.jar)";
    }

    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    protected AbstractInterpreterEditor getInterpreterEditor(Composite p) {
        return new JythonInterpreterEditor (getInterpretersTitle(), p, PydevPlugin.getJythonInterpreterManager(true));
    }

    @Override
    protected void doRestore(String defaultSelectedInterpreter, IProgressMonitor monitor) {
        IInterpreterManager iMan = getInterpreterManager();
        iMan.restorePythopathFor(defaultSelectedInterpreter, monitor);
        
        //we also need to restart our code-completion shell after doing that, as we may have a new classpath,
        //and because of some jython bugs, just adding info to the sys.path later on as in python, is not enough.
        AbstractShell.stopServerShell(IPythonNature.JYTHON_RELATED, AbstractShell.COMPLETION_SHELL);
    }

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager(true);
    }
    
}
