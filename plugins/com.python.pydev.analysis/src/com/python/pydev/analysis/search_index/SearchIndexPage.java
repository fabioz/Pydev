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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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
    private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
    private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

    private Text fPattern;
    private ISearchPageContainer fContainer;
    private boolean fFirstTime = true;

    //    private CLabel fStatusLabel;
    private Button fIsCaseSensitiveCheckbox;
    private Text fModuleNames;
    private Button fHistory;
    private Button fModulesScopeRadio;
    private Button fOpenEditorsScopeRadio;
    private Button fWorkspaceScopeRadio;
    private Button fProjectsScopeRadio;
    private Text fProjectNames;
    private Button fExternalFilesRadio;
    private Text fExternalFolders;
    private Button fSelectProjects;
    private Button fSelectFolders;

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        readConfiguration();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout(10, false);
        composite.setLayout(layout);

        // Line 1
        Label label = new Label(composite, SWT.LEAD);
        label.setText(
                "&Text  (* = any string, ? = any character, \\\\ = escape). Exact match by default. Add * to begin/end for (slower) sub-matches.");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 10, 1));
        label.setFont(composite.getFont());

        // Line 2
        fPattern = createText(composite, SWT.SINGLE | SWT.BORDER, 4, 50);

        fHistory = createButton(composite, SWT.PUSH, "...", 1);
        ((GridData) fHistory.getLayoutData()).widthHint = 25;

        fIsCaseSensitiveCheckbox = createButton(composite, SWT.CHECK, SearchMessages.SearchPage_caseSensitive, 5);

        // Line 3
        label = createLabel(composite, SWT.LEAD, "Scope", 10);

        // Line 4
        fModulesScopeRadio = createButton(composite, SWT.RADIO, "&Module(s)", 1);

        fModuleNames = createText(composite, SWT.SINGLE | SWT.BORDER, 3, 50);

        fWorkspaceScopeRadio = createButton(composite, SWT.RADIO, "&Workspace", 3);

        fOpenEditorsScopeRadio = createButton(composite, SWT.RADIO, "&Open Editors", 3);

        // Line 5
        fProjectsScopeRadio = createButton(composite, SWT.RADIO, "&Project(s)", 1);

        fProjectNames = createText(composite, SWT.SINGLE | SWT.BORDER, 1, 50);

        fSelectProjects = createButton(composite, SWT.PUSH, "...", 2);
        ((GridData) fSelectProjects.getLayoutData()).widthHint = 25;

        fExternalFilesRadio = createButton(composite, SWT.RADIO, "External &Folder(s)", 2);

        fExternalFolders = createText(composite, SWT.SINGLE | SWT.BORDER, 3, 50);

        fSelectFolders = createButton(composite, SWT.PUSH, "...", 1);
        ((GridData) fSelectFolders.getLayoutData()).widthHint = 25;

        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    private Text createText(Composite composite, int style, int cols, int charsLen) {
        Text text = new Text(composite, style);
        text.setFont(composite.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, cols, 1);
        data.widthHint = convertWidthInCharsToPixels(charsLen);
        text.setLayoutData(data);
        return text;
    }

    private Label createLabel(Composite composite, int style, String string, int cols) {
        Label label = new Label(composite, style);
        label.setText("Scope: ");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, cols, 1));
        label.setFont(composite.getFont());
        return label;
    }

    private Button createButton(Composite composite, int style, String string, int cols) {
        Button bt = new Button(composite, style);
        bt.setText(string);
        bt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
        bt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, cols, 1));
        bt.setFont(composite.getFont());
        return bt;
    }

    @Override
    public boolean performAction() {
        SearchIndexQuery query = new SearchIndexQuery(fPattern.getText());
        query.setCaseInsensitive(!fIsCaseSensitiveCheckbox.getSelection());
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
                if (!initializePatternControl()) {
                    String[] previousSearchPatterns = getPreviousSearchPatterns();
                    if (previousSearchPatterns.length > 0) {
                        fPattern.setText(previousSearchPatterns[0]);
                    }
                }
            }
            fPattern.setFocus();
        }
        super.setVisible(visible);

        updateOKStatus();

        ISelection selection = fContainer.getSelection();
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

        int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        for (int i = 0; i < historySize; i++) {
            IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
            SearchPatternData data = (fPreviousSearchPatterns.get(i));
            data.store(histSettings);
        }

    }

    private void statusMessage(boolean error, String message) {
        //        fStatusLabel.setText(message);
        //        if (error) {
        //            fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
        //        } else {
        //            fStatusLabel.setForeground(null);
        //        }
    }

}
