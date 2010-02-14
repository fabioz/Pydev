package org.python.pydev.debug.newconsole;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple3;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
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
     * @see IConsoleFactory#openConsole()
     */
    public void openConsole() {
        createConsole();
    }
    
    /**
     * @return a new PydevConsole or null if unable to create it (user cancels it)
     */
    public PydevConsole createConsole() {
        try {
        	return createConsole(createDefaultPydevInterpreter());
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
    }
    
    public PydevConsole createConsole(PydevConsoleInterpreter interpreter) {
        ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
        try {
            if(interpreter != null){
                PydevConsole console = new PydevConsole(interpreter);
                manager.add(console, true);
                return console;
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
    	
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

//            import sys; sys.ps1=''; sys.ps2=''
//            import sys;print >> sys.stderr, ' '.join([sys.executable, sys.platform, sys.version])
//            print >> sys.stderr, 'PYTHONPATH:'
//            for p in sys.path:
//                print >> sys.stderr,  p
//
//            print >> sys.stderr, 'Ok, all set up... Enjoy'
        
        IProcessFactory iprocessFactory = new IProcessFactory();
        
        Tuple3<Launch, Process, Integer> launchAndProcess = iprocessFactory.createInteractiveLaunch();
        if(launchAndProcess == null){
            return null;
        }
        return createPydevInterpreter(
        		launchAndProcess.o1, launchAndProcess.o2, 
        		launchAndProcess.o3,  iprocessFactory.getNaturesUsed());

        
    }
    
    // Use IProcessFactory to get the required tuple
    public static PydevConsoleInterpreter createPydevInterpreter(
    		final ILaunch launch, Process process, Integer clientPort, 
    		List<IPythonNature> natures) throws Exception {
        if(launch == null){
            return null;
        }

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(IProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(
        		new PydevConsoleCommunication(port, process, clientPort));
        consoleInterpreter.setNaturesUsed(natures);
        
        PydevDebugPlugin.getDefault().addConsoleLaunch(launch);
        
        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                PydevDebugPlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    	
    }

}
