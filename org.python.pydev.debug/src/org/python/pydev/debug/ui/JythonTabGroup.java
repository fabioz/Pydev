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

public class JythonTabGroup extends AbstractLaunchConfigurationTabGroup {
    
    public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
            new MainTab(PydevPlugin.getJythonInterpreterManager()), 
            new PythonProjectRelatedTab(PydevPlugin.getJythonInterpreterManager()),
            new RefreshTab(),
            new EnvironmentTab(),
            new CommonTab() };
        setTabs(tabs);
    }

}
