/*
 * Author: atotic
 * Created: Aug 16, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.InterpreterEditor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;
// E3 import org.eclipse.debug.ui.CommonTab;
// E3 import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.ExternalToolsProgramMessages;
import org.eclipse.ui.externaltools.internal.variable.ExpandVariableContext;

import org.eclipse.jface.dialogs.MessageDialog;

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
	 * A window listener that warns the user about any running programs when
	 * the workbench closes. Programs are killed when the VM exits.
	 */
	private class ProgramLaunchWindowListener implements IWindowListener {
		public void windowActivated(IWorkbenchWindow window) {
		}
		public void windowDeactivated(IWorkbenchWindow window) {
		}
		public void windowClosed(IWorkbenchWindow window) {
			IWorkbenchWindow windows[]= PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 1) {
				// There are more windows still open.
				return;
			}
			ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType programType= manager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
			if (programType == null) {
				return;
			}
			ILaunch launches[]= manager.getLaunches();
			ILaunchConfigurationType configType;
			ILaunchConfiguration config;
			for (int i = 0; i < launches.length; i++) {
				try {
					config= launches[i].getLaunchConfiguration();
					if (config == null) {
						continue;
					}
					configType= config.getType();
				} catch (CoreException e) {
					continue;
				}
				if (configType.equals(programType)) {
					if (!launches[i].isTerminated()) {
						MessageDialog.openWarning(window.getShell(), ExternalToolsProgramMessages.getString("ProgramLaunchDelegate.Workbench_Closing_1"), ExternalToolsProgramMessages.getString("ProgramLaunchDelegate.The_workbench_is_exiting")); //$NON-NLS-1$ //$NON-NLS-2$
						break;
					}
				}
			}
		}
		public void windowOpened(IWorkbenchWindow window) {
		}
	}
	
	/* (non-Javadoc)
	 *
	 */
	public void launch(ILaunchConfiguration conf, String mode,
		ILaunch launch, IProgressMonitor monitor) throws CoreException {

		if (monitor.isCanceled()) 
			return;
		
		ExpandVariableContext resourceContext = ExternalToolsUtil.getVariableContext();
		
		// Get the basic parameters
		IPath location = ExternalToolsUtil.getLocation(conf, resourceContext);
		IPath workingDirectory = ExternalToolsUtil.getWorkingDirectory(conf, resourceContext);
		String interpreter = conf.getAttribute(Constants.ATTR_INTERPRETER, "python");
		String[] arguments = ExternalToolsUtil.getArguments(conf, resourceContext);
		
		if (monitor.isCanceled())
			return;
		
		// Set up the command-line arguments
		int cmdLineLength = 3;
		if (arguments != null)
			cmdLineLength += arguments.length;
		
		String[] cmdLine = new String[cmdLineLength];
		cmdLine[0] = interpreter;
		// Next option is for unbuffered stdout, otherwise Eclipse will not see any output until done
		cmdLine[1] = InterpreterEditor.isJython(interpreter) ? "-i" : "-u";	
		cmdLine[2] = location.toOSString();
		if (arguments != null)
			System.arraycopy(arguments, 0, cmdLine, 3, arguments.length);
		
		File workingDir = workingDirectory == null ? null : workingDirectory.toFile();
			
// E3		String[] envp = DebugPlugin.getDefault().getLaunchManager().getEnvironment(conf);
		
		if (monitor.isCanceled())
			return;
		
		if (windowListener == null) {
			windowListener= new ProgramLaunchWindowListener();
			PlatformUI.getWorkbench().addWindowListener(windowListener);
		}
		
		// Execute the process
//		E3		Process p = DebugPlugin.exec(cmdLine, workingDir, envp);
//DebugPlugin.newProcess(launch, )
		Process p = DebugPlugin.exec(cmdLine, workingDir);
		IProcess process = null;
		
		// add process type to process attributes
		Map processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
		
		// org.eclipse.debug.internal.ui.views.console.ConsoleDocumentPartitioner.connect() attaches streams
		if (p != null) {
			monitor.beginTask(MessageFormat.format(ExternalToolsProgramMessages.getString("ProgramLaunchDelegate.3"), new String[] {conf.getName()}), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
			process = DebugPlugin.newProcess(launch, p, location.toOSString(), processAttributes);
			if (process == null) {
				p.destroy();
// E3			throw new CoreException(new Status(IStatus.ERROR, IExternalToolConstants.PLUGIN_ID, IExternalToolConstants.ERR_INTERNAL_ERROR, ExternalToolsProgramMessages.getString("ProgramLaunchDelegate.4"), null)); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, IExternalToolConstants.PLUGIN_ID, -1, ExternalToolsProgramMessages.getString("ProgramLaunchDelegate.4"), null)); //$NON-NLS-1$
			}
		}
		process.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(cmdLine));
		if (ExternalToolsUtil.isBackground(conf)) {
			// refresh resources after process finishes
			if (ExternalToolsUtil.getRefreshScope(conf) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(conf, process, resourceContext);
				refresher.startBackgroundRefresh();
			}				

// E3		if (CommonTab.isLaunchInBackground(conf)) {
// E3		// refresh resources after process finishes
// E3		if (RefreshTab.getRefreshScope(conf) != null) {
// E3			BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(conf, process);
// E3			refresher.startBackgroundRefresh();
// E3		}				
		} else {
			// wait for process to exit
			while (!process.isTerminated()) {
				try {
					if (monitor.isCanceled()) {
						process.terminate();
						break;
					}
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			
			// refresh resources
// E3			RefreshTab.refreshResources(conf, monitor);
		}
	}
	
	private String generateCommandLine(String[] commandLine) {
		if (commandLine.length < 1)
			return ""; //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < commandLine.length; i++) {
			buf.append(' ');
			char[] characters= commandLine[i].toCharArray();
			StringBuffer command= new StringBuffer();
			boolean containsSpace= false;
			for (int j = 0; j < characters.length; j++) {
				char character= characters[j];
				if (character == '\"') {
					command.append('\\');
				} else if (character == ' ') {
					containsSpace = true;
				}
				command.append(character);
			}
			if (containsSpace) {
				buf.append('\"');
				buf.append(command);
				buf.append('\"');
			} else {
				buf.append(command);
			}
		}	
		return buf.toString();
	}
}
