/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.ISearchHelpContextIds;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.text.FileSearchPage;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.internal.ui.text.ReplaceAction;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class PySearchPage extends DialogPage implements ISearchPage {

    private static final int HISTORY_SIZE = 12;
    public static final String EXTENSION_POINT_ID = "com.python.pydev.ui.search.pySearchPage";

    // Dialog store id constants
    private static final String PAGE_NAME = "PydevSearchPage";
    private static final String STORE_CASE_SENSITIVE = "CASE_SENSITIVE"; //$NON-NLS-1$
    private static final String STORE_IS_REG_EX_SEARCH = "REG_EX_SEARCH"; //$NON-NLS-1$
    private static final String STORE_IS_WHOLE_WORD = "WHOLE_WORD"; //$NON-NLS-1$
    private static final String STORE_SEARCH_DERIVED = "SEARCH_DERIVED"; //$NON-NLS-1$
    private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
    private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

    private List fPreviousSearchPatterns = new ArrayList(HISTORY_SIZE);

    private boolean fFirstTime = true;
    private boolean fIsCaseSensitive;
    private boolean fIsRegExSearch;
    private boolean fIsWholeWord;
    private boolean fSearchDerived;

    private Combo fPattern;
    private Button fIsCaseSensitiveCheckbox;
    private Button fIsRegExCheckbox;
    private Button fIsWholeWordCheckbox;
    private CLabel fStatusLabel;

    private ISearchPageContainer fContainer;

    private ContentAssistCommandAdapter fPatterFieldContentAssist;

    private static class SearchPatternData {
        public final boolean isCaseSensitive;
        public final boolean isRegExSearch;
        public final boolean isWholeWord;
        public final String textPattern;
        public final String[] fileNamePatterns;
        public final int scope;
        public final IWorkingSet[] workingSets;

        public SearchPatternData(String textPattern, boolean isCaseSensitive, boolean isRegExSearch,
                boolean isWholeWord, String[] fileNamePatterns, int scope, IWorkingSet[] workingSets) {
            Assert.isNotNull(fileNamePatterns);
            this.isCaseSensitive = isCaseSensitive;
            this.isRegExSearch = isRegExSearch;
            this.isWholeWord = isWholeWord;
            this.textPattern = textPattern;
            this.fileNamePatterns = fileNamePatterns;
            this.scope = scope;
            this.workingSets = workingSets; // can be null
        }

        public void store(IDialogSettings settings) {
            settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
            settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
            settings.put("isWholeWord", isWholeWord); //$NON-NLS-1$
            settings.put("textPattern", textPattern); //$NON-NLS-1$
            settings.put("fileNamePatterns", fileNamePatterns); //$NON-NLS-1$
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
            String[] fileNamePatterns = settings.getArray("fileNamePatterns"); //$NON-NLS-1$
            if (fileNamePatterns == null) {
                fileNamePatterns = new String[0];
            }
            try {
                int scope = settings.getInt("scope"); //$NON-NLS-1$
                boolean isRegExSearch = settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
                boolean ignoreCase = settings.getBoolean("ignoreCase"); //$NON-NLS-1$
                boolean isWholeWord = settings.getBoolean("isWholeWord"); //$NON-NLS-1$

                return new SearchPatternData(textPattern, !ignoreCase, isRegExSearch, isWholeWord, fileNamePatterns,
                        scope, workingSets);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    private static class TextSearchPageInput extends TextSearchInput {

        private final String fSearchText;
        private final boolean fIsCaseSensitive;
        private final boolean fIsRegEx;
        private final boolean fIsWholeWord;
        private final FileTextSearchScope fScope;

        public TextSearchPageInput(String searchText, boolean isCaseSensitive, boolean isRegEx, boolean isWholeWord,
                FileTextSearchScope scope) {
            fSearchText = searchText;
            fIsCaseSensitive = isCaseSensitive;
            fIsRegEx = isRegEx;
            fIsWholeWord = isWholeWord;
            fScope = scope;
        }

        @Override
        public String getSearchText() {
            return fSearchText;
        }

        @Override
        public boolean isCaseSensitiveSearch() {
            return fIsCaseSensitive;
        }

        @Override
        public boolean isRegExSearch() {
            return fIsRegEx;
        }

        @Override
        public boolean isWholeWordSearch() {
            return fIsWholeWord;
        }

        @Override
        public FileTextSearchScope getScope() {
            return fScope;
        }
    }

    //---- Action Handling ------------------------------------------------

    private ISearchQuery newQuery() throws CoreException {
        SearchPatternData data = getPatternData();
        TextSearchPageInput input = new TextSearchPageInput(data.textPattern, data.isCaseSensitive, data.isRegExSearch,
                data.isWholeWord && !data.isRegExSearch, createTextSearchScope());
        return TextSearchQueryProvider.getPreferred().createQuery(input);
    }

    public boolean performAction() {
        try {
            NewSearchUI.runQueryInBackground(newQuery());
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), SearchMessages.TextSearchPage_replace_searchproblems_title,
                    SearchMessages.TextSearchPage_replace_searchproblems_message, e.getStatus());
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.search.ui.IReplacePage#performReplace()
     */
    public boolean performReplace() {
        try {
            IStatus status = NewSearchUI.runQueryInForeground(getContainer().getRunnableContext(), newQuery());
            if (status.matches(IStatus.CANCEL)) {
                return false;
            }
            if (!status.isOK()) {
                ErrorDialog.openError(getShell(), SearchMessages.TextSearchPage_replace_searchproblems_title,
                        SearchMessages.TextSearchPage_replace_runproblem_message, status);
            }

            Display.getCurrent().asyncExec(new Runnable() {
                public void run() {
                    ISearchResultViewPart view = NewSearchUI.activateSearchResultView();
                    if (view != null) {
                        ISearchResultPage page = view.getActivePage();
                        if (page instanceof FileSearchPage) {
                            FileSearchPage filePage = (FileSearchPage) page;
                            new ReplaceAction(filePage.getSite().getShell(), (FileSearchResult) filePage.getInput(),
                                    null).run();
                        }
                    }
                }
            });
            return true;
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), SearchMessages.TextSearchPage_replace_searchproblems_title,
                    SearchMessages.TextSearchPage_replace_querycreationproblem_message, e.getStatus());
            return false;
        }
    }

    private String getPattern() {
        return fPattern.getText();
    }

    public FileTextSearchScope createTextSearchScope() {
        // Setup search scope
        switch (getContainer().getSelectedScope()) {
            case ISearchPageContainer.WORKSPACE_SCOPE:
                return FileTextSearchScope.newWorkspaceScope(getExtensions(), fSearchDerived);
            case ISearchPageContainer.SELECTION_SCOPE:
                return getSelectedResourcesScope();
            case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
                return getEnclosingProjectScope();
            case ISearchPageContainer.WORKING_SET_SCOPE:
                IWorkingSet[] workingSets = getContainer().getSelectedWorkingSets();
                return FileTextSearchScope.newSearchScope(workingSets, getExtensions(), fSearchDerived);
            default:
                // unknown scope
                return FileTextSearchScope.newWorkspaceScope(getExtensions(), fSearchDerived);
        }
    }

    private FileTextSearchScope getSelectedResourcesScope() {
        HashSet resources = new HashSet();
        ISelection sel = getContainer().getSelection();
        if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
            Iterator iter = ((IStructuredSelection) sel).iterator();
            while (iter.hasNext()) {
                Object curr = iter.next();
                if (curr instanceof IWorkingSet) {
                    IWorkingSet workingSet = (IWorkingSet) curr;
                    if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
                        return FileTextSearchScope.newWorkspaceScope(getExtensions(), fSearchDerived);
                    }
                    IAdaptable[] elements = workingSet.getElements();
                    for (int i = 0; i < elements.length; i++) {
                        IResource resource = (IResource) elements[i].getAdapter(IResource.class);
                        if (resource != null && resource.isAccessible()) {
                            resources.add(resource);
                        }
                    }
                } else if (curr instanceof LineElement) {
                    IResource resource = ((LineElement) curr).getParent();
                    if (resource != null && resource.isAccessible()) {
                        resources.add(resource);
                    }
                } else if (curr instanceof IAdaptable) {
                    IResource resource = (IResource) ((IAdaptable) curr).getAdapter(IResource.class);
                    if (resource != null && resource.isAccessible()) {
                        resources.add(resource);
                    }
                }
            }
        } else if (getContainer().getActiveEditorInput() != null) {
            resources.add(getContainer().getActiveEditorInput().getAdapter(IFile.class));
        }
        IResource[] arr = (IResource[]) resources.toArray(new IResource[resources.size()]);
        return FileTextSearchScope.newSearchScope(arr, getExtensions(), fSearchDerived);
    }

    private FileTextSearchScope getEnclosingProjectScope() {
        String[] enclosingProjectName = getContainer().getSelectedProjectNames();
        if (enclosingProjectName == null) {
            return FileTextSearchScope.newWorkspaceScope(getExtensions(), fSearchDerived);
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource[] res = new IResource[enclosingProjectName.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = root.getProject(enclosingProjectName[i]);
        }

        return FileTextSearchScope.newSearchScope(res, getExtensions(), fSearchDerived);
    }

    private SearchPatternData findInPrevious(String pattern) {
        for (Iterator iter = fPreviousSearchPatterns.iterator(); iter.hasNext();) {
            SearchPatternData element = (SearchPatternData) iter.next();
            if (pattern.equals(element.textPattern)) {
                return element;
            }
        }
        return null;
    }

    /**
     * Return search pattern data and update previous searches.
     * An existing entry will be updated.
     * @return the search pattern data
     */
    private SearchPatternData getPatternData() {
        SearchPatternData match = findInPrevious(fPattern.getText());
        if (match != null) {
            fPreviousSearchPatterns.remove(match);
        }
        match = new SearchPatternData(
                getPattern(),
                isCaseSensitive(),
                fIsRegExCheckbox.getSelection(),
                fIsWholeWordCheckbox.getSelection(),
                getExtensions(),
                getContainer().getSelectedScope(),
                getContainer().getSelectedWorkingSets());
        fPreviousSearchPatterns.add(0, match);
        return match;
    }

    private String[] getPreviousSearchPatterns() {
        int size = fPreviousSearchPatterns.size();
        String[] patterns = new String[size];
        for (int i = 0; i < size; i++) {
            patterns[i] = ((SearchPatternData) fPreviousSearchPatterns.get(i)).textPattern;
        }
        return patterns;
    }

    private String[] getExtensions() {
        ArrayList<String> exts = new ArrayList<String>();
        String[] dottedValidSourceFiles = FileTypesPreferencesPage.getDottedValidSourceFiles();
        for (String sourceFile : dottedValidSourceFiles) {
            exts.add('*' + sourceFile);
        }
        return exts.toArray(new String[0]);
    }

    private boolean isCaseSensitive() {
        return fIsCaseSensitiveCheckbox.getSelection();
    }

    /*
     * Implements method from IDialogPage
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible && fPattern != null) {
            if (fFirstTime) {
                fFirstTime = false;
                // Set item and text here to prevent page from resizing
                fPattern.setItems(getPreviousSearchPatterns());
                //              if (fExtensions.getItemCount() == 0) {
                //                  loadFilePatternDefaults();
                //              }
                if (!initializePatternControl()) {
                    fPattern.select(0);
                    handleWidgetSelected();
                }
            }
            fPattern.setFocus();
        }
        updateOKStatus();

        IEditorInput editorInput = getContainer().getActiveEditorInput();
        getContainer().setActiveEditorCanProvideScopeSelection(
                editorInput != null && editorInput.getAdapter(IFile.class) != null);

        super.setVisible(visible);
    }

    final void updateOKStatus() {
        boolean regexStatus = validateRegex();
        getContainer().setPerformActionEnabled(regexStatus);
    }

    //---- Widget creation ------------------------------------------------

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        readConfiguration();

        Composite result = new Composite(parent, SWT.NONE);
        result.setFont(parent.getFont());
        GridLayout layout = new GridLayout(2, false);
        result.setLayout(layout);

        addTextPatternControls(result);

        Label separator = new Label(result, SWT.NONE);
        separator.setVisible(false);
        GridData data = new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
        data.heightHint = convertHeightInCharsToPixels(1) / 3;
        separator.setLayoutData(data);

        setControl(result);
        Dialog.applyDialogFont(result);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(result, ISearchHelpContextIds.TEXT_SEARCH_PAGE);
    }

    private boolean validateRegex() {
        if (fIsRegExCheckbox.getSelection()) {
            try {
                PatternConstructor.createPattern(fPattern.getText(), fIsCaseSensitive, true);
            } catch (PatternSyntaxException e) {
                String locMessage = e.getLocalizedMessage();
                int i = 0;
                while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
                    i++;
                }
                statusMessage(true, locMessage.substring(0, i)); // only take first line
                return false;
            }
            statusMessage(false, ""); //$NON-NLS-1$
        } else {
            statusMessage(false, SearchMessages.SearchPage_containingText_hint);
        }
        return true;
    }

    private void addTextPatternControls(Composite group) {
        // grid layout with 2 columns

        // Info text
        Label label = new Label(group, SWT.LEAD);
        label.setText(SearchMessages.SearchPage_containingText_text);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(group.getFont());

        // Pattern combo
        fPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
        // Not done here to prevent page from resizing
        // fPattern.setItems(getPreviousSearchPatterns());
        fPattern.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleWidgetSelected();
                updateOKStatus();
            }
        });
        // add some listeners for regex syntax checking
        fPattern.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateOKStatus();
            }
        });
        fPattern.setFont(group.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
        data.widthHint = convertWidthInCharsToPixels(50);
        fPattern.setLayoutData(data);

        ComboContentAdapter contentAdapter = new ComboContentAdapter();
        FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
                true);
        fPatterFieldContentAssist = new ContentAssistCommandAdapter(
                fPattern,
                contentAdapter,
                findProposer,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
                new char[0],
                true);
        fPatterFieldContentAssist.setEnabled(fIsRegExSearch);

        fIsCaseSensitiveCheckbox = new Button(group, SWT.CHECK);
        fIsCaseSensitiveCheckbox.setText(SearchMessages.SearchPage_caseSensitive);
        fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
        fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fIsCaseSensitive = fIsCaseSensitiveCheckbox.getSelection();
            }
        });
        fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fIsCaseSensitiveCheckbox.setFont(group.getFont());

        // RegEx checkbox
        fIsRegExCheckbox = new Button(group, SWT.CHECK);
        fIsRegExCheckbox.setText(SearchMessages.SearchPage_regularExpression);
        fIsRegExCheckbox.setSelection(fIsRegExSearch);

        fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fIsRegExSearch = fIsRegExCheckbox.getSelection();
                updateOKStatus();

                writeConfiguration();
                fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
                fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
            }
        });
        fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
        fIsRegExCheckbox.setFont(group.getFont());

        // Text line which explains the special characters
        fStatusLabel = new CLabel(group, SWT.LEAD);
        fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 2));
        fStatusLabel.setFont(group.getFont());
        fStatusLabel.setAlignment(SWT.LEFT);
        fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint);

        // Whole Word checkbox
        fIsWholeWordCheckbox = new Button(group, SWT.CHECK);
        fIsWholeWordCheckbox.setText(SearchMessages.SearchPage_wholeWord);
        fIsWholeWordCheckbox.setSelection(fIsWholeWord);
        fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
        fIsWholeWordCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fIsWholeWord = fIsWholeWordCheckbox.getSelection();
            }
        });
        fIsWholeWordCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fIsWholeWordCheckbox.setFont(group.getFont());
    }

    private void handleWidgetSelected() {
        int selectionIndex = fPattern.getSelectionIndex();
        if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size()) {
            return;
        }

        SearchPatternData patternData = (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);
        if (!fPattern.getText().equals(patternData.textPattern)) {
            return;
        }
        fIsCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive);
        fIsRegExSearch = patternData.isRegExSearch;
        fIsRegExCheckbox.setSelection(fIsRegExSearch);
        fIsWholeWord = patternData.isWholeWord;
        fIsWholeWordCheckbox.setSelection(fIsWholeWord);
        fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
        fPattern.setText(patternData.textPattern);
        fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
        if (patternData.workingSets != null) {
            getContainer().setSelectedWorkingSets(patternData.workingSets);
        } else {
            getContainer().setSelectedScope(patternData.scope);
        }
    }

    private boolean initializePatternControl() {
        ISelection selection = getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection) selection).getLength() > 0) {
            String text = ((ITextSelection) selection).getText();
            if (text != null) {
                if (fIsRegExSearch) {
                    fPattern.setText(FindReplaceDocumentAdapter.escapeForRegExPattern(text));
                } else {
                    fPattern.setText(insertEscapeChars(text));
                }

                return true;
            }
        }
        return false;
    }

    //  private void loadFilePatternDefaults() {
    //      SearchMatchInformationProviderRegistry registry= SearchPlugin.getDefault().getSearchMatchInformationProviderRegistry();
    //      String[] defaults= registry.getDefaultFilePatterns();
    //      fExtensions.setItems(defaults);
    //      fExtensions.setText(defaults[0]);
    //  }

    private String insertEscapeChars(String text) {
        if (text == null || text.equals(""))
        {
            return ""; //$NON-NLS-1$
        }
        StringBuffer sbIn = new StringBuffer(text);
        BufferedReader reader = new BufferedReader(new StringReader(text));
        int lengthOfFirstLine = 0;
        try {
            lengthOfFirstLine = reader.readLine().length();
        } catch (IOException ex) {
            return ""; //$NON-NLS-1$
        }
        StringBuffer sbOut = new StringBuffer(lengthOfFirstLine + 5);
        int i = 0;
        while (i < lengthOfFirstLine) {
            char ch = sbIn.charAt(i);
            if (ch == '*' || ch == '?' || ch == '\\')
            {
                sbOut.append("\\"); //$NON-NLS-1$
            }
            sbOut.append(ch);
            i++;
        }
        return sbOut.toString();
    }

    private String getExtensionFromEditor() {
        IEditorPart ep = SearchPlugin.getActivePage().getActiveEditor();
        if (ep != null) {
            Object elem = ep.getEditorInput();
            if (elem instanceof IFileEditorInput) {
                String extension = ((IFileEditorInput) elem).getFile().getFileExtension();
                if (extension == null) {
                    return ((IFileEditorInput) elem).getFile().getName();
                }
                return "*." + extension; //$NON-NLS-1$
            }
        }
        return null;
    }

    /**
     * Sets the search page's container.
     * @param container the container to set
     */
    public void setContainer(ISearchPageContainer container) {
        fContainer = container;
    }

    private ISearchPageContainer getContainer() {
        return fContainer;
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
        fIsRegExSearch = s.getBoolean(STORE_IS_REG_EX_SEARCH);
        fIsWholeWord = s.getBoolean(STORE_IS_WHOLE_WORD);
        fSearchDerived = s.getBoolean(STORE_SEARCH_DERIVED);

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
        s.put(STORE_IS_REG_EX_SEARCH, fIsRegExSearch);
        s.put(STORE_IS_WHOLE_WORD, fIsWholeWord);
        s.put(STORE_SEARCH_DERIVED, fSearchDerived);

        int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
        s.put(STORE_HISTORY_SIZE, historySize);
        for (int i = 0; i < historySize; i++) {
            IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
            SearchPatternData data = ((SearchPatternData) fPreviousSearchPatterns.get(i));
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
