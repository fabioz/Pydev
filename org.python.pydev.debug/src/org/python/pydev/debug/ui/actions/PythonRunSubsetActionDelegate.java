/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.python.pydev.debug.codecoverage.RunManyDialog;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PythonRunSubsetActionDelegate extends ActionDelegate implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private IFolder selectedFolder;

	public void run(IAction action) {
		try {
            if (part != null && selectedFolder != null) {

                String runMode = "";
            	if(action.getId().endsWith("PythonRunSubset")){
            	    runMode = ILaunchManager.RUN_MODE;
            	    
            	} else if(action.getId().endsWith("PythonCoverageSubset")){
            	    runMode = ILaunchManager.PROFILE_MODE;
            	    
            	} else{
            	    throw new RuntimeException("Unknown ");
            	}
            	
                

                RunManyDialog dialog = new RunManyDialog(part.getSite().getShell(), selectedFolder.getLocation().toString());
            	if(dialog.open() == Window.OK){
            	    String root = dialog.rootFolder;
            	    String files = dialog.files;
            	    
            	    String interpreter = dialog.interpreter;
            	    String workingDir = dialog.working;
            	    String scriptArgs = dialog.scriptArgs;    
            	    String scriptLocation = dialog.scriptLocation;
            	    boolean scriptSelected = dialog.scriptSelected;
            	    
                    String arguments= ""; //no arguments for multiple run...
            	    if(scriptSelected){
            	        arguments += root+" ";
            	        arguments += scriptArgs;
                        ILaunchConfiguration configuration = createDefaultLaunchConfiguration(selectedFolder, scriptLocation,  workingDir,  arguments,  interpreter);
                        Launch launch = new Launch(configuration,runMode, null );
                	    
                        PythonRunnerConfig config = new PythonRunnerConfig(configuration, runMode);

                        DebugUITools.launch(configuration, runMode);

            	    }else{
	            	    List list = listFilesThatMatch(root, files);
	
	                    for (Iterator iter = list.iterator(); iter.hasNext();) {
	                        Object n = iter.next();
	                        ILaunchConfiguration configuration = createDefaultLaunchConfiguration(selectedFolder, n.toString(),  workingDir,  arguments,  interpreter);
	                        Launch launch = new Launch(configuration,runMode, null );
	                	    
	                        PythonRunnerConfig config = new PythonRunnerConfig(configuration, runMode);
	
	                        DebugUITools.launch(configuration, runMode);
	                        
	                    }
            	    }
            	}
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
	}

	/**
     * @param root
     * @param files
     */
    private List listFilesThatMatch(String root, final String filesFilter) {
        List l = new ArrayList();

        File file = new File(root);
        if(file.exists()){
            FileFilter filter = new FileFilter() {

                public boolean accept(File pathname) {
                    return pathname.isDirectory() == false && pathname.getName().matches(filesFilter);
                }

            };

            l = PydevPlugin.getPyFilesBelow(file, filter, null)[0];
        }
        
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;

    }
	public void selectionChanged(IAction action, ISelection selection) {
	    selectedFolder = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedResource = structuredSelection.getFirstElement();
				if (selectedResource instanceof IFolder)
				    selectedFolder = (IFolder) selectedResource;
			}
		}
	}
	
	
	/**
	 * COPIED/MODIFIED from AntLaunchShortcut
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IFolder folder, String location, String baseDirectory, String arguments, String interpreter) {
		ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(Constants.ID_PYTHON_LAUNCH_CONFIGURATION_TYPE);
		if (type == null) {
			reportError("Python launch configuration not found", null);
			return null;
		}
		StringBuffer buffer = new StringBuffer(folder.getProject().getName());
		buffer.append(" ");
		buffer.append(folder.getName());
		String name = buffer.toString().trim();
//		name= manager.generateUniqueLaunchConfigurationNameFrom(name);
		try {

			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
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
	 * 
	 * @param folder
	 * @return
	 */
	static String getDefaultLocation (IFolder folder) {
		return folder.getRawLocation().toString();
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
			"Python pydev.debug error", "Python launch subset failed", status);
	}

}