/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.MainModuleTab;
import org.python.pydev.editorinput.PySourceLocatorBase;

/**
 * A control for setting the working directory associated with a launch
 * configuration.
 *
 * Almost all of the code comes from JDT's WorkingDirectoryBlock
 */
public class WorkingDirectoryBlock extends AbstractLaunchConfigurationTab {

    private static final String DEFAULT_WORKING_DIRECTORY_TEXT = "${project_loc:/selected project name}";
    // Local directory
    private Button fWorkspaceButton;
    private Button fFileSystemButton;
    private Button fVariablesButton;

    //bug 29565 fix
    private Button fUseDefaultDirButton = null;
    private Button fUseOtherDirButton = null;
    private Text fOtherWorkingText = null;
    private Text fWorkingDirText;

    /**
     * The last launch config this tab was initialized from
     */
    private ILaunchConfiguration fLaunchConfiguration;

    /**
     * A listener to update for text changes and widget selection
     */
    private class WidgetListener extends SelectionAdapter implements ModifyListener {
        public void modifyText(ModifyEvent e) {
            if (e.getSource() == fOtherWorkingText) {

                File file = new File(fOtherWorkingText.getText());
                if (!file.exists()) {
                    setErrorMessage("The directory in the Base Directory does not exist.");
                }
                if (!file.isDirectory()) {
                    setErrorMessage("The directory in the location is not actually a directory.");
                }
            }
            updateLaunchConfigurationDialog();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            Object source = e.getSource();
            if (source == fWorkspaceButton) {
                handleWorkspaceDirBrowseButtonSelected();
            } else if (source == fFileSystemButton) {
                handleWorkingDirBrowseButtonSelected();
            } else if (source == fVariablesButton) {
                handleWorkingDirVariablesButtonSelected();
            } else if (source == fUseDefaultDirButton) {
                //only perform the action if this is the button that was selected
                if (fUseDefaultDirButton.getSelection()) {
                    setDefaultWorkingDir();
                }
            } else if (source == fUseOtherDirButton) {
                //only perform the action if this is the button that was selected
                if (fUseOtherDirButton.getSelection()) {
                    handleUseOtherWorkingDirButtonSelected();
                }
            }
        }
    }

    private WidgetListener fListener = new WidgetListener();
    private MainModuleTab mainModuleTab;

