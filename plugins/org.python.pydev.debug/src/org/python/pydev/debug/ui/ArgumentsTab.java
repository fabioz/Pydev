/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Author: fabioz
 * Created: Aug 20, 2003
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
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.blocks.ProgramArgumentsBlock;
import org.python.pydev.debug.ui.blocks.VMArgumentsBlock;
import org.python.pydev.debug.ui.blocks.WorkingDirectoryBlock;

/**
 * The main Python debug setup tab.
 * 
 * This tab contains:
 * <ul>
 *   <li>The program arguments block</li> 
 *   <li>The VM Arguments block</li>
 *   <li>The Working directory block</li>
 * </ul>
 */
public class ArgumentsTab extends AbstractLaunchConfigurationTab {

    // Widget blocks
    private AbstractLaunchConfigurationTab workingDirectoryBlock;
    private AbstractLaunchConfigurationTab vmArgumentsBlock;
    private AbstractLaunchConfigurationTab programArgumentsBlock;

    public ArgumentsTab(MainModuleTab mainModuleTab) {
        programArgumentsBlock = createProgramArgumentsBlock(mainModuleTab);
        vmArgumentsBlock = createVmArgumentsBlock(mainModuleTab);
        workingDirectoryBlock = createWorkingDirectoryBlock(mainModuleTab);
    }

    protected AbstractLaunchConfigurationTab createWorkingDirectoryBlock(MainModuleTab mainModuleTab) {
        return new WorkingDirectoryBlock(mainModuleTab);
    }

    protected AbstractLaunchConfigurationTab createVmArgumentsBlock(MainModuleTab mainModuleTab) {
        return new VMArgumentsBlock();
    }

    protected AbstractLaunchConfigurationTab createProgramArgumentsBlock(MainModuleTab mainModuleTab) {
        return new ProgramArgumentsBlock();
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        GridLayout gridLayout = new GridLayout();
        comp.setLayout(gridLayout);

        programArgumentsBlock.createControl(comp);
        vmArgumentsBlock.createControl(comp);
        workingDirectoryBlock.createControl(comp);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);
        setMessage(null);

        if (!programArgumentsBlock.isValid(launchConfig)) {
            return false;
        }

        if (!vmArgumentsBlock.isValid(launchConfig)) {
            return false;
        }

        if (!workingDirectoryBlock.isValid(launchConfig)) {
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "Arguments";
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    @Override
    public Image getImage() {
        return PydevDebugPlugin.getImageCache().get(Constants.ARGUMENTS_ICON);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
        // No defaults to set 
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        programArgumentsBlock.initializeFrom(configuration);
        vmArgumentsBlock.initializeFrom(configuration);
        workingDirectoryBlock.initializeFrom(configuration);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        programArgumentsBlock.performApply(configuration);
        vmArgumentsBlock.performApply(configuration);
        workingDirectoryBlock.performApply(configuration);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
     */
    @Override
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
        super.setLaunchConfigurationDialog(dialog);
        programArgumentsBlock.setLaunchConfigurationDialog(dialog);
        workingDirectoryBlock.setLaunchConfigurationDialog(dialog);
        vmArgumentsBlock.setLaunchConfigurationDialog(dialog);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        String result = super.getErrorMessage();

        if (result == null) {
            result = programArgumentsBlock.getErrorMessage();
        }

        if (result == null) {
            result = workingDirectoryBlock.getErrorMessage();
        }

        if (result == null) {
            result = vmArgumentsBlock.getErrorMessage();
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
            result = programArgumentsBlock.getMessage();
        }

        if (result == null) {
            result = workingDirectoryBlock.getMessage();
        }

        if (result == null) {
            result = vmArgumentsBlock.getMessage();
        }

        return result;
    }
}
