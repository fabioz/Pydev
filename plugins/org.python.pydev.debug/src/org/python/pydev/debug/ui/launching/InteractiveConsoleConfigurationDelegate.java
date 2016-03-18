/******************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;

public class InteractiveConsoleConfigurationDelegate extends LaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {

        // We need to cancel this automatic recreation because we are launching from scratch again
        monitor.setCanceled(true);
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                PydevConsoleFactory pydevConsoleFactory = new PydevConsoleFactory();
                pydevConsoleFactory.openConsole();
            }
        });
    }

}
