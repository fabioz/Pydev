/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 8, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.wizards.TextInputWizardPage;

public class PyRenameRefactoringWizard extends RefactoringWizard {

    private final String fInputPageDescription;
    private RefactoringRequest req;
    private TextInputWizardPage inputPage;
    private String fInitialSetting;

    public PyRenameRefactoringWizard(Refactoring refactoring, String defaultPageTitle, String inputPageDescription,
            RefactoringRequest req, String initial) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE);
        setDefaultPageTitle(defaultPageTitle);
        fInputPageDescription = inputPageDescription;
        this.req = req;
        this.fInitialSetting = initial;
    }

    /* non java-doc
     * @see RefactoringWizard#addUserInputPages
     */
    protected void addUserInputPages() {
        inputPage = createInputPage(fInputPageDescription, fInitialSetting);
        addPage(inputPage);
    }

    protected TextInputWizardPage createInputPage(String message, String initialSetting) {
        return new TextInputWizardPage(message, true, initialSetting) {
            protected RefactoringStatus validateTextField(String text) {
                RefactoringStatus status = new RefactoringStatus();
                if (StringUtils.isWord(text)) {
                    req.inputName = text;
                } else {
                    status.addFatalError("The name:" + text + " is not a valid identifier.");
                }
                return status;
            }

            public void createControl(Composite parent) {
                Composite superComposite = new Composite(parent, SWT.NONE);
                setControl(superComposite);
                initializeDialogUnits(superComposite);

                superComposite.setLayout(new GridLayout());
                Composite composite = new Composite(superComposite, SWT.NONE);
                composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                GridLayout layout = new GridLayout();
                layout.numColumns = 2;
                layout.verticalSpacing = 8;
                composite.setLayout(layout);
                //                RowLayouter layouter= new RowLayouter(2);

                Label label = new Label(composite, SWT.NONE);
                label.setText("New value:");

                Text text = createTextInputField(composite);
                text.selectAll();
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.widthHint = convertWidthInCharsToPixels(25);
                text.setLayoutData(gd);

                //                layouter.perform(label, text, 1);
                //                
                //                addOptionalUpdateReferencesCheckbox(composite, layouter);
                //                addOptionalUpdateTextualMatches(composite, layouter);
                //                addOptionalUpdateQualifiedNameComponent(composite, layouter, layout.marginWidth);

                Dialog.applyDialogFont(superComposite);
            }
        };
    }

}
