/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.debug.ui.actions.PyExceptionListProvider;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractPydevPrefs;
import org.python.pydev.shared_core.string.StringMatcher;

public class PyConfigureExceptionDialog extends SelectionDialog {

    protected DefaultFilterMatcher fFilterMatcher = new DefaultFilterMatcher();
    protected boolean updateInThread = true;

    // the visual selection widget group
    private Text filterPatternField;
    private Text addNewExceptionField;

    // providers for populating this dialog
    private ILabelProvider labelProvider;
    private IStructuredContentProvider contentProvider;
    private String filterPattern;

    // the root element to populate the viewer with
    private Object inputElement;

    private FilterJob filterJob;

    // the visual selection widget group
    CheckboxTableViewer listViewer;

    // sizing constants
    private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
    private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

    // enable/disable breaking on the caught
    private Button uncaughtExceptionCheck;
    private boolean handleCaughtExceptions;

    private Button caughtExceptionCheck;
    private boolean handleUncaughtExceptions;

    private Button stopOnExceptionsHandledInSameContextCheck;
    private boolean stopOnExceptionsHandledInSameContext;

    private Button ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck;
    private boolean ignoreExceptionsThrownInLinesWithIgnoreException;

    private Button breakOnDjangoTemplateExceptionsCheck;
    private boolean handleBreakOnDjangoTemplateExceptions;

    protected static String SELECT_ALL_TITLE = WorkbenchMessages.SelectionDialog_selectLabel;
    protected static String DESELECT_ALL_TITLE = WorkbenchMessages.SelectionDialog_deselectLabel;

    public PyConfigureExceptionDialog(Shell parentShell, Object input, IStructuredContentProvider contentProvider,
            ILabelProvider labelProvider, String message) {
        super(parentShell);
        setTitle(WorkbenchMessages.ListSelection_title);
        this.inputElement = input;
        this.contentProvider = contentProvider;
        this.labelProvider = labelProvider;
        if (message != null) {
            setMessage(message);
        } else {
            setMessage(WorkbenchMessages.ListSelection_message);
        }
    }

    /**
     * 
     * @param composite
     *            the parent composite
     * @return the message label
     */
    @Override
    protected Label createMessageArea(Composite composite) {
        Label filterLabel = new Label(composite, SWT.NONE);
        filterLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
        filterLabel.setText("Enter a filter (* = any number of " + "characters, ? = any single character)"
                + "\nor an empty string for no filtering:");

        filterPatternField = new Text(composite, SWT.BORDER);
        filterPatternField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        return filterLabel;
    }

    /**
     * Add the selection and deselection buttons to the dialog.
     * 
     * @param composite
     *            org.eclipse.swt.widgets.Composite
     */
    protected void createSelectionButtons(Composite composite) {
        Composite buttonComposite = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        buttonComposite.setLayout(layout);
        buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

        createSelectAll(buttonComposite);
        createDeselectAll(buttonComposite);
    }

