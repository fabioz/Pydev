/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.codecompletion.PythonShell;

/**
 * @author Fabio Zadrozny
 */
public class PyReloadCodeCompletionModules extends PyAction{

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {
            PythonShell.getServerShell(PythonShell.COMPLETION_SHELL).sendReloadModulesMsg();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
