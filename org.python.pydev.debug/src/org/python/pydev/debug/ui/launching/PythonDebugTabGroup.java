/*
 * Author: wrwright
 * Created on Feb 7, 2004
 * License: Common Public License v1.0
 */ 

package org.python.pydev.debug.ui.launching;

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;

/**
 * Defines tab set for python launcher.
 */
public class PythonDebugTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * @see AbstractLaunchConfigurationTabGroup#createTabs
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)  {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new PythonTab(),
				new CommonTab()
		};
		setTabs(tabs);

	}
}
