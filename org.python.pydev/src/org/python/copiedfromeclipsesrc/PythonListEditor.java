/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.copiedfromeclipsesrc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * An abstract field editor that manages a list of input values. The editor displays a list containing the values, buttons for adding and
 * removing values, and Up and Down buttons to adjust the order of elements in the list.
 * <p>
 * Subclasses must implement the <code>parseString</code>,<code>createList</code>, and <code>getNewInputObject</code> framework
 * methods.
 * </p>
 * 
 * NOTE: COPIED only because we want removePressed to be protected
 */
public abstract class PythonListEditor extends FieldEditor {

    /**
     * The list widget; <code>null</code> if none (before creation or after disposal).
     */
    protected List list;

    /**
     * The button box containing the Add, Remove, Up, and Down buttons; <code>null</code> if none (before creation or after disposal).
     */
    private Composite buttonBox;

    /**
     * The Add button.
     */
    private Button addButton;

    /**
     * The Remove button.
     */
    protected Button removeButton;

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

    /**
     * Creates a new list field editor
     */
    protected PythonListEditor() {
    }

    /**
     * Creates a list field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected PythonListEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    /**
     * Notifies that the Add button has been pressed.
     */
    protected void addPressed() {
        setPresentsDefaultValue(false);
        String input = getNewInputObject();

        if (input != null) {
            int index = list.getSelectionIndex();
            if (index >= 0)
                list.add(input, index + 1);
            else
                list.add(input, 0);
            selectionChanged();
        }
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) list.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     * 
     * @param box the box for the buttons
     */
    private void createButtons(Composite box) {
        addButton = createPushButton(box, "ListEditor.add");//$NON-NLS-1$
        removeButton = createPushButton(box, "ListEditor.remove");//$NON-NLS-1$
        upButton = createPushButton(box, "ListEditor.up");//$NON-NLS-1$
        downButton = createPushButton(box, "ListEditor.down");//$NON-NLS-1$
    }

    /**
     * Combines the given list of items into a single string. This method is the converse of <code>parseString</code>.
     * <p>
     * Subclasses must implement this method.
     * </p>
     * 
     * @param items the list of items
     * @return the combined string
     * @see #parseString
     */
    protected abstract String createList(String[] items);

    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createPushButton(Composite parent, String key) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(JFaceResources.getString(key));
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
//        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
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
                } else if (widget == list) {
                    selectionChanged();
                }
            }
        };
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        list = getListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        list.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    protected void doLoad() {
        if (list != null) {
            String s = getPreferenceStore().getString(getPreferenceName());
            String[] array = parseString(s);
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    protected void doLoadDefault() {
        if (list != null) {
            list.removeAll();
            String s = getPreferenceStore().getDefaultString(getPreferenceName());
            String[] array = parseString(s);
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    protected void doStore() {
        String s = createList(list.getItems());
        if (s != null)
            getPreferenceStore().setValue(getPreferenceName(), s);
    }

    /**
     * Notifies that the Down button has been pressed.
     */
    protected void downPressed() {
        swap(false);
    }

    /**
     * Returns this field editor's button box containing the Add, Remove, Up, and Down button.
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
    public List getListControl(Composite parent) {
        if (list == null) {
            list = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            list.setFont(parent.getFont());
            list.addSelectionListener(getSelectionListener());
            list.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    list = null;
                }
            });
        } else {
            checkParent(list, parent);
        }
        return list;
    }

    /**
     * Creates and returns a new item for the list.
     * <p>
     * Subclasses must implement this method.
     * </p>
     * 
     * @return a new item
     */
    protected abstract String getNewInputObject();

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns this field editor's selection listener. The listener is created if nessessary.
     * 
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (selectionListener == null)
            createSelectionListener();
        return selectionListener;
    }

    /**
     * Returns this field editor's shell.
     * <p>
     * This method is internal to the framework; subclassers should not call this method.
     * </p>
     * 
     * @return the shell
     */
    protected Shell getShell() {
        if (addButton == null)
            return null;
        return addButton.getShell();
    }

    /**
     * Splits the given string into a list of strings. This method is the converse of <code>createList</code>.
     * <p>
     * Subclasses must implement this method.
     * </p>
     * 
     * @param stringList the string
     * @return an array of <code>String</code>
     * @see #createList
     */
    protected abstract String[] parseString(String stringList);

    /**
     * Notifies that the Remove button has been pressed.
     */
    protected void removePressed() {
        setPresentsDefaultValue(false);
        int index = list.getSelectionIndex();
        if (index >= 0) {
            list.remove(index);
            selectionChanged();
        }
    }

    /**
     * Notifies that the list selection has changed.
     */
    protected void selectionChanged() {

        int index = list.getSelectionIndex();
        int size = list.getItemCount();

        removeButton.setEnabled(index >= 0);
        upButton.setEnabled(size > 1 && index > 0);
        downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    public void setFocus() {
        if (list != null) {
            list.setFocus();
        }
    }

    /**
     * Moves the currently selected item up or down.
     * 
     * @param up <code>true</code> if the item should move up, and <code>false</code> if it should move down
     */
    private void swap(boolean up) {
        setPresentsDefaultValue(false);
        int index = list.getSelectionIndex();
        int target = up ? index - 1 : index + 1;

        if (index >= 0) {
            String[] selection = list.getSelection();
            Assert.isTrue(selection.length == 1);
            list.remove(index);
            list.add(selection[0], target);
            list.setSelection(target);
        }
        selectionChanged();
    }

    /**
     * Notifies that the Up button has been pressed.
     */
    protected void upPressed() {
        swap(true);
    }

    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getListControl(parent).setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
    }
}
