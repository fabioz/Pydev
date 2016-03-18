/******************************************************************************
* Copyright (C) 2011-2012  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;
import org.python.pydev.debug.model.PyPropertyTraceManager;

public class PyPropertyTraceDialog extends SelectionDialog {

    // checkboxes to enable/disable stepping into properties
    private Button disableStepIntoPropertiesCheck;
    private Button disableStepIntoGetterCheck;
    private Button disableStepIntoSetterCheck;
    private Button disableStepIntoDeleterCheck;

    // By default user can step into properties
    private boolean disableStepIntoProperties = false;
    private boolean disableStepIntoGetter = false;
    private boolean disableStepIntoSetter = false;
    private boolean disableStepIntoDeleter = false;

    public PyPropertyTraceDialog(Shell parentShell) {
        super(parentShell);
    }

    /**
     * Adds the checkboxes to the dialog
     * 
     * @param composite
     */
    private void createStepIntoOptions(Composite composite) {

        List<String> pyPropertyTraceStatesList = ConfigureExceptionsFileUtils
                .getConfiguredExceptions(PyPropertyTraceManager.PROPERTY_TRACE_STATE);

        if (pyPropertyTraceStatesList.size() == 4) {
            disableStepIntoProperties = Boolean.parseBoolean(pyPropertyTraceStatesList.get(0));
            disableStepIntoGetter = Boolean.parseBoolean(pyPropertyTraceStatesList.get(1));
            disableStepIntoSetter = Boolean.parseBoolean(pyPropertyTraceStatesList.get(2));
            disableStepIntoDeleter = Boolean.parseBoolean(pyPropertyTraceStatesList.get(3));
        }

        disableStepIntoPropertiesCheck = new Button(composite, SWT.CHECK);
        disableStepIntoPropertiesCheck.setText("Disable step into properties");
        disableStepIntoPropertiesCheck.setSelection(disableStepIntoProperties);
        createSelectionListener();

        GridData gridData = new GridData();
        gridData.horizontalIndent = 25;
        disableStepIntoGetterCheck = new Button(composite, SWT.CHECK);
        disableStepIntoGetterCheck.setText("Disable step into property getters");
        disableStepIntoGetterCheck.setSelection(disableStepIntoGetter);
        disableStepIntoGetterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
        disableStepIntoGetterCheck.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalIndent = 25;
        disableStepIntoSetterCheck = new Button(composite, SWT.CHECK);
        disableStepIntoSetterCheck.setText("Disable step into property setters");
        disableStepIntoSetterCheck.setSelection(disableStepIntoSetter);
        disableStepIntoSetterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
        disableStepIntoSetterCheck.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalIndent = 25;
        disableStepIntoDeleterCheck = new Button(composite, SWT.CHECK);
        disableStepIntoDeleterCheck.setText("Disable step into property deleters");
        disableStepIntoDeleterCheck.setSelection(disableStepIntoDeleter);
        disableStepIntoDeleterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
        disableStepIntoDeleterCheck.setLayoutData(gridData);
    }

    /**
     * Creates a selection listener for disableStepIntoPropertiesCheck
     */
    private void createSelectionListener() {
        disableStepIntoPropertiesCheck.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                disableStepIntoGetterCheck.setSelection(false);
                disableStepIntoGetterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
                disableStepIntoSetterCheck.setSelection(false);
                disableStepIntoSetterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
                disableStepIntoDeleterCheck.setSelection(false);
                disableStepIntoDeleterCheck.setEnabled(disableStepIntoPropertiesCheck.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        createStepIntoOptions(composite);
        return composite;
    }

    @Override
    protected void okPressed() {
        this.disableStepIntoProperties = disableStepIntoPropertiesCheck.getSelection();
        this.disableStepIntoGetter = disableStepIntoGetterCheck.getSelection();
        this.disableStepIntoSetter = disableStepIntoSetterCheck.getSelection();
        this.disableStepIntoDeleter = disableStepIntoDeleterCheck.getSelection();
        super.okPressed();
    }

    // Getters

    public boolean isDisableStepIntoProperties() {
        return disableStepIntoProperties;
    }

    public boolean isDisableStepIntoGetter() {
        return disableStepIntoGetter;
    }

    public boolean isDisableStepIntoSetter() {
        return disableStepIntoSetter;
    }

    public boolean isDisableStepIntoDeleter() {
        return disableStepIntoDeleter;
    }
}
