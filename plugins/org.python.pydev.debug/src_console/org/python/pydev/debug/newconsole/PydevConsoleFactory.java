/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.debug.newconsole.env.IProcessFactory.PydevConsoleLaunchInfo;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;

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
        	return createConsole(createDefaultPydevInterpreter(), null);
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }
    
    /**
     * @return a new PydevConsole or null if unable to create it (user cancels it)
     */
    public PydevConsole createConsole(String additionalInitialComands) {
        try {
            return createConsole(createDefaultPydevInterpreter(), additionalInitialComands);
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }
    
    public PydevConsole createConsole(PydevConsoleInterpreter interpreter, String additionalInitialComands) {
        ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
        try {
            if(interpreter != null){
                PydevConsole console = new PydevConsole(interpreter, additionalInitialComands);
                manager.add(console, true);
                return console;
            }
        } catch (Exception e) {
            Log.log(e);
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
        
        PydevConsoleLaunchInfo launchAndProcess = iprocessFactory.createInteractiveLaunch();
        if(launchAndProcess == null){
            return null;
        }
        return createPydevInterpreter(launchAndProcess, iprocessFactory.getNaturesUsed());

        
    }
    
    // Use IProcessFactory to get the required tuple
    public static PydevConsoleInterpreter createPydevInterpreter(
            PydevConsoleLaunchInfo info, List<IPythonNature> natures) throws Exception {
        final ILaunch launch = info.launch;
        Process process = info.process;
        Integer clientPort = info.clientPort;
        IInterpreterInfo interpreterInfo = info.interpreter;
        if(launch == null){
            return null;
        }

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(IProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(
        		new PydevConsoleCommunication(port, process, clientPort));
        consoleInterpreter.setNaturesUsed(natures);
        consoleInterpreter.setInterpreterInfo(interpreterInfo);
        
        PydevDebugPlugin.getDefault().addConsoleLaunch(launch);
        
        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                PydevDebugPlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    	
    }

}
