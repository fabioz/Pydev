/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * An abstract field editor that manages a list of input values. 
 * The editor displays a list containing the values, buttons for
 * adding and removing values, and Up and Down buttons to adjust
 * the order of elements in the list.
 * <p>
 * Subclasses must implement the <code>parseString</code>,
 * <code>createList</code>, and <code>getNewInputObject</code>
 * framework methods.
 * </p>
 */
public abstract class TableEditor extends FieldEditor {

    /**
     * The list widget; <code>null</code> if none
     * (before creation or after disposal).
     */
    private Table table;

    /**
     * The button box containing the Add, Remove, Up, and Down buttons;
     * <code>null</code> if none (before creation or after disposal).
     */
    private Composite buttonBox;

    /**
     * The Add button.
     */
    private Button addButton;

    /**
     * The Remove button.
     */
    private Button removeButton;

    /**
     * The Up button.
     */
    private Button upButton;

    /**
     * The Down button.
     */
    private Button downButton;

    /**
     * The selection listener.
     */
    private SelectionListener selectionListener;

    private TableColumn col1;

    private TableColumn col2;

    /**
     * Creates a new list field editor 
     */
    protected TableEditor() {
    }

    /**
     * Creates a list field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected TableEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    /**
     * Notifies that the Add button has been pressed.
     */
    private void addPressed() {
        setPresentsDefaultValue(false);
        String[] input = getNewInputObject();

        if (input != null) {
            int index = table.getSelectionIndex();
            TableItem tableItem;
            if (index >= 0) {
                tableItem = new TableItem(table, 0, index + 1);
            } else {
                tableItem = new TableItem(table, 0);
            }
            tableItem.setText(input);
            selectionChanged();
        }
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) table.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     *
     * @param box the box for the buttons
     */
    private void createButtons(Composite box) {
        addButton = createPushButton(box, "Add");//$NON-NLS-1$
        removeButton = createPushButton(box, "Remove");//$NON-NLS-1$
        upButton = createPushButton(box, "Up");//$NON-NLS-1$
        downButton = createPushButton(box, "Down");//$NON-NLS-1$
    }

    /**
     * Combines the given list of items into a single string.
     * This method is the converse of <code>parseString</code>. 
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @param items the list of items
     * @return the combined string
     * @see #parseString
     */
    protected abstract String createTable(List<String[]> items);

    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createPushButton(Composite parent, String key) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(key);
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }

    /**
     * Creates a selection listener.
     */
    public void createSelectionListener() {
        selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == addButton) {
                    addPressed();
                } else if (widget == removeButton) {
                    removePressed();
                } else if (widget == upButton) {
                    upPressed();
                } else if (widget == downButton) {
                    downPressed();
                } else if (widget == table) {
                    selectionChanged();
                }
            }
        };
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        table = getTableControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        table.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void doLoad() {
        if (table != null) {
            String s = getPreferenceStore().getString(getPreferenceName());
            List<String[]> array = parseString(s);
            for (String[] data : array) {
                TableItem tableItem = new TableItem(table, 0);
                tableItem.setText(data);
            }
        }
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void doLoadDefault() {
        if (table != null) {
            table.removeAll();
            String s = getPreferenceStore().getDefaultString(getPreferenceName());
            List<String[]> array = parseString(s);
            for (String[] tup : array) {
                TableItem tableItem = new TableItem(table, 0);
                tableItem.setText(tup);
            }
        }
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void doStore() {
        TableItem[] items = table.getItems();
        ArrayList<String[]> list = new ArrayList<String[]>();
        for (TableItem tableItem : items) {
            list.add(new String[] { tableItem.getText(0), tableItem.getText(1) });
        }
        String s = createTable(list);
        if (s != null) {
            getPreferenceStore().setValue(getPreferenceName(), s);
        }
    }

    /**
     * Notifies that the Down button has been pressed.
     */
    private void downPressed() {
        swap(false);
    }

    /**
     * Returns this field editor's button box containing the Add, Remove,
     * Up, and Down button.
     *
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControl(Composite parent) {
        if (buttonBox == null) {
            buttonBox = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            buttonBox.setLayout(layout);
            createButtons(buttonBox);
            buttonBox.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    addButton = null;
                    removeButton = null;
                    upButton = null;
                    downButton = null;
                    buttonBox = null;
                }
            });

        } else {
            checkParent(buttonBox, parent);
        }

        selectionChanged();
        return buttonBox;
    }

    /**
     * Returns this field editor's list control.
     *
     * @param parent the parent control
     * @return the list control
     */
    public Table getTableControl(Composite parent) {
        if (table == null) {
            table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

            table.setHeaderVisible(true);

            col1 = new TableColumn(table, SWT.LEFT);
            col1.setText("Path to translate");
            col1.setWidth(200);
            col2 = new TableColumn(table, SWT.LEFT);
            col2.setText("Translated path");
            col2.setWidth(200);

            table.setFont(parent.getFont());
            table.addSelectionListener(getSelectionListener());
            table.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    col1 = null;
                    col2 = null;
                    table = null;
                }
            });
        } else {
            checkParent(table, parent);
        }
        return table;
    }

    /**
     * Creates and returns a new item for the list.
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @return a new item
     */
    protected abstract String[] getNewInputObject();

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns this field editor's selection listener.
     * The listener is created if nessessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (selectionListener == null) {
            createSelectionListener();
        }
        return selectionListener;
    }

    /**
     * Returns this field editor's shell.
     * <p>
     * This method is internal to the framework; subclassers should not call
     * this method.
     * </p>
     *
     * @return the shell
     */
    protected Shell getShell() {
        if (addButton == null) {
            return null;
        }
        return addButton.getShell();
    }

    /**
     * Splits the given string into a list of strings.
     * This method is the converse of <code>createTable</code>. 
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @param stringList the string
     * @return an array of <code>String</code>
     * @see #createList
     */
    protected abstract List<String[]> parseString(String stringList);

    /**
     * Notifies that the Remove button has been pressed.
     */
    private void removePressed() {
        setPresentsDefaultValue(false);
        int index = table.getSelectionIndex();
        if (index >= 0) {
            table.remove(index);
            selectionChanged();
        }
    }

    /**
     * Notifies that the list selection has changed.
     */
    private void selectionChanged() {

        int index = table.getSelectionIndex();
        int size = table.getItemCount();

        removeButton.setEnabled(index >= 0);
        upButton.setEnabled(size > 1 && index > 0);
        downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    public void setFocus() {
        if (table != null) {
            table.setFocus();
        }
    }

    /**
     * Moves the currently selected item up or down.
     *
     * @param up <code>true</code> if the item should move up,
     *  and <code>false</code> if it should move down
     */
    private void swap(boolean up) {
        setPresentsDefaultValue(false);
        int index = table.getSelectionIndex();
        int target = up ? index - 1 : index + 1;

        if (index >= 0) {
            TableItem targetItem = table.getItem(target);
            TableItem sourceItem = table.getItem(index);

            String[] targetContents = new String[] { targetItem.getText(0), targetItem.getText(1) };
            String[] sourceContents = new String[] { sourceItem.getText(0), sourceItem.getText(1) };

            targetItem.setText(sourceContents);
            sourceItem.setText(targetContents);

            table.setSelection(target);
        }
        selectionChanged();
    }

    /**
     * Notifies that the Up button has been pressed.
     */
    private void upPressed() {
        swap(true);
    }

    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getTableControl(parent).setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
    }
}
