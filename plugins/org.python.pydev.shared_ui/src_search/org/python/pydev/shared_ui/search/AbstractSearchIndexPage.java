/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.dialogs.ProjectSelectionDialog;

public abstract class AbstractSearchIndexPage extends DialogPage implements ISearchPage {

    protected SearchIndexDataHistory searchIndexDataHistory;
    protected Text fPattern;
    protected ISearchPageContainer fContainer;
    protected boolean fFirstTime = true;

    protected Button fIsCaseSensitiveCheckbox;
    protected Button fIsWholeWordCheckbox;

    // Scope
    protected Button fModulesScopeRadio;
    protected Button fWorkspaceScopeRadio;
    protected Button fProjectsScopeRadio;

    // Scope data
    protected Text fModuleNames;
    protected Text fProjectNames;

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
                "&Text  (* = any string, ? = any character, \\\\ = escape).",
                10);

        createComponents(composite);
        if (fSelectProjects != null && fProjectNames != null) {
            fSelectProjects.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Shell activeShell = Display.getCurrent().getActiveShell();
                    ProjectSelectionDialog dialog = new ProjectSelectionDialog(activeShell, null, true);
                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                    String text = fProjectNames.getText();
                    ArrayList<Object> lst = new ArrayList<>();
                    for (String s : StringUtils.split(text, ',')) {
                        s = s.trim();
                        IProject project = root.getProject(s);
                        if (project != null && project.exists() && project.isAccessible()) {
                            lst.add(project);
                        }
                    }
                    dialog.setInitialElementSelections(lst);
                    int open = dialog.open();
                    if (open == Window.OK) {
                        Object[] result = dialog.getResult();
                        if (result != null) {
                            FastStringBuffer buf = new FastStringBuffer();

                            for (Object object : result) {
                                if (object instanceof IProject) {
                                    if (buf.length() > 0) {
                                        buf.append(", ");
                                    }
                                    buf.append(((IProject) object).getName());
                                }
                            }

                            fProjectNames.setText(buf.toString());
                            setRadioSelection(fProjectsScopeRadio);
                        }
                    }
                }
            });
        }

        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    protected void setRadioSelection(Button bt) {
        // We must deselect others
        Composite parent = bt.getParent();
        Control[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            Control child = children[i];
            if (bt != child && child instanceof Button && (child.getStyle() & SWT.RADIO) != 0) {
                ((Button) child).setSelection(false);
            }
        }
        bt.setSelection(true);
    }

    protected void createComponents(Composite composite) {
        // Line 2
        fPattern = createText(composite, SWT.SINGLE | SWT.BORDER, 5, 50);

        // fHistory = createButton(composite, SWT.PUSH, "...", 1);
        // ((GridData) fHistory.getLayoutData()).widthHint = 25;

        fIsCaseSensitiveCheckbox = createButton(composite, SWT.CHECK, SearchMessages.SearchPage_caseSensitive, 5);

        // Line 2 (part 2)
        createLabel(composite, SWT.NONE, "", 5);
        fIsWholeWordCheckbox = createButton(composite, SWT.CHECK, SearchMessages.SearchPage_wholeWord, 5);

        // Line 3
        createLabel(composite, SWT.LEAD, "Scope", 1);
        fWorkspaceScopeRadio = createButton(composite, SWT.RADIO, "&Workspace", 1);

        fModulesScopeRadio = createButton(composite, SWT.RADIO, "&Module(s)", 1);
        fModuleNames = createText(composite, SWT.SINGLE | SWT.BORDER, 2, 50);
        createLabel(composite, SWT.NONE, "", 5);

        // Line 4
        createLabel(composite, SWT.NONE, "", 1);
        fProjectsScopeRadio = createButton(composite, SWT.RADIO, "&Project(s)", 1);

        fProjectNames = createText(composite, SWT.SINGLE | SWT.BORDER, 2, 50);

        fSelectProjects = createButton(composite, SWT.PUSH, "...", 1);
        ((GridData) fSelectProjects.getLayoutData()).widthHint = 25;

        createLabel(composite, SWT.LEAD,
                "\n\nNote: only modules in the PyDev index will be searched (valid modules below a source folder).",
                10);
        createLabel(composite, SWT.LEAD,
                "Note: wildcards may be used for modules and project matching.",
                10);
    }

    protected Text createText(Composite composite, int style, int cols, int charsLen) {
        Text text = new Text(composite, style);
        text.setFont(composite.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, cols, 1);
        data.widthHint = convertWidthInCharsToPixels(charsLen);
        text.setLayoutData(data);
        return text;
    }

    protected Label createLabel(Composite composite, int style, String string, int cols) {
        Label label = new Label(composite, style);
        label.setText(string);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, cols, 1));
        label.setFont(composite.getFont());
        return label;
    }

    protected Button createButton(Composite composite, int style, String string, int cols) {
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

        if (fWorkspaceScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_WORKSPACE, "");
        }

        if (fProjectsScopeRadio.getSelection()) {
            return new ScopeAndData(SearchIndexData.SCOPE_PROJECTS, fProjectNames.getText());
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
                if (last != null) {
                    this.fIsCaseSensitiveCheckbox.setSelection(last.isCaseSensitive);
                    this.fIsWholeWordCheckbox.setSelection(last.isWholeWord);
                }

                // Override some settings from the current selection
                initializeFromSelection(last);
            }
        }
        super.setVisible(visible);

        if (visible && fPattern != null) {
            fPattern.selectAll();
            fPattern.setFocus();
        }

        updateOKStatus();
    }

    protected SearchIndexData initializeFromLast() {
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

    protected void updateOKStatus() {
        fContainer.setPerformActionEnabled(true);
    }

    protected void initializeFromSelection(SearchIndexData last) {
        ISelection selection = fContainer.getSelection();
        if (selection instanceof ITextSelection && !selection.isEmpty()
                && ((ITextSelection) selection).getLength() > 0) {
            boolean regularPath = true;

            // As we have a checkbox for whole word now, the code below shouldn't be needed anymore.

            // if (selection instanceof TextSelection) {
            //    // If we got a substring, add * as needed before/after.
            //    TextSelection tx = (TextSelection) selection;
            //    IDocument doc = getDocument(tx);
            //    if (doc != null) {
            //        int offset = tx.getOffset();
            //        int length = tx.getLength();
            //        try {
            //            String txt = doc.get(offset, length);
            //            if (!txt.startsWith("*")) {
            //                if (offset > 0) {
            //                    char c = doc.getChar(offset - 1);
            //                    if (Character.isJavaIdentifierPart(c)) {
            //                        txt = '*' + txt;
            //                    }
            //                }
            //            }
            //
            //            if (!txt.endsWith("*")) {
            //                if (doc.getLength() > offset + length) {
            //                    char c = doc.getChar(offset + length);
            //                    if (Character.isJavaIdentifierPart(c)) {
            //                        txt = txt + '*';
            //                    }
            //                }
            //            }
            //            fPattern.setText(txt);
            //            regularPath = false;
            //        } catch (BadLocationException e) {
            //            // Ignore
            //        }
            //    }
            // }

            if (regularPath) {
                String text = ((ITextSelection) selection).getText();
                if (text != null) {
                    fPattern.setText(text);
                }
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
            if (initializeScopeFromLast(last)) {
                return;
            }
        }

        //All others failed: go for workspace selection
        this.fWorkspaceScopeRadio.setSelection(true);

    }

    // Hack to get document from text selection.
    private IDocument getDocument(TextSelection tx) {
        try {
            Method method = TextSelection.class.getDeclaredMethod("getDocument");
            method.setAccessible(true);
            return (IDocument) method.invoke(tx);
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

    protected boolean initializeScopeFromLast(SearchIndexData last) {
        int scope = last.scope;
        switch (scope) {
            case SearchIndexData.SCOPE_WORKSPACE:
                this.fWorkspaceScopeRadio.setSelection(true);
                return true;

            case SearchIndexData.SCOPE_MODULES:
                this.fModulesScopeRadio.setSelection(true);
                return true;

            case SearchIndexData.SCOPE_PROJECTS:
                this.fProjectsScopeRadio.setSelection(true);
                return true;
        }

        return false;
    }

    /**
     * Subclasses should override so that given the selected resource the project names/ module names are properly filled
     * for the initial values.
     */
    protected abstract void checkSelectedResource(Collection<String> projectNames, Collection<String> moduleNames,
            IResource resource);
}
