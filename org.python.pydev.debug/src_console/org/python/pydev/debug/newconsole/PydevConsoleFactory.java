package org.python.pydev.debug.newconsole;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;
import org.python.pydev.plugin.PydevPlugin;

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
            if(interpreter != null){
	            PydevConsole console = new PydevConsole(interpreter);
	            console.setPrompt(createConsolePrompt());

	            manager.add(console, true);
            }
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
    public static PydevConsoleInterpreter createDefaultPydevInterpreter() throws Exception, 
            UserCanceledException {


//            final ILaunch launch = new IProcessFactory().createILaunch(project, 
//                    PydevPlugin.getScriptWithinPySrc("pydevconsole.py"), manager, ""+port);
            
//            import sys; sys.ps1=''; sys.ps2=''
//            import sys;print >> sys.stderr, ' '.join([sys.executable, sys.platform, sys.version])
//            print >> sys.stderr, 'PYTHONPATH:'
//            for p in sys.path:
//                print >> sys.stderr,  p
//
//            print >> sys.stderr, 'Ok, all set up... Enjoy'
        final ILaunch launch = new IProcessFactory().createInteractiveLaunch();
        if(launch == null){
        	return null;
        }

        PydevConsoleInterpreter interpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(IProcessFactory.INTERACTIVE_LAUNCH_PORT));
        interpreter.setConsoleCommunication(new PydevConsoleCommunication(port));

        if (launch != null) {
            interpreter.addCloseOperation(new Runnable() {
                public void run() {
                    IProcess[] processes = launch.getProcesses();
                    if (processes != null) {
                        for (IProcess p:processes) {
                            try {
                                p.terminate();
                            } catch (Exception e) {
                                PydevPlugin.log(e);
                            }
                        }
                    }
                }
            });
        }
        return interpreter;

    }

}
