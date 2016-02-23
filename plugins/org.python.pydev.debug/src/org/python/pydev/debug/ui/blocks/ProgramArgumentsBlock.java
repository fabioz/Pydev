/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import org.eclipse.core.runtime.CoreException;
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
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;


/**
 * A control for setting the working directory associated with a launch
 * configuration.
 * 
 * Most of the code comes from JDT's JavaArgumentsTab class.
 */
public class ProgramArgumentsBlock extends AbstractLaunchConfigurationTab {

    private Text fPrgmArgumentsText;

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {

        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        group.setFont(font);
        GridLayout gridLayout = new GridLayout();
        group.setLayout(gridLayout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        String controlName = "Program arguments: ";
        group.setText(controlName);

        fPrgmArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 40;
        gd.widthHint = 100;
        fPrgmArgumentsText.setLayoutData(gd);
        fPrgmArgumentsText.setFont(font);
        fPrgmArgumentsText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        String buttonLabel = "Variables...";
        Button pgrmArgVariableButton = createPushButton(group, buttonLabel, null);

        pgrmArgVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        pgrmArgVariableButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
                dialog.open();
                String variable = dialog.getVariableExpression();
                if (variable != null) {
                    fPrgmArgumentsText.insert(variable);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "Program arguments";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            String text = configuration.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "");
            fPrgmArgumentsText.setText(text);
        } catch (CoreException e) {
            setErrorMessage("Exception occurred reading configuration" + e.getStatus().getMessage());
            Log.log(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, fPrgmArgumentsText.getText().trim());
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        // No defaults to set
    }
}
