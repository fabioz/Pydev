/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.FileOrResource;
import org.python.pydev.debug.ui.launching.LaunchConfigurationCreator;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.dialogs.PythonModulePickerDialog;

/**
 * A control for selecting a python module.
 */
public class MainModuleBlock extends AbstractLaunchConfigurationTab {

    private Text fMainModuleText;
    private Button fMainModuleBrowseButton;
    private String fProjectName;
    private ModifyListener fProjectModifyListener;
    private boolean fUnitTesting;

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        setControl(group);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        group.setLayout(topLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("Main Module");

        fMainModuleText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fMainModuleText.setLayoutData(gd);
        fMainModuleText.setFont(font);
        fMainModuleText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        final Composite lParent = parent;
        fMainModuleBrowseButton = createPushButton(group, "Browse...", null);

        // On button click, this displays the python module picker dialog.
        fMainModuleBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IResource[] currentResources = getMainModuleResources();
                IResource resource = workspace.getRoot().findMember(fProjectName);

                if (resource instanceof IProject) {
                    IProject project = (IProject) resource;
                    String title, message;
                    if (!fUnitTesting) {
                        title = "Main Module";
                        message = "Choose Python module which starts execution";
                    }
                    else
                    {
                        title = "Main Modules";
                        message = "Choose Python module(s) and/or package(s) to test";
                    }
                    PythonModulePickerDialog dialog = new PythonModulePickerDialog(lParent.getShell(), title,
                            message, project, fUnitTesting);

                    // Fixed request 1407469: main module browse button forgets path                    
                    if (currentResources != null) {
                        dialog.setInitialSelections(currentResources);
                    }

                    int result = dialog.open();
                    if (result == PythonModulePickerDialog.OK) {
                        Object results[] = dialog.getResult();
                        if ((results != null) && (results.length > 0)) {

                            ArrayList<IResource> r_results = new ArrayList<IResource>();

                            for (int i = 0; i < results.length; i++) {
                                if (results[i] instanceof IResource) {
                                    if (results[i] instanceof IFile) {
                                        r_results.add((IFile) results[i]);
                                    }
                                    else {
                                        r_results.add((IResource) results[i]);
                                    }
                                }
                            }
                            fMainModuleText.setText(LaunchConfigurationCreator.getDefaultLocation(
                                    FileOrResource.createArray(r_results.toArray(new IResource[r_results.size()])),
                                    true));
                        }
                    }
                }
            }
        });

        // Create a ModifyListener, used to listen for project modifications in the ProjectBlock. 
        // This assumes that the Project is in a Text control...
        fProjectModifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                Widget widget = e.widget;
                if (widget instanceof Text) {
                    Text text = (Text) widget;
                    fProjectName = text.getText();
                    IWorkspace workspace = ResourcesPlugin.getWorkspace();
                    IResource resource = workspace.getRoot().findMember(fProjectName);

                    boolean enabled = false;
                    if ((resource != null) && (resource instanceof IProject)) {
                        IProject project = (IProject) resource;
                        PythonNature nature = PythonNature.getPythonNature(project);
                        enabled = (nature != null);
                    }

                    fMainModuleBrowseButton.setEnabled(enabled);
                }
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Main module";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {

        // Initialize the location field
        String location = "";
        try {
            location = configuration.getAttribute(Constants.ATTR_LOCATION, "");
        } catch (CoreException e) {
        }
        fMainModuleText.setText(location);

        // Obtain a copy of the project name (not displayed)
        String projectName = "";
        try {
            projectName = configuration.getAttribute(Constants.ATTR_PROJECT, "");
        } catch (CoreException e) {
        }
        fProjectName = projectName;

        try {
            String identifier = configuration.getType().getIdentifier(); //configuration.getType().getIdentifier();
            fUnitTesting = (identifier.equals(Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE)
                    || identifier.equals(Constants.ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE)
                    || identifier.equals(Constants.ID_IRONPYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE)
                    || identifier.equals(Constants.ID_PYTHON_COVERAGE_LAUNCH_CONFIGURATION_TYPE));
        } catch (CoreException e) {
            setErrorMessage("Unable to resolve location");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String value = fMainModuleText.getText().trim();
        setAttribute(configuration, Constants.ATTR_LOCATION, value);
        configuration.setMappedResources(getMainModuleResources());
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        //no defaults to set
    }

    /**
     * Obtains an IFile that targets the current main module.
     * 
     * This is used for initializing the module selection dialog.
     * 
     * @return The main module file. 
     */
    private IResource[] getMainModuleResources() {
        String path = fMainModuleText.getText();
        ArrayList<IResource> resourceList = new ArrayList<IResource>();
        if (path.length() > 0) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

            IPath projectPath = new Path(null, fProjectName).makeAbsolute();
            if (projectPath.segmentCount() != 1) {
                return null;
            }

            IResource resource = root.getProject(fProjectName);
            IProject project = null;
            if (resource != null) {
                project = resource.getProject();
            }

            StringSubstitution stringSubstitution = getStringSubstitution(root);
            if (stringSubstitution != null) {
                try {
                    //may have multiple files selected for the run for unittest and code-coverage
                    for (String loc : StringUtils.splitAndRemoveEmptyTrimmed(path, '|')) {
                        String onepath = stringSubstitution.performStringSubstitution(loc, false);
                        IFile f = new PySourceLocatorBase().getFileForLocation(Path.fromOSString(onepath), project);
                        if (f != null) {
                            resourceList.add(f);
                            continue;
                        }
                        IContainer container = new PySourceLocatorBase().getContainerForLocation(
                                Path.fromOSString(onepath),
                                project);
                        if (container != null) {
                            resourceList.add(container);
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }

        }
        if (resourceList.isEmpty()) {
            return null;
        }
        return resourceList.toArray(new IResource[resourceList.size()]);
    }

    /**
     * @param root the workspace root.
     * @return an object capable on making string substitutions based on variables in the project and in the workspace.
     */
    public StringSubstitution getStringSubstitution(IWorkspaceRoot root) {
        IPath projectPath = new Path(null, fProjectName).makeAbsolute();
        if (projectPath.segmentCount() != 1) {
            // Path for project must have (only) one segment.
            return null;
        }

        IProject resource = root.getProject(fProjectName);
        IPythonNature nature = null;
        if (resource != null) {
            nature = PythonNature.getPythonNature(resource);
        }

        StringSubstitution stringSubstitution = new StringSubstitution(nature);
        return stringSubstitution;
    }

    /**
     * Sets attributes in the working copy
     * 
     * @param configuration The configuration to set the attribute in
     * @param name Name of the attribute to set
     * @param value Value to set 
     */
    private void setAttribute(ILaunchConfigurationWorkingCopy configuration, String name, String value) {
        if (value == null || value.length() == 0) {
            configuration.setAttribute(name, (String) null);
        } else {
            configuration.setAttribute(name, value);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        boolean result = super.isValid(launchConfig);

        if (result) {
            setMessage(null);
            setErrorMessage(null);

            IPath projectPath = new Path(null, fProjectName).makeAbsolute();
            if (projectPath.segmentCount() != 1) {
                String message = "Path for project must have (only) one segment."; //$NON-NLS-1$
                setErrorMessage(message);
                return false;
            }

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            StringSubstitution stringSubstitution = getStringSubstitution(root);
            if (stringSubstitution == null) {
                String message = "Unable to get StringSubstitution (shouldn't happen)."; //$NON-NLS-1$
                setErrorMessage(message);
                return false;
            }

            String location = fMainModuleText.getText();
            try {

                if (fUnitTesting) {

                    //may have  multiple files selected for the run for unitest and code-coverage
                    for (String loc : StringUtils.splitAndRemoveEmptyTrimmed(location, '|')) {
                        String expandedLocation = stringSubstitution.performStringSubstitution(loc);
                        File file = new File(expandedLocation);
                        if (!file.exists()) {
                            setErrorMessage(StringUtils.format(
                                    "The file \"%s\" does not exist.", file));
                            result = false;
                            break;
                        }

                    }
                } else {
                    String expandedLocation = stringSubstitution.performStringSubstitution(location);
                    File file = new File(expandedLocation);
                    if (!file.exists()) {
                        setErrorMessage(StringUtils.format(
                                "The file \"%s\" does not exist.", file));
                        result = false;

                    } else if (!file.isFile()) {

                        File mainModule = new File(expandedLocation + File.separator + "__main__.py");

                        if (!mainModule.isFile()) {
                            setErrorMessage(StringUtils.format(
                                    "The file \"%s\" does not actually map to a file.", file));
                            result = false;
                        }
                    }
                }

            } catch (CoreException e) {
                setErrorMessage("Unable to resolve location");
                result = false;
            }
        }
        return result;
    }

    /**
     * Obtain a listener, used to detect changes of the currently selected project
     * This updates the browse button, and allos the appropriate selection of the main module.
     *  
     * @return a ModifyListener that updates the block controls.
     */
    public ModifyListener getProjectModifyListener() {
        return fProjectModifyListener;
    }
}
