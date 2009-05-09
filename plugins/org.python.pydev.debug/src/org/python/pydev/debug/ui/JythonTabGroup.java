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
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        MainModuleTab mainModuleTab = new MainModuleTab();
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
            new ArgumentsTab(mainModuleTab),
            new InterpreterTab(PydevPlugin.getJythonInterpreterManager()),
            new RefreshTab(),
            new EnvironmentTab(),
            new CommonTab() };
        setTabs(tabs);
    }
}
