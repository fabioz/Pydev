/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.dialogs.DialogMemento;

/**
 * This dialog will select an existing entry or give the user a chance to create a new one.
 * 
 * It's always persisted in the preference store and values are gotten from there (users just
 * need to pass the key which it should manage -- internally values are stored as a list separated
 * by '|').
 */
public class SelectExistingOrCreateNewDialog extends TreeSelectionDialog implements SelectionListener {

    /**
     * Text when nothing is selected (so, anything written will be the text executed).
     */
    private static final String NEW_ENTRY_TEXT = "New entry (whathever is written in the text field).";

    /**
     * Helper for saving/restoring the dialog position
     */
    private final DialogMemento memento;

    /**
     * Preference store from where the preferences key is gotten.
     */
    private final IPreferenceStore preferenceStore;

    /**
     * Key where the commands available should be set.
     */
    private String preferenceKey;

    /**
     * List of commands available (obtained from the preferences). Note that it may change on delete
     * and it should be used when the user presses OK to save the current commands.
     */
    private List<String> input;

    private Button btAdd;

    private Button btRemove;

    /**
     * @param preferenceStore the store from where we should get the preferences key.
     * @param preferenceKey this is a key where the value is a string with substrings separated by '|'.
     * @param shellMementoId the id for saving the memento settings for the dialog.
     */
    public SelectExistingOrCreateNewDialog(Shell parent, IPreferenceStore preferenceStore, String preferenceKey,
            String shellMementoId) {

        super(parent, new ToStringLabelProvider(), new StringFromListContentProvider());
        this.memento = new DialogMemento(parent, shellMementoId);
        this.preferenceStore = preferenceStore;

        final String initialValue = preferenceStore.getString(preferenceKey);

        this.preferenceKey = preferenceKey;

        this.setInput(StringUtils.split(initialValue, '|'));
        this.setAllowMultiple(false);
        this.setValidator(createValidator());
        this.setHelpAvailable(false);
        //as we have many special things about deleting, filtering in this class, it's important that
        //elements are up to date.
        this.updateInThread = false;
    }

