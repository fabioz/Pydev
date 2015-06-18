/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractSearchIndexPage extends DialogPage implements ISearchPage {

    protected SearchIndexDataHistory searchIndexDataHistory;
    protected Text fPattern;
    protected ISearchPageContainer fContainer;
    protected boolean fFirstTime = true;

    protected Button fIsCaseSensitiveCheckbox;

    // Scope
    protected Button fModulesScopeRadio;
    protected Button fOpenEditorsScopeRadio;
    protected Button fWorkspaceScopeRadio;
    protected Button fProjectsScopeRadio;
    protected Button fExternalFilesRadio;

    // Scope data
    protected Text fModuleNames;
    protected Text fProjectNames;
    protected Text fExternalFolders;

    protected Button fHistory;
    protected Button fSelectProjects;
    protected Button fSelectFolders;

    public AbstractSearchIndexPage(AbstractUIPlugin plugin) {
        searchIndexDataHistory = new SearchIndexDataHistory(plugin);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        searchIndexDataHistory.readConfiguration();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout(10, false);
        composite.setLayout(layout);

        // Line 1
        createLabel(composite, SWT.LEAD,
                "&Text  (* = any string, ? = any character, \\\\ = escape). Exact match by default. Add * to begin/end for sub-matches (slower).",
                10);

        if (acceptExternalFoldersAndOpenEditors()) {
            // Line 2
            fPattern = createText(composite, SWT.SINGLE | SWT.BORDER, 4, 50);

            fHistory = createButton(composite, SWT.PUSH, "...", 1);
            ((GridData) fHistory.getLayoutData()).widthHint = 25;

            fIsCaseSensitiveCheckbox = createButton(composite, SWT.CHECK, SearchMessages.SearchPage_caseSensitive, 5);

            // Line 3
            createLabel(composite, SWT.LEAD, "Scope", 10);

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
        } else {
            // Line 2
            fPattern = createText(composite, SWT.SINGLE | SWT.BORDER, 5, 50);

            // fHistory = createButton(composite, SWT.PUSH, "...", 1);
            // ((GridData) fHistory.getLayoutData()).widthHint = 25;

            fIsCaseSensitiveCheckbox = createButton(composite, SWT.CHECK, SearchMessages.SearchPage_caseSensitive, 5);

            // Line 3
            createLabel(composite, SWT.LEAD, "Scope", 1);
            fWorkspaceScopeRadio = createButton(composite, SWT.RADIO, "&Workspace", 1);

            fModulesScopeRadio = createButton(composite, SWT.RADIO, "&Module(s)", 1);
            fModuleNames = createText(composite, SWT.SINGLE | SWT.BORDER, 2, 50);
            createLabel(composite, SWT.NONE, "", 5);

            // Line 4
            createLabel(composite, SWT.NONE, "", 1);
            fProjectsScopeRadio = createButton(composite, SWT.RADIO, "&Project(s)", 1);

            fProjectNames = createText(composite, SWT.SINGLE | SWT.BORDER, 3, 50);

            // fSelectProjects = createButton(composite, SWT.PUSH, "...", 1);
            // ((GridData) fSelectProjects.getLayoutData()).widthHint = 25;

            createLabel(composite, SWT.LEAD,
                    "\n\nNote: only modules in the PyDev index will be searched (valid modules below a source folder).",
                    10);
            createLabel(composite, SWT.LEAD,
                    "Note: wildcards may be used for modules and project matching.",
                    10);
        }

        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    public boolean acceptExternalFoldersAndOpenEditors() {
        return false;
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
        label.setText(string);
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

    protected ScopeAndData getScopeAndData() {
        if (fModulesScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_MODULES, fModuleNames.getText());
        }

        if (fOpenEditorsScopeRadio != null && fOpenEditorsScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_OPEN_EDITORS, "");
        }

        if (fWorkspaceScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_WORKSPACE, "");
        }

        if (fProjectsScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_PROJECTS, fProjectNames.getText());
        }

        if (fExternalFilesRadio != null && fExternalFilesRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_EXTERNAL_FOLDERS, fExternalFolders.getText());
        }

        // If nothing works, use workspace!
        return new ScopeAndData(SearchIndexData.SCOPE_WORKSPACE, "");
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

                // Load settings from last activation
                SearchIndexData last = initializeFromLast();

                // Override some settings from the current selection
                initializeFromSelection(last);
            }
            fPattern.setFocus();
        }
        super.setVisible(visible);

        updateOKStatus();
    }

    private SearchIndexData initializeFromLast() {
        SearchIndexData last = searchIndexDataHistory.getLast();
        if (last != null) {
            String text = last.textPattern;
            if (text != null && text.length() > 0) {
                fPattern.setText(text);
                return last;
            }
        }
        return null;
    }

    private void updateOKStatus() {
        fContainer.setPerformActionEnabled(true);
    }

    private void initializeFromSelection(SearchIndexData last) {
        ISelection selection = fContainer.getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty()
                && ((ITextSelection) selection).getLength() > 0) {
            String text = ((ITextSelection) selection).getText();
            if (text != null) {
                fPattern.setText(text);
            }
        }

        Collection<String> projectNames = new HashSet<>();
        Collection<String> moduleNames = new HashSet<>();

        ISelection sel = fContainer.getSelection();
        boolean hasNonEditorSelection = true;
        if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
            Iterator<?> iter = ((IStructuredSelection) sel).iterator();
            while (iter.hasNext()) {
                Object curr = iter.next();
                if (curr instanceof IWorkingSet) {
                    IWorkingSet workingSet = (IWorkingSet) curr;
                    if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
                        // Empty working set: ignore
                        continue;

                    }
                    IAdaptable[] elements = workingSet.getElements();
                    for (int i = 0; i < elements.length; i++) {
                        IResource resource = elements[i].getAdapter(IResource.class);
                        checkSelectedResource(projectNames, moduleNames, resource);
                    }
                } else if (curr instanceof ICustomLineElement) {
                    IResource resource = ((ICustomLineElement) curr).getParent();
                    checkSelectedResource(projectNames, moduleNames, resource);

                } else if (curr instanceof IAdaptable) {
                    IResource resource = ((IAdaptable) curr).getAdapter(IResource.class);
                    checkSelectedResource(projectNames, moduleNames, resource);
                }
            }
        } else if (fContainer.getActiveEditorInput() != null) {
            hasNonEditorSelection = false;
            checkSelectedResource(projectNames, moduleNames, fContainer.getActiveEditorInput().getAdapter(IFile.class));
        }

        this.fModuleNames.setText(StringUtils.join(", ", moduleNames));
        this.fProjectNames.setText(StringUtils.join(", ", projectNames));

        // Set the scope (with early return)
        if (hasNonEditorSelection) {
            if (!moduleNames.isEmpty()) {
                this.fModulesScopeRadio.setSelection(true);
                return;

            } else if (!projectNames.isEmpty()) {
                this.fProjectsScopeRadio.setSelection(true);
                return;
            }
        }

        if (last != null) {
            int scope = last.scope;
            switch (scope) {
                case SearchIndexData.SCOPE_WORKSPACE:
                    this.fWorkspaceScopeRadio.setSelection(true);
                    return;

                case SearchIndexData.SCOPE_MODULES:
                    this.fModulesScopeRadio.setSelection(true);
                    return;

                case SearchIndexData.SCOPE_PROJECTS:
                    this.fProjectsScopeRadio.setSelection(true);
                    return;

                case SearchIndexData.SCOPE_EXTERNAL_FOLDERS:
                    this.fExternalFilesRadio.setSelection(true);
                    return;

                case SearchIndexData.SCOPE_OPEN_EDITORS:
                    this.fOpenEditorsScopeRadio.setSelection(true);
                    return;
            }
        }

        //All others failed: go for workspace selection
        this.fWorkspaceScopeRadio.setSelection(true);

    }

    /**
     * Subclasses should override so that given the selected resource the project names/ module names are properly filled
     * for the initial values.
     */
    protected abstract void checkSelectedResource(Collection<String> projectNames, Collection<String> moduleNames,
            IResource resource);
}
