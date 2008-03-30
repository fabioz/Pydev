package org.python.pydev.debug.newconsole;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.ui.console.IConsoleFactory;
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
        ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
        try {
            PydevConsoleInterpreter interpreter = createDefaultPydevInterpreter();
            if(interpreter != null){
	            PydevConsole console = new PydevConsole(interpreter);
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
        final ILaunch launch = launchAndProcess.o1;
        if(launch == null){
            return null;
        }

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(IProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(new PydevConsoleCommunication(port, launchAndProcess.o2, launchAndProcess.o3));
        consoleInterpreter.setNaturesUsed(iprocessFactory.getNaturesUsed());
        
        PydevDebugPlugin.getDefault().addConsoleLaunch(launch);
        
        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                PydevDebugPlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    }

}
