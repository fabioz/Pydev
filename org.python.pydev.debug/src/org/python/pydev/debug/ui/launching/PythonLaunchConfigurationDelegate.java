/*
 * Author: atotic
 * Created: Aug 16, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.ui.IWindowListener;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * 
 * Launcher for the python scripts.
 * 
 * <p>The code is pretty much copied from ExternalTools' ProgramLaunchDelegate.
 * <p>I would have subclassed, but ProgramLaunchDelegate hides important internals
 * 
 * Based on org.eclipse.ui.externaltools.internal.program.launchConfigurations.ProgramLaunchDelegate
 */
public class PythonLaunchConfigurationDelegate implements ILaunchConfigurationDelegate
	{
	private static IWindowListener windowListener;
	
	/**
	 * Launches the python process.
	 * 
	 * Modelled after Ant & Java runners
	 * see WorkbenchLaunchConfigurationDelegate::launch
	 */
	public void launch(ILaunchConfiguration conf, String mode,
		ILaunch launch, IProgressMonitor monitor) throws CoreException {

		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask("Preparing configuration", 3);

		PythonRunnerConfig runConfig = new PythonRunnerConfig(conf, mode);
		
		monitor.worked(1);
		try {
			PythonRunner.run(runConfig, launch, monitor);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unexpected IO Exception in Pydev debugger", null));
		}
//		ClassLoader save = cur.getContextClassLoader();
//		cur.setContextClassLoader(getClass().getClassLoader());
//		try {
//			PythonDebugClient test = new PythonDebugClient();
//			test.init("localhost", 29000, -1, null, null, null); // do whatever needs the
// contextClassLoader
//		} catch (PythonDebugException e1) {
//			DebugPlugin.log(e1);
//		} finally {
//		  cur.setContextClassLoader(save);
//		}
//		if (CommonTab.isLaunchInBackground(conf)) {
//		// refresh resources after process finishes
//			if (RefreshTab.getRefreshScope(conf) != null) {
//				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(conf, process);
//				refresher.startBackgroundRefresh();
//			}
//			// refresh resources
//			RefreshTab.refreshResources(conf, monitor);
//		}
	}
}