    /**
     * Creates a special validator that considers that items may be gotten from what's filtered (not only actually selected).
     */
    private ISelectionStatusValidator createValidator() {
        return new ISelectionStatusValidator() {

            public IStatus validate(Object[] selection) {
                if (selection != null && selection.length == 1) {
                    return new Status(IStatus.OK, PydevPlugin.getPluginID(), getEntry(selection[0].toString()));
                }
                TreeItem[] items = getTreeViewer().getTree().getItems();
                if (selection == null || selection.length == 0) {
                    //not available in selection
                    if (items != null) {
                        if (items.length == 1) {
                            return new Status(IStatus.OK, PydevPlugin.getPluginID(), getEntry(items[0].getData()
                                    .toString()));
                        }
                        if (items.length > 0) {
                            String textInEditor = text.getText();
                            for (TreeItem item : items) {
                                if (item.getData().toString().equals(textInEditor)) {
                                    //exact match of what's written to an item, so, just use it.
                                    return new Status(IStatus.OK, PydevPlugin.getPluginID(), textInEditor);
                                }
                            }
                        }
                    }
                }

                if ((selection == null || selection.length == 0) && (items == null || items.length == 0)) {
                    return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), "No selection available.");
                }

                return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), "Only 1 entry may be selected or visible.");
            }

            private String getEntry(String string) {
                if (NEW_ENTRY_TEXT.equals(string)) {
                    return text.getText();
                }
                return string;
            }
        };
    }

    @Override
    public boolean close() {
        memento.writeSettings(getShell());
        return super.close();
    }

    @Override
    public Control createDialogArea(Composite parent) {
        memento.readSettings();
        Control ret = super.createDialogArea(parent);

        getTreeViewer().getTree().addKeyListener(new KeyListener() {

            /**
             * Support for deleting the current selection on del or backspace.
             */
            public void keyReleased(KeyEvent e) {
                if (e.keyCode == SWT.DEL || e.keyCode == SWT.BS) {
                    removeSelection();
                }
            }

            public void keyPressed(KeyEvent e) {

            }
        });

        Tree tree = getTreeViewer().getTree();
        GridData layoutData = (GridData) tree.getLayoutData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        return ret;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#createTreeViewer(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected TreeViewer createTreeViewer(Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 0;
        composite.setLayout(gridLayout);
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(layoutData);

        TreeViewer ret = super.createTreeViewer(composite);

        Composite buttonBox = new Composite(composite, SWT.NULL);
        GridData gridData = new GridData(SWT.END, SWT.FILL, false, false);
        buttonBox.setLayoutData(gridData);

        GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 0;
        buttonBox.setLayout(layout);
        btAdd = createPushButton(buttonBox, "Add");
        btRemove = createPushButton(buttonBox, "Remove (DEL)");

        return ret;
    }

    private Button createPushButton(Composite parent, String text) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);

        button.addSelectionListener(this);
        return button;
    }

    /**
     * Returns the number of pixels corresponding to the
     * given number of horizontal dialog units.
     * <p>
     * Clients may call this framework method, but should not override it.
     * </p>
     *
     * @param control the control being sized
     * @param dlus the number of horizontal dialog units
     * @return the number of pixels
     */
    protected int convertHorizontalDLUsToPixels(Control control, int dlus) {
        GC gc = new GC(control);
        gc.setFont(control.getFont());
        int averageWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();

        double horizontalDialogUnitSize = averageWidth * 0.25;

        return (int) Math.round(dlus * horizontalDialogUnitSize);
    }

    @Override
    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

    /*
     * @see SelectionStatusDialog#computeResult()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void computeResult() {
        doFinalUpdateBeforeComputeResult();

        IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
        List list = selection.toList();
        if (list.size() == 1) {
            Object selected = list.get(0);
            if (NEW_ENTRY_TEXT.equals(selected)) {
                list = newCommand();
            }
            setResult(list);
        } else {
            TreeItem[] items = getTreeViewer().getTree().getItems();
            if (items.length == 1) {
                //there is only one item filtered in the tree it may be that one (or a custom).
                list = new ArrayList();
                Object entry = items[0].getData();
                if (NEW_ENTRY_TEXT.equals(entry)) {
                    list = newCommand();
                } else {
                    list.add(entry);
                }
                setResult(list);
            } else if (items.length > 1) {
                String textInEditor = text.getText();
                for (TreeItem item : items) {
                    if (item.getData().toString().equals(textInEditor)) {
                        //exact match of what's written to an item, so, just use it.
                        list = new ArrayList();
                        list.add(textInEditor);
                    }
                }
                setResult(list);
            }
        }
    }

    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == btAdd) {
            InputDialog dialog = new InputDialog(getShell(), "Add custom command to list",
                    "Add custom command to list", "", new IInputValidator() {

                        public String isValid(String newText) {
                            if (newText.trim().length() == 0) {
                                return "Command not entered.";
                            }
                            if (input.contains(newText)) {
                                return "Command already entered.";
                            }
                            return null;
                        }
                    });
            int open = dialog.open();
            if (open == InputDialog.OK) {
                String value = dialog.getValue();
                input.add(value);
                saveCurrentCommands(value); //Save it.
                updateGui();
            }
        } else if (source == btRemove) {
            removeSelection();

        }
    }

    public void widgetDefaultSelected(SelectionEvent e) {
        //Do nothing.
    }

    /**
     * Creates a new command (should be used only when OK is pressed, as it will add that
     * command to the key in the preferences)
     * 
     * @return a list with a single command gotten from the text in the text field.
     */
    private List<String> newCommand() {
        ArrayList<String> list = new ArrayList<String>();
        String newCommand = this.text.getText().trim();
        if (newCommand.length() > 0) {
            list.add(newCommand);
            saveCurrentCommands(newCommand);
        }
        return list;
    }

    /**
     * Saves the current list of commands in the preferences, adding the one passed as a parameter
     * if it is not null.
     */
    private void saveCurrentCommands(String newCommand) {
        ArrayList<String> newCommands = new ArrayList<String>(input);
        if (newCommand != null && !input.contains(newCommand)) {
            newCommands.add(newCommand);
        }
        newCommands.remove(NEW_ENTRY_TEXT); //never save this entry.
        preferenceStore.setValue(preferenceKey, StringUtils.join("|", newCommands));
    }

    /**
     * Caches the entries that are currently accepted to show in the tree.
     */
    Set<String> currentlyAccepted = new HashSet<String>();

    /**
     * Overridden because we want to update the pre-computed list of accepted entries.
     */
    @Override
    protected void setFilter(String text, IProgressMonitor monitor, boolean updateFilter) {
        if (updateFilter) {
            if (fFilterMatcher.lastPattern.equals(text)) {
                //no actual change...
                return;
            }
            fFilterMatcher.setFilter(text);
            if (monitor.isCanceled()) {
                return;
            }
        }

        updateFilterEntries(monitor);
        //the filter is already updated in this class.
        super.setFilter(text, monitor, false);
    }

    /**
     * Whenever the update finishes, we have to update our OK status because it depends not only
     * on the selection, but also on the visible items.
     */
    @Override
    protected void onFinishUpdateJob() {
        updateOKStatus();
    }

    /**
     * Updates what should be shown in the tree. We have to override because if we
     * don't match anything we want to add a NEW_ENTRY_TEXT.
     */
    private void updateFilterEntries(IProgressMonitor monitor) {
        currentlyAccepted.clear();
        for (String s : input) {
            if (NEW_ENTRY_TEXT.equals(s)) {
                continue;
            }

            if (fFilterMatcher.match(s)) {
                currentlyAccepted.add(s);
            }
            if (monitor.isCanceled()) {
                return;
            }
        }
        if (currentlyAccepted.size() == 0) {
            currentlyAccepted.add(NEW_ENTRY_TEXT);
        }
    }

    /**
     * Overridden to get the input set and always add a NEW_ENTRY_TEXT if it's still not there.
     * (and also update the pre-computed filter entries accepted on a new input).
     */
    @Override
    public void setInput(Object input) {
        this.input = (List<String>) input;
        if (this.input.indexOf(NEW_ENTRY_TEXT) == -1) {
            this.input.add(NEW_ENTRY_TEXT);
        }
        super.setInput(input);
        this.updateFilterEntries(new NullProgressMonitor());
    }

    /**
     * Overridden because of the special support for having a NEW_ENTRY_TEXT if nothing matches
     * the current text (so, we pre-compute what's accepted and only check that here).
     */
    @Override
    protected boolean matchItemToShowInTree(Object element) {
        return this.currentlyAccepted.contains(element);
    }

    private void updateGui() {
        setFilter(text.getText(), new NullProgressMonitor(), false);
        updateSelectionIfNothingSelected(getTreeViewer().getTree());
    }

    private void removeSelection() {
        IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
        List<String> list = selection.toList();
        for (String s : list) {
            if (NEW_ENTRY_TEXT.equals(s)) {
                continue; //don't delete this one.
            }
            input.remove(s);
        }
        saveCurrentCommands(null);

        //updates the selection
        updateGui();
    }

}

/**
 * Transform anything that gets here into a string.
 */
final class ToStringLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        return "" + element;
    }
}

/**
 * Works with lists of strings
 */
final class StringFromListContentProvider implements ITreeContentProvider {

    public Object[] getChildren(Object element) {
        if (element instanceof List) {
            List list = (List) element;
            return list.toArray();
        }
        return new Object[0];
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof List && ((List) element).size() > 0;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
        //do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}
