/*
 * Author: atotic
 * Created: Aug 20, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
// E3 import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
// E3 import org.eclipse.debug.ui.RefreshTab;

/**
 * Create tabs for the debugger setup.
 * 
 * <p>Creates the tabs in the Debug setup window
 */
public class PythonTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new PythonMainTab(),
// E3			new RefreshTab(),
// E3			new EnvironmentTab(),
			new CommonTab()	};
		setTabs(tabs);
	}
}
