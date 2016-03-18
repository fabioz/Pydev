/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author raul
 * @author fabioz
 */
public abstract class AbstractKeyValueDialog extends Dialog {

    /**
     * The title of the dialog.
     */
    protected String title;

    /**
     * The message to display, or <code>null</code> if none.
     */
    protected String message;

    /**
     * Ok button widget.
     */
    protected Button okButton;

    /**
     * Error message label widget.
     */
    protected Text errorMessageText;

    /**
     * Input text widget.
     */
    protected Text keyField;

    /**
     * Input text widget.
     */
    protected Text valueField;

    /**
     * The interpreter name input value
     */
    protected String finalKeyValue = null;

    /**
     * The interpreter executable input value
     */
    protected String finalValueValue = null;

    /**
     * Button id for an "Browser" button (value 64).
     */
    public int BROWSER_ID = 64;

    /**
     * Button browser label
     */
    public String BROWSER_LABEL = "Brows&e...";

    /**
     * Browser button widget.
     */
    protected Button browserButton;

    protected Listener changesValidator;

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            finalKeyValue = keyField.getText().trim();
            //Getting the absolute PATH here because we cannot work with links!
            finalValueValue = valueField.getText().trim();
        } else {
            finalKeyValue = null;
            finalValueValue = null;
        }
        super.buttonPressed(buttonId);
    }

    public AbstractKeyValueDialog(Shell shell, String dialogTitle, String dialogMessage) {
        super(shell);
        this.title = dialogTitle;
        this.message = dialogMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null)
            shell.setText(title);
    }

    /**
     * Sets or clears the error message.
     * If not <code>null</code>, the OK button is disabled.
     * 
     * @param errorMessage
     *            the error message, or <code>null</code> to clear
     */
    public void setErrorMessage(String errorMessage) {
        errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$
        okButton.setEnabled(errorMessage == null);
        errorMessageText.getParent().update();
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        main.setLayout(layout);
        main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);

        // create message
        if (message != null) {
            Label messageLabel = new Label(main, SWT.WRAP);
            messageLabel.setText(message);
            data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
            data.horizontalSpan = 3;
            data.widthHint = 150;
            messageLabel.setLayoutData(data);
        }

        Label separator = new Label(main, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        data.horizontalSpan = 4;
        separator.setText("");
        separator.setLayoutData(data);

        createFields(main);

        errorMessageText = new Text(main, SWT.READ_ONLY);
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        data.horizontalSpan = 3;
        data.widthHint = 150;
        errorMessageText.setLayoutData(data);

        applyDialogFont(main);

        return main;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        setErrorMessage(getInitialMessage());
    }

    /**
     * Creates the three widgets that represent the password entry area. 
     * @param parent  the parent of the widgets, which has two columns
     */
    protected void createFields(Composite parent) {
        changesValidator = createChangesValidator();

        new Label(parent, SWT.NONE).setText(getKeyLabelText());

        keyField = new Text(parent, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 3;
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
        keyField.setLayoutData(data);
        keyField.addListener(SWT.SELECTED, changesValidator);
        keyField.addListener(SWT.KeyDown, changesValidator);
        keyField.addListener(SWT.KeyUp, changesValidator);

        new Label(parent, SWT.NONE).setText(getValueLabelText());

        valueField = new Text(parent, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
        valueField.setLayoutData(data);
        valueField.addListener(SWT.SELECTED, changesValidator);
        valueField.addListener(SWT.KeyDown, changesValidator);
        valueField.addListener(SWT.KeyUp, changesValidator);

        browserButton = createButton(parent, BROWSER_ID, BROWSER_LABEL, true);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 1;
        data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        browserButton.setLayoutData(data);
        browserButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent evt) {
                String file = handleBrowseButton();
                if (file != null) {
                    file = file.trim();
                    setValueField(file);
                }
                changesValidator.handleEvent(null); //Make it update the error message
            }

        });
    }

    protected void setValueField(String value) {
        valueField.setText(value);
    }

    /**
     * Return the key/value inserted by the user.
     * 
     * @return the key/value inserted by the user or <code>null</code> if user canceled
     */
    public Tuple<String, String> getKeyAndValueEntered() {
        if (finalKeyValue == null || finalValueValue == null || finalKeyValue.trim().length() == 0
                || finalValueValue.trim().length() == 0) {
            return null;
        }
        return new Tuple<String, String>(finalKeyValue.trim(), finalValueValue.trim());
    }

    protected abstract String handleBrowseButton();

    protected abstract String getValueLabelText();

    protected abstract String getKeyLabelText();

    protected abstract Listener createChangesValidator();

    protected abstract String getInitialMessage();

}
