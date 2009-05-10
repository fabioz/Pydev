/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.python.pydev.core.IInterpreterManager;
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

    //=============================================================================================
    // ILaunchShortcut IMPL
    //=============================================================================================
    @SuppressWarnings("unchecked")
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            
            //single selection
            if (structuredSelection.size() == 1) {
                Object object = structuredSelection.getFirstElement();
                if (object instanceof IAdaptable) {

                    IFile resource = (IFile) ((IAdaptable) object).getAdapter(IFile.class);
                    if (resource != null) {
                        launch(resource, mode, null);
                        return;
                    }

                    IFolder folder = (IFolder) ((IAdaptable) object).getAdapter(IFolder.class);
                    if (folder != null) {
                        launch(folder, mode, null);
                        return;
                    }
                }

            //multiple selection
            } else if (structuredSelection.size() > 1) {
                List<IResource> sel = new ArrayList<IResource>();
                for (Iterator<Object> it = structuredSelection.iterator(); it.hasNext();) {
                    Object object = it.next();
                    if (object instanceof IAdaptable) {
                        IFolder folder = (IFolder) ((IAdaptable) object).getAdapter(IFolder.class);
                        if (folder != null) {
                            sel.add(folder);
                        }else{
                            IFile file = (IFile) ((IAdaptable) object).getAdapter(IFile.class);
                            if(file != null){
                                sel.add(file);
                            }
                        }
                    }
                }
                if (sel.size() > 0) {
                    launch(sel.toArray(new IResource[sel.size()]), mode, null);
                }
                return;
            }
            StringBuffer buf = new StringBuffer();
            for (Iterator<Object> it = structuredSelection.iterator(); it.hasNext();) {
                buf.append(it.next());
            }
            reportError("Unable to discover launch config for: "+buf, null);
            return;
        }else{
            PydevPlugin.log("Expecting instance of IStructuredSelection. Received: "+selection.getClass().getName());
        }
        
    }

    /**
     * Launch for a selected editor.
     */
    public void launch(IEditorPart editor, String mode) {
        //we have an editor to run
        IEditorInput input = editor.getEditorInput();
        IFile file = (IFile) input.getAdapter(IFile.class);
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
        String msg = "Unable to launch the file. " + 
                "Possible reasons may include:\n" +
                "    - the file (editor) being launched is not under a project in the workspace;\n" + 
                "    - the file was deleted.";
        reportError(msg, null);
    }

    /**
     * Report some error to the user.
     */
    protected static void reportError(String message, Throwable throwable) {
        if (message == null){
            message = "Unexpected error";
        }
        
        IStatus status = null;
        if (throwable instanceof CoreException) {
            status = ((CoreException) throwable).getStatus();
        } else {
            status = new Status(IStatus.ERROR, "org.python.pydev.debug", 0, message, throwable);
        }
        ErrorDialog.openError(PydevDebugPlugin.getActiveWorkbenchWindow().getShell(), "Python pydev.debug error", "Python launch failed",
                status);
    }

    /**
     * COPIED/MODIFIED from AntLaunchShortcut
     * Returns a list of existing launch configuration for the given file.
     */
    protected List<ILaunchConfiguration> findExistingLaunchConfigurations(IResource[] file) {
        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
        List<ILaunchConfiguration> validConfigs = new ArrayList<ILaunchConfiguration>();
        if (type == null) {
            return validConfigs;
        }

        try {
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
            
            //let's see if we can find it with a location relative or not.
            String defaultLocation = LaunchConfigurationCreator.getDefaultLocation(file, true);
            String defaultLocation2 = LaunchConfigurationCreator.getDefaultLocation(file, false);
            
            for (int i = 0; i < configs.length; i++) {
                String configPath = configs[i].getAttribute(Constants.ATTR_LOCATION, "");
                if (defaultLocation.equals(configPath) || defaultLocation2.equals(configPath)) {
                    validConfigs.add(configs[i]);
                }
            }
        } catch (CoreException e) {
            reportError("Unexpected error", e);
        }
        return validConfigs;
    }

    /**
     * @return a string with the launch configuration type that should be used for the run.
     */
    protected abstract String getLaunchConfigurationType();

    protected ILaunchConfiguration createDefaultLaunchConfiguration(IResource[] resource) {
        try {
            ILaunchConfigurationWorkingCopy createdConfiguration = createDefaultLaunchConfigurationWithoutSaving(resource);
            return createdConfiguration.doSave();
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }

    protected ILaunchConfigurationWorkingCopy createDefaultLaunchConfigurationWithoutSaving(IResource[] resource)
            throws CoreException{
        IInterpreterManager pythonInterpreterManager = getInterpreterManager();
        String projName = resource[0].getProject().getName();
        ILaunchConfigurationWorkingCopy createdConfiguration = LaunchConfigurationCreator
                .createDefaultLaunchConfiguration(resource, getLaunchConfigurationType(),
                        LaunchConfigurationCreator.getDefaultLocation(resource, false), //it'll be made relative later on
                        pythonInterpreterManager, projName);

        // Common Tab Arguments
        CommonTab tab = new CommonTab();
        tab.setDefaults(createdConfiguration);
        tab.dispose();
        return createdConfiguration;
    }

    /**
     * @return the interpreter manager associated with this shortcut (may be overridden if it is not python)
     */
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getPythonInterpreterManager();
    }

    /**
     * COPIED/MODIFIED from AntLaunchShortcut
     */
    protected ILaunchConfiguration chooseConfig(List<ILaunchConfiguration> configs) {
        if (configs.isEmpty()) {
            return null;
        }
        ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
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

    protected void launch(IResource file, String mode, String targetAttribute) {
        launch(new IResource[] { file }, mode, targetAttribute);
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
    protected void launch(IResource[] file, String mode, String targetAttribute) {
        ILaunchConfiguration conf = null;
        List<ILaunchConfiguration> configurations = findExistingLaunchConfigurations(file);
        if (configurations.isEmpty())
            conf = createDefaultLaunchConfiguration(file);
        else {
            if (configurations.size() == 1) {
                conf = configurations.get(0);
            } else {
                conf = chooseConfig(configurations);
                if (conf == null){
                    // User cancelled selection
                    return;
                }
            }
        }

        if (conf != null) {
            DebugUITools.launch(conf, mode);
            return;
        }
        fileNotFound();
    }

}
