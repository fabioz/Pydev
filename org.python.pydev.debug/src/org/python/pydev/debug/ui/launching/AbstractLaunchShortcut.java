/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Called when "Run Script..." popup menu item is selected.
 * 
 * <p>Manages configurations (store/load/default)
 * <p>Launches the "Run python..." window
 * <p>code almost all copied from AntLaunchShortcut:
 * Based on org.eclipse.ui.externaltools.internal.ant.launchConfigurations.AntLaunchShortcut
 */
public abstract class AbstractLaunchShortcut implements ILaunchShortcut {

	boolean fShowDialog = false;		// show configuration dialog?
	
    //=============================================================================================
    // ILaunchShortcut IMPL
	//=============================================================================================
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
                
				IFolder folder = (IFolder)((IAdaptable)object).getAdapter(IFolder.class);
				if (folder != null) {
				    launch(folder, mode, null);
				    return;
				}
			}
		}
		fileNotFound();
	}
	
    
	public void launch(IEditorPart editor, String mode) {
        //we have an editor to run
		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile)input.getAdapter(IFile.class);
		if (file != null) {
			launch(file, mode, null);
			return;
		}
		fileNotFound();
	}
	//=============================================================================================
	// END ILaunchShortcut IMPL
	//=============================================================================================
	
	protected void fileNotFound() {
		reportError("Unable to launch the file, not found??", null);	
	}
	
	protected boolean verifyMode(String mode) {
		boolean ok = mode.equals(ILaunchManager.RUN_MODE) || mode.equals(ILaunchManager.DEBUG_MODE);
		
        if (!ok){
			reportError("Unknown launch mode: " + mode, null);
        }
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
	private List findExistingLaunchConfigurations(IResource file) {
		ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
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
	private String getDefaultLocation (IResource file) {
		return file.getRawLocation().toString();
// E3		IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
// E3		return varManager.generateVariableExpression("workspace_loc", file.getFullPath().toString());
	}
	
    protected abstract String getLaunchConfigurationType();
    
	/**
	 * COPIED/MODIFIED from AntLaunchShortcut
	 */
	protected ILaunchConfiguration createDefaultLaunchConfiguration(IResource resource) {
		ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
		if (type == null) {
			reportError("Python launch configuration not found", null);
			return null;
		}

        StringBuffer buffer = new StringBuffer(resource.getProject().getName());
		buffer.append(" ");
		buffer.append(resource.getName());
		String name = buffer.toString().trim();
		name= manager.generateUniqueLaunchConfigurationNameFrom(name);
		
        try {

			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			// Python Main Tab Arguments
// E3			IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
			String location = getDefaultLocation(resource);
// E3			String baseDirectory = varManager.generateVariableExpression("workspace_loc",file.getRawLocation().removeLastSegments(1).toString());
			String baseDirectory = resource.getRawLocation().removeLastSegments(1).toString();
			String arguments = "";
			String interpreter = PydevPlugin.getInterpreterManager().getDefaultInterpreter();
			
            workingCopy.setAttribute(Constants.ATTR_PROJECT,resource.getProject().getName());
            workingCopy.setAttribute(Constants.ATTR_RESOURCE_TYPE,resource.getType());
            workingCopy.setAttribute(Constants.ATTR_INTERPRETER,interpreter);
            
            workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
			workingCopy.setAttribute(Constants.ATTR_LOCATION,location);
			workingCopy.setAttribute(Constants.ATTR_WORKING_DIRECTORY,baseDirectory);
			workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS,arguments);

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
	private ILaunchConfiguration chooseConfig(List configs) {
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
	protected void launch(IResource file, String mode, String targetAttribute) {
		if (!verifyMode(mode)){
            reportError("Invalid mode "+mode, null);
		    return;
        }

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
				String groupID = "";
			    
				if(mode.equals("run")) {
			        groupID = Constants.PYTHON_RUN_LAUNCH_GROUP;
			    }else if (mode.equals("debug")){
			        groupID = Constants.PYTHON_DEBUG_LAUNCH_GROUP;
			    }
			    
				DebugUITools.openLaunchConfigurationDialog(PydevDebugPlugin.getActiveWorkbenchWindow().getShell(), conf, groupID, null);
			} else {
				DebugUITools.launch(conf, mode);
			}
			return;
		}
		fileNotFound();
	}
	
	public void setShowDialog(boolean showDialog) {
		fShowDialog = showDialog;
	}
}
