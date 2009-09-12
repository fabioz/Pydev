/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ui.pages.extractlocal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRefactoring;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.pages.core.eclipse.RowLayouter;
import org.python.pydev.refactoring.ui.pages.core.eclipse.TextInputWizardPage;

public class ExtractLocalInputPage extends TextInputWizardPage {
    public static final String PAGE_NAME = "ExtractLocalInputPage"; //$NON-NLS-1$

    public ExtractLocalInputPage() {
        super(PAGE_NAME, true);
    }

    public void createControl(Composite parent) {
        Composite result = new Composite(parent, SWT.NONE);
        setControl(result);

        /* Create Label and TextField, we use the faciltiies provided by our base class */
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        result.setLayout(layout);
        RowLayouter layouter = new RowLayouter(2);

        Label label = new Label(result, SWT.NONE);
        label.setText(Messages.extractLocalVariableName);

        Text text = createTextInputField(result);
        text.selectAll();
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        layouter.perform(label, text, 1);

        Dialog.applyDialogFont(result);
    }

    /*
     * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#textModified(java.lang.String)
     */
    protected void textModified(String text) {
        getRequestProcessor().setVariableName(text);
        super.textModified(text);
    }

    /*
     * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
     */
    protected RefactoringStatus validateTextField(String text) {
        return getExtractlocalRefactoring().checkVarName(text);
    }

    private ExtractLocalRequestProcessor getRequestProcessor() {
        return getExtractlocalRefactoring().getRequestProcessor();
    }

    private ExtractLocalRefactoring getExtractlocalRefactoring() {
        return (ExtractLocalRefactoring) getRefactoring();
    }
}
