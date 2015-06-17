/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search.replace;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.search.ICustomSearchQuery;
import org.python.pydev.shared_ui.search.SearchMessages;

public class ReplaceConfigurationPage extends UserInputWizardPage {

    private static final String SETTINGS_GROUP = "ReplaceDialog2"; //$NON-NLS-1$
    private static final String SETTINGS_REPLACE_WITH = "replace_with"; //$NON-NLS-1$

    private final ReplaceRefactoring fReplaceRefactoring;

    private Combo fTextField;
    private Button fReplaceWithRegex;
    private Label fStatusLabel;
    private ContentAssistCommandAdapter fTextFieldContentAssist;

    public ReplaceConfigurationPage(ReplaceRefactoring refactoring) {
        super("ReplaceConfigurationPage"); //$NON-NLS-1$
        fReplaceRefactoring = refactoring;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite result = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        result.setLayout(layout);

        Label description = new Label(result, SWT.NONE);
        int numberOfMatches = fReplaceRefactoring.getNumberOfMatches();
        int numberOfFiles = fReplaceRefactoring.getNumberOfFiles();
        String[] arguments = { String.valueOf(numberOfMatches), String.valueOf(numberOfFiles) };
        if (numberOfMatches > 1 && numberOfFiles > 1) {
            description.setText(MessageFormat.format(SearchMessages.ReplaceConfigurationPage_description_many_in_many,
                    (Object[]) arguments));
        } else if (numberOfMatches == 1) {
            description.setText(SearchMessages.ReplaceConfigurationPage_description_one_in_one);
        } else {
            description.setText(MessageFormat.format(SearchMessages.ReplaceConfigurationPage_description_many_in_one,
                    (Object[]) arguments));
        }
        description.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));

        ICustomSearchQuery query = fReplaceRefactoring.getQuery();

        Label label1 = new Label(result, SWT.NONE);
        label1.setText(SearchMessages.ReplaceConfigurationPage_replace_label);

        Text clabel = new Text(result, SWT.BORDER | SWT.READ_ONLY);
        clabel.setText(query.getSearchString());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(50);
        clabel.setLayoutData(gd);

        Label label2 = new Label(result, SWT.NONE);
        label2.setText(SearchMessages.ReplaceConfigurationPage_with_label);

        fTextField = new Combo(result, SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(50);
        fTextField.setLayoutData(gd);
        fTextField.setFocus();
        fTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });

        IDialogSettings settings = SharedUiPlugin.getDefault().getDialogSettings().getSection(SETTINGS_GROUP);
        if (settings != null) {
            String[] previousReplaceWith = settings.getArray(SETTINGS_REPLACE_WITH);
            if (previousReplaceWith != null) {
                fTextField.setItems(previousReplaceWith);
                fTextField.select(0);
            }
        }

        ComboContentAdapter contentAdapter = new ComboContentAdapter();
        IContentProposalProvider replaceProposer = null;

        //the code below is so that this works in Eclipse 3.3.
        try {
            //new FindReplaceDocumentAdapterContentProposalProvider(false);
            Class<?> class1 = getClass().getClassLoader().loadClass(
                    "org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider");
            Constructor<?> constructor = class1.getConstructor(Boolean.class);
            replaceProposer = (IContentProposalProvider) constructor.newInstance(false);
        } catch (Throwable e) {
            //just ignore it if we don't succeed
        }

        try {
            fTextFieldContentAssist = new ContentAssistCommandAdapter(fTextField, contentAdapter, replaceProposer,
                    ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[] { '$', '\\' }, true);
        } catch (Throwable e) {
            // Not available in eclipse 3.2
            fTextFieldContentAssist = new ContentAssistCommandAdapter(fTextField, contentAdapter, replaceProposer,
                    ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[] { '$', '\\' });
        }

        new Label(result, SWT.NONE);
        fReplaceWithRegex = new Button(result, SWT.CHECK);
        fReplaceWithRegex.setText(SearchMessages.ReplaceConfigurationPage_isRegex_label);
        fReplaceWithRegex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setContentAssistsEnablement(fReplaceWithRegex.getSelection());
            }
        });
        if (query.isRegexSearch()) {
            fReplaceWithRegex.setSelection(true);
        } else {
            fReplaceWithRegex.setSelection(false);
            fReplaceWithRegex.setEnabled(false);
        }

        fStatusLabel = new Label(result, SWT.NULL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.BOTTOM;
        gd.horizontalSpan = 2;
        fStatusLabel.setLayoutData(gd);

        setContentAssistsEnablement(fReplaceWithRegex.getSelection());

        setControl(result);

        Dialog.applyDialogFont(result);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), "org.eclipse.search.replace_dialog_context");
    }

    final void updateOKStatus() {
        RefactoringStatus status = new RefactoringStatus();
        if (fReplaceWithRegex != null && fReplaceWithRegex.getSelection()) {
            try {
                PatternConstructor.interpretReplaceEscapes(fReplaceWithRegex.getText(), fReplaceRefactoring.getQuery()
                        .getSearchString(), "\n"); //$NON-NLS-1$
            } catch (PatternSyntaxException e) {
                String locMessage = e.getLocalizedMessage();
                int i = 0;
                while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
                    i++;
                }
                status.addError(locMessage.substring(0, i)); // only take first line
            }
        }
        setPageComplete(status);
    }

    private void setContentAssistsEnablement(boolean enable) {
        fTextFieldContentAssist.setEnabled(enable);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
     */
    @Override
    protected boolean performFinish() {
        initializeRefactoring();
        storeSettings();
        return super.performFinish();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
     */
    @Override
    public IWizardPage getNextPage() {
        initializeRefactoring();
        storeSettings();
        return super.getNextPage();
    }

    private void storeSettings() {
        String[] items = fTextField.getItems();
        ArrayList<String> history = new ArrayList<String>();
        history.add(fTextField.getText());
        int historySize = Math.min(items.length, 6);
        for (int i = 0; i < historySize; i++) {
            String curr = items[i];
            if (!history.contains(curr)) {
                history.add(curr);
            }
        }
        IDialogSettings settings = SharedUiPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_GROUP);
        settings.put(SETTINGS_REPLACE_WITH, history.toArray(new String[history.size()]));

    }

    private void initializeRefactoring() {
        fReplaceRefactoring.setReplaceString(fTextField.getText());
    }

}