package org.python.pydev.debug.newconsole;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;

/**
 * Could ask to configure the interpreter in the preferences
 * 
 * PreferencesUtil.createPreferenceDialogOn(null, preferencePageId, null, null)
 * 
 * This is the class responsible for creating the console (and setting up the communication
 * between the console server and the client).
 *
 * @author Fabio
 */
public class PydevConsoleFactory implements IConsoleFactory {

    
    /**
     * @return the prompt to be used in the console.
     */
    protected ScriptConsolePrompt createConsolePrompt() {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();

        String newPrompt = store.getString(PydevConsoleConstants.PREF_NEW_PROMPT);
        String continuePrompt = store.getString(PydevConsoleConstants.PREF_CONTINUE_PROMPT);

        if (newPrompt == null || newPrompt.length() == 0) {
            newPrompt = PydevConsoleConstants.DEFAULT_NEW_PROMPT;
        }
        if (continuePrompt == null || continuePrompt.length() == 0) {
            continuePrompt = PydevConsoleConstants.DEFAULT_CONTINUE_PROMPT;
        }

        return new ScriptConsolePrompt(newPrompt, continuePrompt);
    }


    /**
     * @see IConsoleFactory#openConsole()
     */
    public void openConsole() {
        ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
        try {
            PydevConsoleInterpreter interpreter = createDefaultPydevInterpreter();
            PydevConsole console = new PydevConsole(interpreter);
            console.setPrompt(createConsolePrompt());

            manager.add(console, true);
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

    /**
     * @return A PydevConsoleInterpreter with its communication configured.
     * 
     * @throws CoreException
     * @throws IOException
     * @throws UserCanceledException
     */
    public static PydevConsoleInterpreter createDefaultPydevInterpreter() throws CoreException, IOException,
            UserCanceledException {


        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof PyEdit) {
            PyEdit edit = (PyEdit) activeEditor;

            IProject project = edit.getProject();
            IInterpreterManager manager = edit.getPythonNature().getRelatedInterpreterManager();
            int port = SocketUtil.findUnusedLocalPort();
            
            final ILaunch launch = new IProcessFactory().createILaunch(project, 
                    PydevPlugin.getScriptWithinPySrc("pydevconsole.py"), manager, ""+port);
            
//            import sys; sys.ps1=''; sys.ps2=''
//            import sys;print >> sys.stderr, ' '.join([sys.executable, sys.platform, sys.version])
//            print >> sys.stderr, 'PYTHONPATH:'
//            for p in sys.path:
//                print >> sys.stderr,  p
//
//            print >> sys.stderr, 'Ok, all set up... Enjoy'

            PydevConsoleInterpreter interpreter = new PydevConsoleInterpreter();
            interpreter.setConsoleCommunication(new PydevConsoleCommunication(port));

            if (launch != null) {
                interpreter.addCloseOperation(new Runnable() {
                    public void run() {
                        IProcess[] processes = launch.getProcesses();
                        if (processes != null) {
                            for (int i = 0; i < processes.length; i++) {
                                try {
                                    processes[i].terminate();
                                } catch (DebugException e) {
                                    PydevPlugin.log(e);
                                }
                            }
                        }
                    }
                });
            }
            return interpreter;
        } else {
            PydevPlugin.log("Active editor must be an instance of a Pydev Editor for creating a console!");
            return null;
        }

    }

}
