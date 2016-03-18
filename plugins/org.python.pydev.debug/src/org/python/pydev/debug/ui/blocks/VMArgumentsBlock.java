/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * Editor for Interpreter arguments of a Python launch configuration.
 * 
 * Almost all of the code comes straight from JDT's VMArgumentsBlock class.
 */
public class VMArgumentsBlock extends AbstractLaunchConfigurationTab {

    // VM arguments widgets
    protected Text fVMArgumentsText;
    private Button fPgrmArgVariableButton;

    /*
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        setControl(group);
        GridLayout topLayout = new GridLayout();
        group.setLayout(topLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("VM arguments (for python.exe or java.exe): ");

        fVMArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 40;
        gd.widthHint = 100;
        fVMArgumentsText.setLayoutData(gd);
        fVMArgumentsText.setFont(font);
        fVMArgumentsText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        fPgrmArgVariableButton = createPushButton(group, "Variables...", null);
        fPgrmArgVariableButton.setFont(font);
        fPgrmArgVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        fPgrmArgVariableButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
                dialog.open();
                String variable = dialog.getVariableExpression();
                if (variable != null) {
                    fVMArgumentsText.insert(variable);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    /*
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(Constants.ATTR_VM_ARGUMENTS, (String) null);
    }

    /*
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            fVMArgumentsText.setText(configuration.getAttribute(Constants.ATTR_VM_ARGUMENTS, "")); //$NON-NLS-1$
        } catch (CoreException e) {
            setErrorMessage("Exception occurred reading configuration: " + e.getStatus().getMessage());
            PydevDebugPlugin.log(IStatus.ERROR, null, e);
        }
    }

    /*
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(Constants.ATTR_VM_ARGUMENTS, getAttributeValueFrom(fVMArgumentsText));
    }

    /*
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "VM arguments";
    }

    /**
     * Returns the string in the text widget, or <code>null</code> if empty.
     * @param text The Text to obtain the value from.
     * @return text or <code>null</code>
     */
    protected String getAttributeValueFrom(Text text) {
        String content = text.getText().trim();
        if (content.length() > 0) {
            return content;
        }
        return null;
    }

    public void setEnabled(boolean enabled) {
        fVMArgumentsText.setEnabled(enabled);
        fPgrmArgVariableButton.setEnabled(enabled);
    }
}
