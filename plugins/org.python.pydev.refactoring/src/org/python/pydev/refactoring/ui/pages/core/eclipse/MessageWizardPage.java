/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/* Copied from the eclipse project, the original file wasn't exported. rschuett */

package org.python.pydev.refactoring.ui.pages.core.eclipse;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public abstract class MessageWizardPage extends UserInputWizardPage {

    public static final int STYLE_NONE = 0;
    public static final int STYLE_INFORMATION = 1;
    public static final int STYLE_QUESTION = 2;
    public static final int STYLE_ERROR = 3;
    public static final int STYLE_WARNING = 4;

    private final int fStyle;

    public MessageWizardPage(String pageName, boolean isLastUserPage, int style) {
        super(pageName);
        fStyle = style;
    }

    protected abstract String getMessageString();

    protected Image getMessageImage() {
        switch (fStyle) {
            case STYLE_ERROR:
                return Display.getCurrent().getSystemImage(SWT.ICON_ERROR);
            case STYLE_WARNING:
                return Display.getCurrent().getSystemImage(SWT.ICON_WARNING);
            case STYLE_INFORMATION:
                return Display.getCurrent().getSystemImage(SWT.ICON_INFORMATION);
            case STYLE_QUESTION:
                return Display.getCurrent().getSystemImage(SWT.ICON_QUESTION);
            default:
                return null;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        Composite result = new Composite(parent, SWT.NONE);
        setControl(result);
        GridLayout layout = new GridLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN) * 3 / 2;
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING) * 2;
        layout.numColumns = 2;
        result.setLayout(layout);

        Image image = getMessageImage();
        if (image != null) {
            Label label = new Label(result, SWT.NULL);
            image.setBackground(label.getBackground());
            label.setImage(image);
            label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING));
        }

        String message = getMessageString();
        if (message != null) {
            Label messageLabel = new Label(result, SWT.WRAP);
            messageLabel.setText(message);
            GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_BEGINNING);
            data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            messageLabel.setLayoutData(data);
            messageLabel.setFont(result.getFont());
        }
        Dialog.applyDialogFont(result);
    }
}
