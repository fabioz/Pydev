/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import com.python.pydev.analysis.search.SearchMessages;

/**
 * This is still a work in progress!!!
 */
public class SearchIndexPage extends DialogPage implements ISearchPage {

    private static final String PAGE_NAME = "SearchIndexPage";
    private static final String STORE_CASE_SENSITIVE = "CASE_SENSITIVE"; //$NON-NLS-1$
    private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
    private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

    private Combo fPattern;
    private ISearchPageContainer fContainer;
    private boolean fFirstTime = true;
    private boolean fIsCaseSensitive;

    private CLabel fStatusLabel;
    private Button fIsCaseSensitiveCheckbox;

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        readConfiguration();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);

        // Info text
        Label label = new Label(composite, SWT.LEAD);
        label.setText("Text");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(composite.getFont());

        fPattern = new Combo(composite, SWT.SINGLE | SWT.BORDER);
        fPattern.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
        data.widthHint = convertWidthInCharsToPixels(50);
        fPattern.setLayoutData(data);

        fIsCaseSensitiveCheckbox = new Button(composite, SWT.CHECK);
        fIsCaseSensitiveCheckbox.setText(SearchMessages.SearchPage_caseSensitive);
        fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
        fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fIsCaseSensitive = fIsCaseSensitiveCheckbox.getSelection();
            }
        });
        fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fIsCaseSensitiveCheckbox.setFont(composite.getFont());

        fStatusLabel = new CLabel(composite, SWT.LEAD);
        fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 2));
        fStatusLabel.setFont(composite.getFont());
        fStatusLabel.setAlignment(SWT.LEFT);
        fStatusLabel.setText("(* = any string, ? = any character, \\ = escape)");

        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    @Override
    public boolean performAction() {
        SearchIndexQuery query = new SearchIndexQuery(fPattern.getText());
        query.setCaseInsensitive(!fIsCaseSensitive);
        NewSearchUI.runQueryInBackground(query);
        return true;
    }

    @Override
    public void setContainer(ISearchPageContainer container) {
        fContainer = container;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && fPattern != null) {
            if (fFirstTime) {
                fFirstTime = false;
                fPattern.setItems(getPreviousSearchPatterns());
                if (!initializePatternControl()) {
                    fPattern.select(0);
                }
            }
            fPattern.setFocus();
        }
        super.setVisible(visible);

        updateOKStatus();

        IEditorInput editorInput = fContainer.getActiveEditorInput();
        if (editorInput != null) {
            IFile currentFile = editorInput.getAdapter(IFile.class);
            //TODO: Use it for the scoping...
        }

    }

    final void updateOKStatus() {
        fContainer.setPerformActionEnabled(true);
    }

    private static final int HISTORY_SIZE = 12;
    private List<SearchPatternData> fPreviousSearchPatterns = new ArrayList<>(HISTORY_SIZE);

    private static class SearchPatternData {
        public final boolean isCaseSensitive;
        public final String textPattern;
        public final int scope;
        public final IWorkingSet[] workingSets;

        public SearchPatternData(String textPattern, boolean isCaseSensitive, int scope, IWorkingSet[] workingSets) {
            this.isCaseSensitive = isCaseSensitive;
            this.textPattern = textPattern;
            this.scope = scope;
            this.workingSets = workingSets; // can be null
        }

        public void store(IDialogSettings settings) {
            settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
            settings.put("textPattern", textPattern); //$NON-NLS-1$
            settings.put("scope", scope); //$NON-NLS-1$
            if (workingSets != null) {
                String[] wsIds = new String[workingSets.length];
                for (int i = 0; i < workingSets.length; i++) {
                    wsIds[i] = workingSets[i].getLabel();
                }
                settings.put("workingSets", wsIds); //$NON-NLS-1$
            } else {
                settings.put("workingSets", new String[0]); //$NON-NLS-1$
            }

        }

        public static SearchPatternData create(IDialogSettings settings) {
            String textPattern = settings.get("textPattern"); //$NON-NLS-1$
            String[] wsIds = settings.getArray("workingSets"); //$NON-NLS-1$
            IWorkingSet[] workingSets = null;
            if (wsIds != null && wsIds.length > 0) {
                IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
                workingSets = new IWorkingSet[wsIds.length];
                for (int i = 0; workingSets != null && i < wsIds.length; i++) {
                    workingSets[i] = workingSetManager.getWorkingSet(wsIds[i]);
                    if (workingSets[i] == null) {
                        workingSets = null;
                    }
                }
            }

            try {
                int scope = settings.getInt("scope"); //$NON-NLS-1$
                boolean ignoreCase = settings.getBoolean("ignoreCase"); //$NON-NLS-1$

                return new SearchPatternData(textPattern, !ignoreCase, scope, workingSets);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    private String[] getPreviousSearchPatterns() {
        int size = fPreviousSearchPatterns.size();
        String[] patterns = new String[size];
        for (int i = 0; i < size; i++) {
            patterns[i] = fPreviousSearchPatterns.get(i).textPattern;
        }
        return patterns;
    }

    private boolean initializePatternControl() {
        ISelection selection = getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty()
                && ((ITextSelection) selection).getLength() > 0) {
            String text = ((ITextSelection) selection).getText();
            if (text != null) {
                fPattern.setText(text);
                fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
                return true;
            }
        }
        return false;
    }

    private ISelection getSelection() {
        return fContainer.getSelection();
    }

    //--------------- Configuration handling --------------

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    @Override
    public void dispose() {
        writeConfiguration();
        super.dispose();
    }

    /**
     * Returns the page settings for this Text search page.
     *
     * @return the page settings to be used
     */
    private IDialogSettings getDialogSettings() {
        return SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
    }

    /**
     * Initializes itself from the stored page settings.
     */
    private void readConfiguration() {
        IDialogSettings s = getDialogSettings();
        fIsCaseSensitive = s.getBoolean(STORE_CASE_SENSITIVE);

        try {
            int historySize = s.getInt(STORE_HISTORY_SIZE);
            for (int i = 0; i < historySize; i++) {
                IDialogSettings histSettings = s.getSection(STORE_HISTORY + i);
                if (histSettings != null) {
                    SearchPatternData data = SearchPatternData.create(histSettings);
                    if (data != null) {
                        fPreviousSearchPatterns.add(data);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    /**
     * Stores it current configuration in the dialog store.
     */
    private void writeConfiguration() {
        IDialogSettings s = getDialogSettings();
        s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);

        int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        for (int i = 0; i < historySize; i++) {
            IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
            SearchPatternData data = (fPreviousSearchPatterns.get(i));
            data.store(histSettings);
        }

    }

    private void statusMessage(boolean error, String message) {
        fStatusLabel.setText(message);
        if (error) {
            fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
        } else {
            fStatusLabel.setForeground(null);
        }
    }

}
