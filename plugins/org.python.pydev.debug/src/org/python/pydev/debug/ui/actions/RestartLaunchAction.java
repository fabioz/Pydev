/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.bindingutils.KeyBindingHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

@SuppressWarnings("restriction")
public class RestartLaunchAction extends PyAction implements IUpdate {

    protected IPageBookViewPage page;
    protected ProcessConsole console;
    private final ILaunch launch;
    private final ILaunchConfiguration launchConfiguration;

    private static ILaunch lastLaunch;
    private static ILaunchConfiguration lastConfig;

    public RestartLaunchAction(IPageBookViewPage page, ProcessConsole console) {
        this.page = page;
        this.console = console;
        launch = this.console.getProcess().getLaunch();
        launchConfiguration = launch.getLaunchConfiguration();

        lastLaunch = launch;
        lastConfig = launch.getLaunchConfiguration();

        update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update() {
        IProcess process = console.getProcess();
        setEnabled(true);
        KeySequence binding = KeyBindingHelper
                .getCommandKeyBinding("org.python.pydev.debug.ui.actions.relaunchLastAction");
        String str = binding != null ? "(" + binding.format() + " when on Pydev editor)" : "(unbinded)";
        if (process.canTerminate()) {
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH));
            this.setToolTipText("Restart the current launch. " + str);

        } else {
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH1));
            this.setToolTipText("Relaunch with the same configuration." + str);
        }
    }

    public static void relaunch(ILaunch launch, ILaunchConfiguration launchConfiguration) {
        if (launch != null && launchConfiguration != null) {
            try {
                launch.terminate();
            } catch (DebugException e) {
                Log.log(e);
            }
            try {
                launchConfiguration.launch(launch.getLaunchMode(), null);
            } catch (CoreException e) {
                Log.log(e);
            }
        }
    }

    public void run(IAction action) {
        relaunch(launch, launchConfiguration);
    }

    public void run() {
        run(this);
    }

    public void dispose() {
        this.page = null;
        this.console = null;
    }

    public static void relaunchLast() {
        relaunch(lastLaunch, lastConfig);
    }

}
