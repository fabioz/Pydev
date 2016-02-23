/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.blocks.MainModuleBlock;
import org.python.pydev.debug.ui.blocks.ProjectBlock;
import org.python.pydev.debug.ui.blocks.PythonPathBlock;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Tab where user chooses project and Python module for launch
 * 
 * <p>
 * Also show PYTHONPATH information
 * </p>
 * 
 * TODO: Fix code completion job scheduling problem with this tab.
 * Show progress dialog when ASTManager and thus PYTHONPATH information
 * is not yet available.
 * 
 * @author Mikko Ohtamaa
 */
public class MainModuleTab extends AbstractLaunchConfigurationTab {

    // Widget blocks
    public final ProjectBlock fProjectBlock;
    public final MainModuleBlock fMainModuleBlock;
    public final PythonPathBlock fPythonPathBlock;

    public MainModuleTab() {
        fProjectBlock = new ProjectBlock();
        fMainModuleBlock = new MainModuleBlock();
        fPythonPathBlock = new PythonPathBlock();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);
        GridLayout gridLayout = new GridLayout();
        composite.setLayout(gridLayout);

        fProjectBlock.createControl(composite);
        fMainModuleBlock.createControl(composite);
        fPythonPathBlock.createControl(composite);

        // Add a modify listener, that updates the module block
        // when the selected project changes
        fProjectBlock.addModifyListener(fMainModuleBlock.getProjectModifyListener());
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
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        fProjectBlock.initializeFrom(configuration);
        fMainModuleBlock.initializeFrom(configuration);
        fPythonPathBlock.initializeFrom(configuration);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        fProjectBlock.performApply(configuration);
        fMainModuleBlock.performApply(configuration);
        fPythonPathBlock.performApply(configuration);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#setLaunchConfigurationDialog(org.eclipse.debug.ui.ILaunchConfigurationDialog)
     */
    @Override
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
        super.setLaunchConfigurationDialog(dialog);

        fProjectBlock.setLaunchConfigurationDialog(dialog);
        fMainModuleBlock.setLaunchConfigurationDialog(dialog);
        fPythonPathBlock.setLaunchConfigurationDialog(dialog);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        String result = super.getErrorMessage();

        if (result == null) {
            result = fProjectBlock.getErrorMessage();
        }

        if (result == null) {
            result = fMainModuleBlock.getErrorMessage();
        }

        if (result == null) {
            result = fPythonPathBlock.getErrorMessage();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getMessage()
     */
    @Override
    public String getMessage() {
        String result = super.getMessage();

        if (result == null) {
            result = fProjectBlock.getMessage();
        }

        if (result == null) {
            result = fMainModuleBlock.getMessage();
        }

        if (result == null) {
            result = fPythonPathBlock.getMessage();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        boolean result = super.isValid(launchConfig);

        if (result) {
            result = fProjectBlock.isValid(launchConfig);
        }

        if (result) {
            result = fMainModuleBlock.isValid(launchConfig);
        }

        if (result) {
            result = fPythonPathBlock.isValid(launchConfig);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "Main";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
     */
    @Override
    public Image getImage() {
        return PydevPlugin.getImageCache().get(Constants.MAIN_ICON);
    }
}
