/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.core.log.Log;

import com.aptana.interactive_console.InteractiveConsolePlugin;
import com.aptana.interactive_console.console.ui.ScriptConsoleManager;
import com.aptana.js.interactive_console.JsInteractiveConsolePlugin;
import com.aptana.js.interactive_console.console.env.JSIProcessFactory;
import com.aptana.js.interactive_console.console.env.JSIProcessFactory.JSConsoleLaunchInfo;
import com.aptana.js.interactive_console.console.env.UserCanceledException;

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
public class JSConsoleFactory implements IConsoleFactory {

    /**
     * @see IConsoleFactory#openConsole()
     */
    public void openConsole() {
        createConsole(null);
    }

    /**
     * Create a new JSConsole or null if unable to create it (user cancels it)
     */
    public void createConsole(String additionalInitialComands) {
        try {
            JSConsoleInterpreter interpreter = createDefaultJSInterpreter();
            if (interpreter == null) {
                return;
            }
            createConsole(interpreter, additionalInitialComands);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void createConsole(final JSConsoleInterpreter interpreter, final String additionalInitialComands) {

        Job job = new Job("Create Interactive Console") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Create Interactive Console", 10);
                IStatus returnStatus = Status.OK_STATUS;
                try {
                    ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
                    monitor.worked(1);
                    JSConsole console = new JSConsole(interpreter, additionalInitialComands);
                    monitor.worked(1);
                    manager.add(console, true);
                } catch (Exception e) {
                    Log.log(e);
                    returnStatus = new Status(IStatus.ERROR, JsInteractiveConsolePlugin.PLUGIN_ID,
                            "Error initializing console.", e);

                } finally {
                    monitor.done();
                }

                return returnStatus;
            }

        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * @return A JSConsoleInterpreter with its communication configured.
     * 
     * @throws CoreException
     * @throws IOException
     * @throws UserCanceledException
     */
    public static JSConsoleInterpreter createDefaultJSInterpreter() throws Exception, UserCanceledException {
        JSIProcessFactory iprocessFactory = new JSIProcessFactory();

        JSConsoleLaunchInfo launchAndProcess = iprocessFactory.createInteractiveLaunch();
        if (launchAndProcess == null) {
            return null;
        }
        return createJSInterpreter(launchAndProcess);

    }

    // Use IProcessFactory to get the required tuple
    public static JSConsoleInterpreter createJSInterpreter(JSConsoleLaunchInfo info) throws Exception {
        final ILaunch launch = info.launch;
        Process process = info.process;
        Integer clientPort = info.clientPort;
        if (launch == null) {
            return null;
        }

        JSConsoleInterpreter consoleInterpreter = new JSConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(JSIProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(new JSConsoleCommunication(port, process, clientPort));
        consoleInterpreter.setLaunch(launch);
        consoleInterpreter.setProcess(process);

        InteractiveConsolePlugin.getDefault().addConsoleLaunch(launch);

        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                InteractiveConsolePlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    }

}
