package org.python.pydev.debug.ui.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.console.OpenConsoleAction;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;

public class InteractiveConsoleConfigurationDelegate extends LaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
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
