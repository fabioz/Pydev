/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Special launch configuration for Jython
 */
public class JythonTabGroup extends AbstractLaunchConfigurationTabGroup {

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
     */
    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        MainModuleTab mainModuleTab = new MainModuleTab();
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { mainModuleTab,
                new ArgumentsTab(mainModuleTab), new InterpreterTab(PydevPlugin.getJythonInterpreterManager()),
                new RefreshTab(), new EnvironmentTab(), new CommonTab() };
        setTabs(tabs);
    }
}
