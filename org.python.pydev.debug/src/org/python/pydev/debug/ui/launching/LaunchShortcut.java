/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
// E3 import org.eclipse.core.variables.IStringVariableManager;
// E3 import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.debug.core.*;
import org.python.pydev.plugin.PydevPrefs;

/**
 * Called when "Run Script..." popup menu item is selected.
 * 
 * <p>Manages configurations (store/load/default)
 * <p>Launches the "Run python..." window
 * <p>code almost all copied from AntLaunchShortcut:
 * Based on org.eclipse.ui.externaltools.internal.ant.launchConfigurations.AntLaunchShortcut
 */
public class LaunchShortcut implements ILaunchShortcut {

	boolean fShowDialog = false;		// show configuration dialog?
	
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				IFile resource = (IFile)((IAdaptable)object).getAdapter(IFile.class);
				if (resource != null) {
					launch(resource, mode, null);
					return;
				}
			}
		}
		fileNotFound();
	}
	
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile)input.getAdapter(IFile.class);
		if (file != null) {
			launch(file, mode, null);
			return;
		}
		fileNotFound();
	}
	
	private void fileNotFound() {
		reportError("Unable to launch the file, not found??", null);	
	}
	
	protected boolean verifyMode(String mode) {
		boolean ok = mode.equals(ILaunchManager.RUN_MODE) ||
					mode.equals(ILaunchManager.DEBUG_MODE) ||
					mode.equals(ILaunchManager.PROFILE_MODE);
		if (!ok)
			reportError("Unknown launch mode: " + mode, null);
		return ok;
	}

	
	protected static void reportError(String message, Throwable throwable) {
		if (message == null)
			message = "Unexpected error";
		IStatus status = null;
		if (throwable instanceof CoreException) {
			status = ((CoreException)throwable).getStatus();
		} else {
			status = new Status(IStatus.ERROR, "org.python.pydev.debug", 0, message, throwable);
		}
		ErrorDialog.openError(PydevDebugPlugin.getActiveWorkbenchWindow().getShell(), 
			"Python pydev.debug error", "Python launch failed", status);
	}
	
	/**
	 * COPIED/MODIFIED from AntLaunchShortcut
	 * Returns a list of existing launch configuration for the given file.
	 */
	public static List findExistingLaunchConfigurations(IFile file) {
		ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(Constants.ID_PYTHON_LAUNCH_CONFIGURATION_TYPE);
		List validConfigs= new ArrayList();
		if (type == null)
			return validConfigs;			
		try {
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
			String defaultLocation =  getDefaultLocation(file);
			for (int i = 0; i < configs.length; i++) {
				String configPath = configs[i].getAttribute(Constants.ATTR_LOCATION, "");
				if (defaultLocation.equals(configPath))
					validConfigs.add(configs[i]);
			}
		} catch (CoreException e) {
			reportError("Unexpected error", e);
		}
		return validConfigs;
	}
	
	/**
	 * @param file
	 * @return default string for the location field
	 */
	static String getDefaultLocation (IFile file) {
		return file.getRawLocation().toString();
// E3		IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
// E3		return varManager.generateVariableExpression("workspace_loc", file.getFullPath().toString());
	}
	
	/**
	 * COPIED/MODIFIED from AntLaunchShortcut
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IFile file) {
		ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(Constants.ID_PYTHON_LAUNCH_CONFIGURATION_TYPE);
		if (type == null) {
			reportError("Python launch configuration not found", null);
			return null;
		}
		StringBuffer buffer = new StringBuffer(file.getProject().getName());
		buffer.append(" ");
		buffer.append(file.getName());
		String name = buffer.toString().trim();
		name= manager.generateUniqueLaunchConfigurationNameFrom(name);
		try {

			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			// Python Main Tab Arguments
// E3			IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
			String location = getDefaultLocation(file);
// E3			String baseDirectory = varManager.generateVariableExpression("workspace_loc",file.getRawLocation().removeLastSegments(1).toString());
			String baseDirectory = file.getRawLocation().removeLastSegments(1).toString();
			String arguments = "";
			String interpreter = PydevPrefs.getDefaultInterpreter();
			workingCopy.setAttribute(Constants.ATTR_LOCATION,location);
			workingCopy.setAttribute(Constants.ATTR_WORKING_DIRECTORY,baseDirectory);
			workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS,arguments);
			workingCopy.setAttribute(Constants.ATTR_INTERPRETER,interpreter);

			// Common Tab Arguments
			CommonTab tab = new CommonTab();
			tab.setDefaults(workingCopy);
			tab.dispose();
			return workingCopy.doSave();
		} catch (CoreException e) {
			reportError(null, e);
			return null;
		}
	}

	/**
	 * COPIED/MODIFIED from AntLaunchShortcut
	 */
	public static ILaunchConfiguration chooseConfig(List configs) {
		if (configs.isEmpty()) {
			return null;
		}
		ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
		dialog.setElements(configs.toArray(new ILaunchConfiguration[configs.size()]));
		dialog.setTitle("Pick a Python configuration");
		dialog.setMessage("Choose a python configuration to run");
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK)
			return (ILaunchConfiguration) dialog.getFirstResult();
		else
			return null;
	}

	/**
	 * Launch the given targets in the given build file. The targets are
	 * launched in the given mode.
	 * 
	 * @param file the build file to launch
	 * @param mode the mode in which the build file should be executed
	 * @param targetAttribute the targets to launch, in the form of the launch
	 * configuration targets attribute.
	 */
	public void launch(IFile file, String mode, String targetAttribute) {
		if (!verifyMode(mode))
			return;

		ILaunchConfiguration conf = null;
		List configurations = findExistingLaunchConfigurations(file);
		if (configurations.isEmpty())
			conf = createDefaultLaunchConfiguration(file);
		else {
			if (configurations.size() == 1) {
				conf = (ILaunchConfiguration)configurations.get(0);
			} else {
				conf = chooseConfig(configurations);
				if (conf == null)
					// User cancelled selection
					return;
			}
		}
		if (conf != null) {
			if (fShowDialog) {
				IStatus status = new Status(IStatus.INFO, Constants.PLUGIN_ID, 0, "Hmm", null); //$NON-NLS-1$
				String groupID = "";
			    
				if(mode.equals("run")) {
			        groupID = Constants.PYTHON_RUN_LAUNCH_GROUP;
			    }else if (mode.equals("debug")){
			        groupID = Constants.PYTHON_DEBUG_LAUNCH_GROUP;
			    }else if (mode.equals("profile")){
			        groupID = Constants.PYTHON_COVERAGE_LAUNCH_GROUP;
			    }
			    
				DebugUITools.openLaunchConfigurationDialog(PydevDebugPlugin.getActiveWorkbenchWindow().getShell(), conf, groupID, null);
			} else {
				DebugUITools.launch(conf, mode);
				//  what's this code doing?
				// It is copied from Ant, so I am keeping it around
//				if (targetAttribute != null) {
//					String newName= DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(configuration.getName());
//					try {
//						configuration= configuration.copy(newName);
//						((ILaunchConfigurationWorkingCopy) configuration).setAttribute(IExternalToolConstants.ATTR_ANT_TARGETS, targetAttribute);
//					} catch (CoreException exception) {
//						reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Exception_launching"), new String[] {file.getName()}), exception); //$NON-NLS-1$
//						return;
//					}
//				}
//				DebugUITools.launch(configuration, mode);
			}
			return;
		}
		fileNotFound();
	}
	
	public void setShowDialog(boolean showDialog) {
		fShowDialog = showDialog;
	}
}
