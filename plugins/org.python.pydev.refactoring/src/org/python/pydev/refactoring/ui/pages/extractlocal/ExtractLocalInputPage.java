/******************************************************************************
* Copyright (C) 2007-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ui.pages.extractlocal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRefactoring;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.pages.core.eclipse.RowLayouter;
import org.python.pydev.refactoring.ui.pages.core.eclipse.TextInputWizardPage;
import org.python.pydev.shared_core.string.StringUtils;

public class ExtractLocalInputPage extends TextInputWizardPage {
    public static final String PAGE_NAME = "ExtractLocalInputPage"; //$NON-NLS-1$
    private static final String EXTRACT_LOCAL_REPLACE_DUPLICATES = "EXTRACT_LOCAL_REPLACE_DUPLICATES";
    private Button replaceDuplicates;

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

        replaceDuplicates = new Button(result, SWT.CHECK);
        ExtractLocalRequestProcessor requestProcessor = getRequestProcessor();
        replaceDuplicates.setText(StringUtils.format("Also replace &duplicates (%s references)?",
                requestProcessor.getDuplicatesSize()));

        IPreferenceStore preferences = PydevPrefs.getPreferences();
        boolean replace = preferences.getBoolean(EXTRACT_LOCAL_REPLACE_DUPLICATES);
        replaceDuplicates.setSelection(replace);
        requestProcessor.setReplaceDuplicates(replace);
        replaceDuplicates.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getRequestProcessor().setReplaceDuplicates(replaceDuplicates.getSelection());
                IPreferenceStore preferences = PydevPrefs.getPreferences();
                preferences.setValue(EXTRACT_LOCAL_REPLACE_DUPLICATES, replaceDuplicates.getSelection());
            }

        });
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        replaceDuplicates.setLayoutData(gridData);

        layouter.perform(label, text, 1);

        Dialog.applyDialogFont(result);
    }

    /*
     * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#textModified(java.lang.String)
     */
    @Override
    protected void textModified(String text) {
        getRequestProcessor().setVariableName(text);
        super.textModified(text);
    }

    /*
     * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
     */
    @Override
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