    /**
     * Creates a Select All button and its respective listener.
     * 
     * @param buttonComposite
     */
    private void createSelectAll(Composite buttonComposite) {
        Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, SELECT_ALL_TITLE, false);

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listViewer.setAllChecked(true);
            }
        };
        selectButton.addSelectionListener(listener);
    }

    /**
     * Creates a DeSelect All button and its respective listener.
     * 
     * @param buttonComposite
     */
    private void createDeselectAll(Composite buttonComposite) {
        SelectionListener listener;
        Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, DESELECT_ALL_TITLE,
                false);

        listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listViewer.setAllChecked(false);
                TableItem[] currentItems = listViewer.getTable().getItems();
                for (TableItem tableItem : currentItems) {
                    removeFromSelectedElements(tableItem.getText());
                }
            }
        };
        deselectButton.addSelectionListener(listener);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        // page group
        Composite composite = (Composite) super.createDialogArea(parent);

        initializeDialogUnits(composite);

        createMessageArea(composite);

        listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
        data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
        listViewer.getTable().setLayoutData(data);

        listViewer.setLabelProvider(labelProvider);
        listViewer.setContentProvider(contentProvider);

        createSelectionButtons(composite);

        initContent();
        // initialize page
        if (!getInitialElementSelections().isEmpty()) {
            checkInitialSelections();
        }

        Dialog.applyDialogFont(composite);

        getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (getCheckBoxTableViewer().getChecked(element)) {
                    addToSelectedElements(element);
                }
                return matchExceptionToShowInList(element);
            }
        });

        getCheckBoxTableViewer().addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                if (event.getChecked()) {
                    addToSelectedElements(event.getElement());
                } else {
                    removeFromSelectedElements(event.getElement());
                }
            }
        });

        createCustomExceptionUI(composite);
        createDealingWithExceptionsOptions(composite);

        return composite;
    }

    /**
     * @param composite
     * 
     *            Create a new text box and a button, which allows user to add
     *            custom exception. Attach a listener to the AddException Button
     */
    private void createCustomExceptionUI(Composite composite) {
        addNewExceptionField = new Text(composite, SWT.BORDER);
        addNewExceptionField.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        Button buttonAdd = new Button(composite, SWT.PUSH);
        buttonAdd.setLayoutData(new GridData(GridData.END, GridData.END, true, false));
        buttonAdd.setText("Add Exception");

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addCustomException();
            }
        };
        buttonAdd.addSelectionListener(listener);

    }

    /**
     * Add the new exception in the content pane
     * 
     */
    private void addCustomException() {
        String customException = addNewExceptionField.getText().trim();
        Object[] currentElements = contentProvider.getElements(inputElement);

        ArrayList<Object> currentElementsList = new ArrayList<Object>();
        for (int i = 0; i < currentElements.length; ++i) {
            Object element = currentElements[i];
            currentElementsList.add(element);
        }

        if (customException.isEmpty()) {
            return;
        }

        if (!currentElementsList.contains(customException)) {
            getViewer().add(customException);
            addNewExceptionField.setText("");
            ((PyExceptionListProvider) contentProvider).addUserConfiguredException(customException);
        } else {
            IStatus status = new Status(IStatus.WARNING, DebugUIPlugin.getUniqueIdentifier(),
                    "Duplicate: This exception already exists");
            DebugUIPlugin.errorDialog(getShell(), DebugUIPlugin.removeAccelerators("Add Custom User Exception"),
                    "Error", status);
        }
    }

    /**
     * Creates options related to dealing with exceptions. 
     */
    private void createDealingWithExceptionsOptions(Composite composite) {
        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        uncaughtExceptionCheck = new Button(composite, SWT.CHECK);
        uncaughtExceptionCheck.setText("Suspend on uncaught exceptions");
        uncaughtExceptionCheck.setSelection(instance.getBreakOnUncaughtExceptions());

        caughtExceptionCheck = new Button(composite, SWT.CHECK);
        caughtExceptionCheck.setText("Suspend on caught exceptions *");
        caughtExceptionCheck.setSelection(instance.getBreakOnCaughtExceptions());

        stopOnExceptionsHandledInSameContextCheck = new Button(composite, SWT.CHECK);
        stopOnExceptionsHandledInSameContextCheck.setText("    Skip exceptions caught in same function");
        stopOnExceptionsHandledInSameContextCheck.setSelection(instance.getSkipCaughtExceptionsInSameFunction());

        ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck = new Button(composite, SWT.CHECK);
        ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck
                .setText("    Ignore exceptions thrown in lines with # @IgnoreException");
        ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck.setSelection(instance
                .getIgnoreExceptionsThrownInLinesWithIgnoreException());

        caughtExceptionCheck.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateStates();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }
        });
        updateStates();

        Label label = new Label(composite, SWT.NONE);
        label.setText("* Will make debugging ~ 2x slower");

        breakOnDjangoTemplateExceptionsCheck = new Button(composite, SWT.CHECK);
        breakOnDjangoTemplateExceptionsCheck.setText("Suspend on django template render exceptions");
        breakOnDjangoTemplateExceptionsCheck.setSelection(PydevPlugin.getDefault().getPreferenceStore()
                .getBoolean(AbstractPydevPrefs.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS));
    }

    private void updateStates() {
        boolean enable = caughtExceptionCheck.getSelection();
        stopOnExceptionsHandledInSameContextCheck.setEnabled(enable);
        ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck.setEnabled(enable);
    }

    /**
     * Updates the current filter with the text field text.
     */
    protected void doFilterUpdate(IProgressMonitor monitor) {
        setFilter(filterPatternField.getText(), monitor, true);
    }

    // filtering things...
    protected void setFilter(String text, IProgressMonitor monitor, boolean updateFilterMatcher) {
        if (monitor.isCanceled()) {
            return;
        }

        if (updateFilterMatcher) {
            // just so that subclasses may already treat it.
            if (fFilterMatcher.lastPattern.equals(text)) {
                // no actual change...
                return;
            }
            fFilterMatcher.setFilter(text);
            if (monitor.isCanceled()) {
                return;
            }
        }

        getViewer().refresh();
        setSelectedElementChecked();
    }

    protected boolean matchExceptionToShowInList(Object element) {
        return fFilterMatcher.match(element);
    }

    /**
     * The <code>ListSelectionDialog</code> implementation of this
     * <code>Dialog</code> method builds a list of the selected elements for
     * later retrieval by the client and closes this dialog.
     */
    @Override
    protected void okPressed() {

        // Get the input children.
        Object[] children = contentProvider.getElements(inputElement);
        // Build a list of selected children.
        if (children != null) {
            ArrayList<Object> list = new ArrayList<Object>();
            for (int i = 0; i < children.length; ++i) {
                Object element = children[i];
                if (listViewer.getChecked(element)) {
                    list.add(element);
                }
            }
            // If filter is on and checkedElements are not in filtered list
            // then content provider.getElements doesn't fetch the same
            if (selectedElements != null) {
                for (Object selectedElement : selectedElements) {
                    if (!list.contains(selectedElement)) {
                        list.add(selectedElement);
                    }
                }
            }
            setResult(list);
        }

        //Save whether to break debugger or not on caught / uncaught exceptions
        handleCaughtExceptions = caughtExceptionCheck.getSelection();
        handleUncaughtExceptions = uncaughtExceptionCheck.getSelection();
        stopOnExceptionsHandledInSameContext = stopOnExceptionsHandledInSameContextCheck.getSelection();
        ignoreExceptionsThrownInLinesWithIgnoreException = ignoreExceptionsThrownInLinesWithIgnoreExceptionCheck
                .getSelection();

        PydevPlugin.getDefault().getPreferenceStore().setValue(
                AbstractPydevPrefs.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS,
                breakOnDjangoTemplateExceptionsCheck.getSelection());

        super.okPressed();
    }

    public boolean getResultHandleUncaughtExceptions() {
        return this.handleUncaughtExceptions;
    }

    public boolean getResultHandleCaughtExceptions() {
        return this.handleCaughtExceptions;
    }

    public boolean getResultStopOnExceptionsHandledInSameContext() {
        return this.stopOnExceptionsHandledInSameContext;
    }

    public boolean getResultIgnoreExceptionsThrownInLinesWithIgnoreException() {
        return this.ignoreExceptionsThrownInLinesWithIgnoreException;
    }

    /**
     * Returns the viewer used to show the list.
     * 
     * @return the viewer, or <code>null</code> if not yet created
     */
    protected CheckboxTableViewer getViewer() {
        return listViewer;
    }

    /**
     * Returns the viewer cast to the correct instance. Possibly
     * <code>null</code> if the viewer has not been created yet.
     * 
     * @return the viewer cast to CheckboxTableViewer
     */
    protected CheckboxTableViewer getCheckBoxTableViewer() {
        return getViewer();
    }

    /**
     * Initialises this dialog's viewer after it has been laid out.
     */
    private void initContent() {
        listViewer.setInput(inputElement);
        Listener listener = new Listener() {
            public void handleEvent(Event e) {
                if (updateInThread) {
                    if (filterJob != null) {
                        // cancel it if it was already in progress
                        filterJob.cancel();
                    }
                    filterJob = new FilterJob();
                    filterJob.start();
                } else {
                    doFilterUpdate(new NullProgressMonitor());
                }
            }
        };

        filterPatternField.setText(filterPattern != null ? filterPattern : "");
        filterPatternField.addListener(SWT.Modify, listener);
    }

    /**
     * Visually checks the previously-specified elements in this dialog's list
     * viewer.
     */
    private void checkInitialSelections() {
        Iterator itemsToCheck = getInitialElementSelections().iterator();

        while (itemsToCheck.hasNext()) {
            listViewer.setChecked(itemsToCheck.next(), true);
        }
    }

    /**
     * setSelectedElementChecked
     * 
     * Visually checks the elements in the selectedElements list after the
     * refresh, which is triggered on applying / removing filter
     * 
     */
    private void setSelectedElementChecked() {
        if (selectedElements != null) {
            for (Object element : selectedElements) {
                getViewer().setChecked(element, true);
            }
        }
    }

    private List<Object> selectedElements;

    private void addToSelectedElements(Object element) {
        if (selectedElements == null) {
            selectedElements = new ArrayList<Object>();
        }
        if (!selectedElements.contains(element)) {
            selectedElements.add(element);
        }
    }

    private void removeFromSelectedElements(Object element) {
        if (selectedElements != null && selectedElements.contains(element)) {
            selectedElements.remove(element);
        }
    }

    class FilterJob extends Thread {
        // only thing it implements is the cancelled
        IProgressMonitor monitor = new NullProgressMonitor();

        public FilterJob() {
            setPriority(Thread.MIN_PRIORITY);
            setName("PyConfigureExceptionDialog: FilterJob");
        }

        @Override
        public void run() {
            try {
                sleep(300);
            } catch (InterruptedException e) {
                // ignore
            }
            if (!monitor.isCanceled()) {
                Display display = Display.getDefault();
                display.asyncExec(new Runnable() {

                    public void run() {
                        if (!monitor.isCanceled() && filterPatternField != null && !filterPatternField.isDisposed()) {
                            doFilterUpdate(monitor);
                        }
                    }

                });
            }
        }

        public void cancel() {
            this.monitor.setCanceled(true);
        }
    }

    protected class DefaultFilterMatcher {
        public StringMatcher fMatcher;
        public String lastPattern;

        public DefaultFilterMatcher() {
            setFilter("");

        }

        public void setFilter(String pattern) {
            setFilter(pattern, true, false);
        }

        private void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
            fMatcher = new StringMatcher(pattern + '*', ignoreCase, ignoreWildCards);
            this.lastPattern = pattern;
        }

        public boolean match(Object element) {
            boolean match = fMatcher.match(labelProvider.getText(element));
            if (match) {
                return true;
            }
            return false;
        }
    }
}
