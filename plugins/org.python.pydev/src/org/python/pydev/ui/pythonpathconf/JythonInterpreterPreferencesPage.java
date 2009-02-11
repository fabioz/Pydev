/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
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
    
    protected void createFieldEditors() {
        super.createFieldEditors();
        addField(new DirectoryFieldEditor(IInterpreterManager.JYTHON_CACHE_DIR, "-Dpython.cachedir", getFieldEditorParent()));
    }

    @Override
    protected void doRestore(IProgressMonitor monitor) {
        IInterpreterManager iMan = getInterpreterManager();
        iMan.restorePythopathForAllInterpreters(monitor);
        
        //we also need to restart our code-completion shell after doing that, as we may have a new classpath,
        //and because of some jython bugs, just adding info to the sys.path later on as in python, is not enough.
        for(String interpreter:iMan.getInterpreters()){
            AbstractShell.stopServerShell(interpreter, AbstractShell.COMPLETION_SHELL);
        }
    }

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager(true);
    }
    
}
