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
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
// import org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher;
import org.eclipse.ui.externaltools.internal.variable.ExpandVariableContext;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * 
 * Launcher for the python scripts.
 * 
 * <p>The code is pretty much copied from ExternalTools' ProgramLaunchDelegate.
 * <p>I would have subclassed, but ProgramLaunchDelegate hides important internals
 * 
 * @see org.eclipse.ui.externaltools.internal.program.launchConfigurations.ProgramLaunchDelegate
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

		ExpandVariableContext resourceContext = ExternalToolsUtil.getVariableContext();
		PythonRunnerConfig runConfig = new PythonRunnerConfig(conf, mode, resourceContext);
		PythonRunner runner = new PythonRunner();
		
		monitor.worked(1);
		try {
			runner.run(runConfig, launch, monitor);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, PydevDebugPlugin.getPluginID(), 0, "Unexpected IO Exception in Pydev debugger", null));
		}
//		ClassLoader save = cur.getContextClassLoader();
//		cur.setContextClassLoader(getClass().getClassLoader());
//		try {
//			PythonDebugClient test = new PythonDebugClient();
//			test.init("localhost", 29000, -1, null, null, null);			  // do whatever needs the contextClassLoader
//		} catch (PythonDebugException e1) {
//			DebugPlugin.log(e1);
//		} finally {
//		  cur.setContextClassLoader(save);
//		}			
// E3		if (CommonTab.isLaunchInBackground(conf)) {
// E3		// refresh resources after process finishes
// E3		if (RefreshTab.getRefreshScope(conf) != null) {
// E3			BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(conf, process);
// E3			refresher.startBackgroundRefresh();
// E3		}				
// refresh resources
// E3			RefreshTab.refreshResources(conf, monitor);
	}
}
