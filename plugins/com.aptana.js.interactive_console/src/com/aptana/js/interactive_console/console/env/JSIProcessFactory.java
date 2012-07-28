/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 20, 2006
 */
package com.aptana.js.interactive_console.console.env;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.aptana.shared_core.net.SocketUtil;

/**
 * This class is used to create the given IProcess and get the console that is
 * attached to that process.
 */
public class JSIProcessFactory {

    public static final class JSConsoleLaunchInfo {
        public final Launch launch;
        public final Process process;
        public final int clientPort;

        /**
         * @param launch
         * @param process
         * @param clientPort
         * @param interpreter
         * @param frame
         */
        public JSConsoleLaunchInfo(Launch launch, Process process,
                int clientPort) {
            this.launch = launch;
            this.process = process;
            this.clientPort = clientPort;
        }
    }

    /**
     * @return a shell that we can use.
     */
    public Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static final String INTERACTIVE_LAUNCH_PORT = "INTERACTIVE_LAUNCH_PORT"; //$NON-NLS-1$

    /**
     * Creates a launch (and its associated IProcess) for the xml-rpc server to
     * be used in the interactive console.
     * 
     * It'll ask the user how to create it: - editor - python interpreter -
     * jython interpreter
     * 
     * @return the Launch, the Process created and the port that'll be used for
     *         the server to call back into this client for requesting input.
     * 
     * @throws UserCanceledException
     * @throws Exception
     */
    public JSConsoleLaunchInfo createInteractiveLaunch()
            throws UserCanceledException, Exception {

        ChooseProcessTypeDialog dialog = new ChooseProcessTypeDialog(getShell());
        if (dialog.open() == ChooseProcessTypeDialog.OK) {
            return createLaunch(dialog);
        }
        return null;
    }

    private static ILaunchConfiguration createLaunchConfig() {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = manager
                .getLaunchConfigurationType("com.aptana.js.interactive_console.interactiveConsoleConfigurationType"); //$NON-NLS-1$
        ILaunchConfigurationWorkingCopy newInstance;
        try {
            newInstance = launchConfigurationType.newInstance(null, manager
                    .generateLaunchConfigurationName("JS Interactive Launch")); //$NON-NLS-1$
        } catch (CoreException e) {
            return null;
        }
        newInstance.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
        return newInstance;
    }

    public JSConsoleLaunchInfo createLaunch(ChooseProcessTypeDialog dialog)
            throws Exception {
        Process process = null;
        Integer[] ports = SocketUtil.findUnusedLocalPorts(2);
        int port = ports[0];
        int clientPort = ports[1];

        final Launch launch = new Launch(createLaunchConfig(), "interactive", //$NON-NLS-1$
                null);
        launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false"); //$NON-NLS-1$
        launch.setAttribute(INTERACTIVE_LAUNCH_PORT, String.valueOf(port));

        process = new RhinoEclipseProcess(port, clientPort);

        IProcess newProcess = new JSSpawnedInterpreterProcess(launch,
                process, "JS name for UI", null);

        launch.addProcess(newProcess);

        return new JSConsoleLaunchInfo(launch, process, clientPort);
    }

}
