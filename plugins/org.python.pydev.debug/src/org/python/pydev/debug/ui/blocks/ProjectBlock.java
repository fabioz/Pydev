/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
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
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.navigator.PythonLabelProvider;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;


/**
 * A control for selecting a python project.
 */
public class ProjectBlock extends AbstractLaunchConfigurationTab {

    private Text fProjectText;
    private Button fProjectBrowseButton;

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Font font = parent.getFont();
        Group group = new Group(parent, SWT.NONE);
        group.setText("Project");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);

        // Project chooser
        fProjectText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fProjectText.setLayoutData(gd);
        fProjectText.setFont(font);
        fProjectText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        fProjectBrowseButton = createPushButton(group, "Browse...", null); //$NON-NLS-1$
        fProjectBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                // Filter out project by python nature
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IProject[] projects = workspace.getRoot().getProjects();
                ArrayList<IProject> pythonProjects = new ArrayList<IProject>();
                for (IProject project : projects) {
                    try {
                        if (project.isOpen() && project.hasNature(PythonNature.PYTHON_NATURE_ID)) {
                            pythonProjects.add(project);
                        }
                    } catch (CoreException exception) {
                        Log.log(exception);
                    }

                }
                projects = pythonProjects.toArray(new IProject[pythonProjects.size()]);

                // Only allow the selection of projects, do not present directories
                ILabelProvider labelProvider = new PythonLabelProvider();
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
                dialog.setTitle("Project selection");
                dialog.setMessage("Choose a project for the run");
                dialog.setElements(projects);

                dialog.open();

                Object object = dialog.getFirstResult();
                if ((object != null) && (object instanceof IProject)) {
                    IProject project = (IProject) object;
                    PythonNature pythonNature = PythonNature.getPythonNature(project);
                    if (pythonNature == null) {
                        // The project does not have an associated python nature...
                        String msg = "The selected project must have the python nature associated.";
                        String title = "Invalid project (no python nature associated).";
                        ErrorDialog.openError(getShell(), title, msg,
                                PydevPlugin.makeStatus(IStatus.WARNING, title, null));
                    }

                    String projectName = project.getName();
                    fProjectText.setText(projectName);
                }
                updateLaunchConfigurationDialog();
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "Project";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        String projectName = "";
        try {
            projectName = configuration.getAttribute(Constants.ATTR_PROJECT, "");
        } catch (CoreException e) {
        }
        fProjectText.setText(projectName);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String value = fProjectText.getText().trim();
        setAttribute(configuration, Constants.ATTR_PROJECT, value);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        // No defaults to set
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        boolean result = super.isValid(launchConfig);

        if (result) {
            setErrorMessage(null);
            setMessage(null);

            String projectName = fProjectText.getText();
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IResource resource = workspace.getRoot().findMember(projectName);

            if (resource == null) {
                setErrorMessage("Invalid project");
                result = false;
            } else if (resource instanceof IProject) {
                IProject project = (IProject) resource;
                PythonNature nature = PythonNature.getPythonNature(project);
                if (nature == null) {
                    setErrorMessage("Invalid project (no python nature associated).");
                    result = false;
                }
            }
        }

        return result;
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

    /**
     * Adds a modification listener to the current control.
     * 
     * This is used to update the module browse button, depending 
     * on the project's python nature.
     * 
     * @param listener The listener to use
     */
    public void addModifyListener(ModifyListener listener) {
        if (fProjectText == null) {
            waitingForProjectTextToExist.add(listener);
        } else {
            fProjectText.addModifyListener(listener);
            for (ModifyListener l : waitingForProjectTextToExist) {
                fProjectText.addModifyListener(l);
            }
            waitingForProjectTextToExist.clear();
        }
    }

    List<ModifyListener> waitingForProjectTextToExist = new ArrayList<ModifyListener>();

}