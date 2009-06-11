package org.python.pydev.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Create tabs for the debugger setup.
 * 
 * <p>
 * Creates the tabs in the Debug setup window
 * </p>
 * 
 * TODO: Fix tabs so that invalid configuration disables Apply and Run buttons always
 */
public class IronpythonTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
        MainModuleTab mainModuleTab = new MainModuleTab();
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                mainModuleTab, 
                new ArgumentsTab(mainModuleTab),
                new InterpreterTab(PydevPlugin.getIronpythonInterpreterManager()),
                new RefreshTab(), 
                new EnvironmentTab(), 
                new CommonTab() };
        setTabs(tabs);
    }
}