    public WorkingDirectoryBlock(MainModuleTab mainModuleTab) {
        this.mainModuleTab = mainModuleTab;
        this.mainModuleTab.fProjectBlock.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                //project modified
                updateLaunchConfigurationDialog();
            }
        });
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Font font = parent.getFont();
        Group group = createGroup(parent, "Working directory:", 2, 1, GridData.FILL_HORIZONTAL);
        setControl(group);
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(group, IJavaDebugHelpContextIds.WORKING_DIRECTORY_BLOCK);
        //default choice
        Composite comp = createComposite(group, font, 2, 2, GridData.FILL_BOTH, 0, 0);
        fUseDefaultDirButton = createRadioButton(comp, "Default:");
        fUseDefaultDirButton.addSelectionListener(fListener);
        fWorkingDirText = createSingleText(comp, 1);
        fWorkingDirText.addModifyListener(fListener);
        fWorkingDirText.setEditable(false);
        //user enter choice
        fUseOtherDirButton = createRadioButton(comp, "Other:");
        fUseOtherDirButton.addSelectionListener(fListener);
        fOtherWorkingText = createSingleText(comp, 1);
        fOtherWorkingText.addModifyListener(fListener);
        //buttons
        Composite buttonComp = createComposite(comp, font, 3, 2, GridData.HORIZONTAL_ALIGN_END);
        GridLayout ld = (GridLayout) buttonComp.getLayout();
        ld.marginHeight = 1;
        ld.marginWidth = 0;
        fWorkspaceButton = createPushButton(buttonComp, "Workspace...", null);
        fWorkspaceButton.addSelectionListener(fListener);
        fFileSystemButton = createPushButton(buttonComp, "File System...", null);
        fFileSystemButton.addSelectionListener(fListener);
        fVariablesButton = createPushButton(buttonComp, "Variables...", null);
        fVariablesButton.addSelectionListener(fListener);
    }

    /**
     * Show a dialog that lets the user select a working directory
     */
    private void handleWorkingDirBrowseButtonSelected() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setMessage("Select a working directory for the launch configuration:");
        String currentWorkingDir = getWorkingDirectoryText();
        if (!currentWorkingDir.trim().equals("")) { //$NON-NLS-1$
            File path = new File(currentWorkingDir);
            if (path.exists()) {
                dialog.setFilterPath(currentWorkingDir);
            }
        }
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            fOtherWorkingText.setText(selectedDirectory);
        }
    }

    /**
     * Show a dialog that lets the user select a working directory from 
     * the workspace
     */
    private void handleWorkspaceDirBrowseButtonSelected() {
        IContainer currentContainer = getContainer();
        if (currentContainer == null) {
            currentContainer = ResourcesPlugin.getWorkspace().getRoot();
        }
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), currentContainer, false,
                "Select a workspace relative working directory");
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] results = dialog.getResult();
        if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
            IPath path = (IPath) results[0];
            String containerName = path.makeRelative().toString();
            setOtherWorkingDirectoryText("${workspace_loc:" + containerName + "}"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Returns the selected workspace container,or <code>null</code>
     */
    protected IContainer getContainer() {
        String path = getWorkingDirectoryText();
        if (path.length() > 0) {
            IResource res = null;
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            StringSubstitution stringSubstitution = this.mainModuleTab.fMainModuleBlock.getStringSubstitution(root);
            try {
                if (stringSubstitution != null) {
                    path = stringSubstitution.performStringSubstitution(path, false);
                }
                IPath uriPath = new Path(path).makeAbsolute();
                res = new PySourceLocatorBase().getContainerForLocation(uriPath, null);
            } catch (CoreException e) {
                Log.log(e);
            }
            if (res instanceof IContainer) {
                return (IContainer) res;
            }
        }
        return null;
    }

    /**
     * The default working dir radio button has been selected.
     */
    private void handleUseDefaultWorkingDirButtonSelected() {
        fWorkspaceButton.setEnabled(false);
        fOtherWorkingText.setEnabled(false);
        fVariablesButton.setEnabled(false);
        fFileSystemButton.setEnabled(false);
        fUseOtherDirButton.setSelection(false);
        fWorkingDirText.setEnabled(true);
    }

    /**
     * The other working dir radio button has been selected
     * 
     * @since 3.2
     */
    private void handleUseOtherWorkingDirButtonSelected() {
        fOtherWorkingText.setEnabled(true);
        fWorkspaceButton.setEnabled(true);
        fVariablesButton.setEnabled(true);
        fFileSystemButton.setEnabled(true);
        fWorkingDirText.setEnabled(false);
        updateLaunchConfigurationDialog();
    }

    /**
     * The working dir variables button has been selected
     */
    private void handleWorkingDirVariablesButtonSelected() {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variableText = dialog.getVariableExpression();
        if (variableText != null) {
            fOtherWorkingText.insert(variableText);
        }
    }

    /**
     * Sets the default working directory
     */
    protected void setDefaultWorkingDir() {
        setDefaultWorkingDirectoryText(DEFAULT_WORKING_DIRECTORY_TEXT);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);
        setMessage(null);
        // if variables are present, we cannot resolve the directory
        String workingDirPath = getWorkingDirectoryText();
        if (workingDirPath.indexOf("${") >= 0) { //$NON-NLS-1$
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            try {
                manager.validateStringVariables(workingDirPath);
            } catch (CoreException e) {
                setErrorMessage(e.getMessage());
                return false;
            }
        } else if (workingDirPath.length() > 0) {
            IContainer container = getContainer();
            if (container == null) {
                File dir = new File(workingDirPath);
                if (dir.isDirectory()) {
                    return true;
                }
                setErrorMessage("Only directories can be selected");
                return false;
            }
        } else if (workingDirPath.length() == 0) {
            setErrorMessage("A non-empty directory must be selected");
            return false;
        }
        return true;
    }

    /**
     * Defaults are empty.
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(Constants.ATTR_WORKING_DIRECTORY, (String) null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        setLaunchConfiguration(configuration);
        try {
            String wd = configuration.getAttribute(Constants.ATTR_WORKING_DIRECTORY, (String) null);
            String owd = configuration.getAttribute(Constants.ATTR_OTHER_WORKING_DIRECTORY, (String) null);
            setDefaultWorkingDir();

            if ((wd != null && wd.equals(owd)) || owd == null) {
                //will set the default as the other working directory text
                setOtherWorkingDirectoryText(wd);
            } else {
                fOtherWorkingText.setText(owd);
            }
        } catch (CoreException e) {
            setErrorMessage("Exception occurred reading configuration" + e.getStatus().getMessage());
            Log.log(e);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {

        configuration.setAttribute(Constants.ATTR_OTHER_WORKING_DIRECTORY, fOtherWorkingText.getText().trim());

        if (fUseDefaultDirButton.getSelection()) {
            configuration.setAttribute(Constants.ATTR_WORKING_DIRECTORY, (String) null);
        } else {
            configuration.setAttribute(Constants.ATTR_WORKING_DIRECTORY, getWorkingDirectoryText());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Working Directory";
    }

    /**
     * gets the path from the text box that is selected
     * @return the working directory the user wishes to use
     * @since 3.2
     */
    protected String getWorkingDirectoryText() {
        if (fUseDefaultDirButton.getSelection()) {
            return fWorkingDirText.getText().trim();
        }
        return fOtherWorkingText.getText().trim();
    }

    /**
     * sets the default working directory text
     * @param dir the dir to set the widget to
     * @since 3.2
     */
    protected void setDefaultWorkingDirectoryText(String dir) {
        if (dir != null) {
            fWorkingDirText.setText(dir);
            fUseDefaultDirButton.setSelection(true);
            fUseOtherDirButton.setSelection(false);
            handleUseDefaultWorkingDirButtonSelected();
        }
    }

    /**
     * sets the other dir text
     * @param dir the new text
     * @since 3.2
     */
    protected void setOtherWorkingDirectoryText(String dir) {
        if (dir != null) {
            fOtherWorkingText.setText(dir);
            fUseOtherDirButton.setSelection(true);
            fUseDefaultDirButton.setSelection(false);
            handleUseOtherWorkingDirButtonSelected();
        }
    }

    /**
     * Sets the project currently specified by the
     * given launch config, if any.
     */
    private void setLaunchConfiguration(ILaunchConfiguration config) {
        fLaunchConfiguration = config;
    }

    /**
     * Returns the current project context
     */
    private ILaunchConfiguration getLaunchConfiguration() {
        return fLaunchConfiguration;
    }

    /**
     * Creates a new text widget 
     * @param parent the parent composite to add this text widget to
     * @param hspan the horizontal span to take up on the parent composite
     * @return the new text widget
     */
    private static Text createSingleText(Composite parent, int hspan) {
        Text t = new Text(parent, SWT.SINGLE | SWT.BORDER);
        t.setFont(parent.getFont());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = hspan;
        t.setLayoutData(gd);
        return t;
    }

    /**
     * Creates a Group widget
     * @param parent the parent composite to add this group to
     * @param text the text for the heading of the group
     * @param columns the number of columns within the group
     * @param hspan the horizontal span the group should take up on the parent
     * @param fill the style for how this composite should fill into its parent
     * Can be one of <code>GridData.FILL_HORIZONAL</code>, <code>GridData.FILL_BOTH</code> or <code>GridData.FILL_VERTICAL</code>
     * @return the new group
     */
    private static Group createGroup(Composite parent, String text, int columns, int hspan, int fill) {
        Group g = new Group(parent, SWT.NONE);
        g.setLayout(new GridLayout(columns, false));
        g.setText(text);
        g.setFont(parent.getFont());
        GridData gd = new GridData(fill);
        gd.horizontalSpan = hspan;
        g.setLayoutData(gd);
        return g;
    }

    /**
     * Creates a Composite widget
     * @param parent the parent composite to add this composite to
     * @param columns the number of columns within the composite
     * @param hspan the horizontal span the composite should take up on the parent
     * @param fill the style for how this composite should fill into its parent
     * Can be one of <code>GridData.FILL_HORIZONAL</code>, <code>GridData.FILL_BOTH</code> or <code>GridData.FILL_VERTICAL</code>
     * @return the new group
     */
    private static Composite createComposite(Composite parent, Font font, int columns, int hspan, int fill) {
        Composite g = new Composite(parent, SWT.NONE);
        g.setLayout(new GridLayout(columns, false));
        g.setFont(font);
        GridData gd = new GridData(fill);
        gd.horizontalSpan = hspan;
        g.setLayoutData(gd);
        return g;
    }

    /**
     * Creates a Composite widget
     * @param parent the parent composite to add this composite to
     * @param columns the number of columns within the composite
     * @param hspan the horizontal span the composite should take up on the parent
     * @param fill the style for how this composite should fill into its parent
     * Can be one of <code>GridData.FILL_HORIZONAL</code>, <code>GridData.FILL_BOTH</code> or <code>GridData.FILL_VERTICAL</code>
     * @param marginwidth the width of the margin to place around the composite (default is 5, specified by GridLayout)
     * @param marginheight the height of the margin to place around the composite (default is 5, specified by GridLayout)
     * @return the new group
     */
    private static Composite createComposite(Composite parent, Font font, int columns, int hspan, int fill,
            int marginwidth, int marginheight) {
        Composite g = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(columns, false);
        layout.marginWidth = marginwidth;
        layout.marginHeight = marginheight;
        g.setLayout(layout);
        g.setFont(font);
        GridData gd = new GridData(fill);
        gd.horizontalSpan = hspan;
        g.setLayoutData(gd);
        return g;
    }

    /**
     * Allows this entire block to be enabled/disabled
     * @param enabled whether to enable it or not
     */
    protected void setEnabled(boolean enabled) {
        fUseDefaultDirButton.setEnabled(enabled);
        fUseOtherDirButton.setEnabled(enabled);
        if (fOtherWorkingText.isEnabled()) {
            fOtherWorkingText.setEnabled(enabled);
            fWorkspaceButton.setEnabled(enabled);
            fVariablesButton.setEnabled(enabled);
            fFileSystemButton.setEnabled(enabled);
        }
        // in the case where the 'other' text is selected and we want to enable
        if (fUseOtherDirButton.getSelection() && enabled == true) {
            fOtherWorkingText.setEnabled(enabled);
        }
    }
}
