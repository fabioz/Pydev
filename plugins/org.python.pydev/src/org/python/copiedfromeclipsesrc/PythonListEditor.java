/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.copiedfromeclipsesrc;

import java.net.MalformedURLException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.ui.pythonpathconf.NameAndExecutable;
import org.python.pydev.ui.pythonpathconf.conda.CondaConfigDialog;

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

    public static boolean USE_ICONS = true;

    /**
     * The list widget; <code>null</code> if none (before creation or after disposal).
     */
    private Tree treeWithInterpreters;

    /**
     * The button box containing the Add, Remove, Up, and Down buttons; <code>null</code> if none (before creation or after disposal).
     */
    private Composite buttonBox;

    /**
     * The first ToolBar item.
     */
    private ToolItem initialTextItem;

    /**
     * The Add button.
     */
    private MenuItem addMenuItem;

    /**
     * The Quick Auto config button.
     */
    protected MenuItem autoConfigMenuItem;

    /**
     * The Pipenv config button (may be null as it's only available for cpython).
     */
    protected MenuItem pipenvConfigMenuItem;

    /**
     * The Avanced Auto config button.
     */
    protected MenuItem advAutoConfigMenuItem;

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
     * The image to be shown in each interpreter.
     */
    private Image imageInterpreter;

    private Menu menu;

    private Button configCondaButton;

    private Composite parent;

    protected abstract IInterpreterManager getInterpreterManager();

    /**
     * Creates a new list field editor
     */
    protected PythonListEditor() {
        if (USE_ICONS) {
            imageInterpreter = ImageCache.asImage(SharedUiPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON));
        }
    }

    /**
     * Creates a list field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected PythonListEditor(String name, String labelText, Composite parent) {
        this();
        init(name, labelText);
        createControl(parent);
    }

    /**
     * Notifies that one of the Config buttons (or Add) has been pressed.
     *
     * @param configType the type of configuration to use when creating the new interpreter.
     */
    public void addPressed(int configType) {
        NameAndExecutable input = getNewInputObject(configType);
        if (input != null) {
            if (input.o1 != null && input.o2 != null) {
                setPresentsDefaultValue(false);
                TreeItem item = createInterpreterItem(input.o1, input.o2);
                try {
                    treeWithInterpreters.setSelection(item);
                } catch (Exception e) {
                    Log.log(e);
                }
                selectionChanged();
                this.updateTree();
            }
        }
    }

    protected abstract void updateTree();

    /**
     * Adds a new tree item to the interpreter tree.
     * @return
     */
    protected TreeItem createInterpreterItem(String name, String executable) {
        TreeItem item = new TreeItem(treeWithInterpreters, SWT.NULL);
        item.setText(new String[] { name, executable });
        item.setImage(this.imageInterpreter);
        return item;
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) treeWithInterpreters.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     *
     * @param box the box for the buttons
     * @throws MalformedURLException 
     */
    private void createButtons(Composite box) throws MalformedURLException {
        parent = box.getParent();
        final int interpreterType = getInterpreterManager().getInterpreterType();
        String selectTitle = "";
        switch (interpreterType) {
            case IPythonNature.INTERPRETER_TYPE_PYTHON:
                selectTitle = "Brows&e for python/pypy exe";
                break;
            case IPythonNature.INTERPRETER_TYPE_JYTHON:
                selectTitle = "Brows&e for Jython jar";
                break;
            case IPythonNature.INTERPRETER_TYPE_IRONPYTHON:
                selectTitle = "Brows&e for ipy exe";
                break;

            default:
                Log.log("Unhandled type: " + interpreterType);
                selectTitle = "Select executable";
        }

        ToolBar toolBar = new ToolBar(box, SWT.HORIZONTAL);

        initialTextItem = new ToolItem(toolBar, SWT.DROP_DOWN);
        initialTextItem.setText("New");
        initialTextItem.addSelectionListener(getSelectionListener());

        menu = new Menu(toolBar);

        addMenuItem = createMenuItem(menu, selectTitle);

        if (interpreterType == IPythonNature.INTERPRETER_TYPE_PYTHON) {
            pipenvConfigMenuItem = createMenuItem(menu,
                    InterpreterConfigHelpers.CONFIG_PIPENV_NAME);
        }

        autoConfigMenuItem = createMenuItem(menu,
                InterpreterConfigHelpers.CONFIG_AUTO_NAME);

        advAutoConfigMenuItem = createMenuItem(menu,
                InterpreterConfigHelpers.CONFIG_ADV_AUTO_NAME);
        advAutoConfigMenuItem
                .setToolTipText(
                        "Choose from a list of valid interpreters, and select the folders to be in the SYSTEM pythonpath.");

        removeButton = createButton(box, UIConstants.REMOVE);
        upButton = createButton(box, SWT.ARROW | SWT.UP);
        downButton = createButton(box, SWT.ARROW | SWT.DOWN);

        configCondaButton = createPushButton(box,
                "Config conda");
    }

    /**
     * This method is not longer used!
     */
    protected String createList(String[] items) {
        throw new RuntimeException("doLoad/doStore should be overridden (so that it's not needed)");
    }

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

    private Button createButton(Composite parent, int style) {
        Button button = new Button(parent, style);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        //        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }

    private Button createButton(Composite parent, String imageURL) {
        Button button = new Button(parent, SWT.NONE);
        button.setImage(ImageCache.asImage(SharedUiPlugin.getImageCache().get(imageURL)));
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        //        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }

    private MenuItem createMenuItem(Menu menu, String key) {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE);
        menuItem.setText(key);
        menuItem.addSelectionListener(getSelectionListener());
        return menuItem;
    }

    /**
     * Creates a selection listener.
     */
    public void createSelectionListener() {
        selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == initialTextItem) {
                    initialPressed();
                } else if (widget == addMenuItem) {
                    addPressed(InterpreterConfigHelpers.CONFIG_MANUAL);
                } else if (widget == autoConfigMenuItem) {
                    addPressed(InterpreterConfigHelpers.CONFIG_AUTO);
                } else if (pipenvConfigMenuItem != null && widget == pipenvConfigMenuItem) {
                    addPressed(InterpreterConfigHelpers.CONFIG_PIPENV);
                } else if (widget == advAutoConfigMenuItem) {
                    addPressed(InterpreterConfigHelpers.CONFIG_ADV_AUTO);
                } else if (widget == configCondaButton) {
                    configCondaPressed();
                } else if (widget == removeButton) {
                    removePressed();
                } else if (widget == upButton) {
                    upPressed();
                } else if (widget == downButton) {
                    downPressed();
                } else if (widget == treeWithInterpreters) {
                    selectionChanged();
                }
            }
        };
    }

    public void initialPressed() {
        Rectangle rect = initialTextItem.getBounds();
        Point pt = initialTextItem.getParent().toDisplay(new Point(rect.x, rect.y));
        menu.setLocation(pt.x, pt.y + rect.height);
        menu.setVisible(true);
    }

    private void configCondaPressed() {
        IInterpreterInfo[] interpreterInfos = getInterpreterManager().getInterpreterInfos();
        CondaConfigDialog condaConfigDialog = new CondaConfigDialog(parent.getShell(), interpreterInfos);
        condaConfigDialog.open();
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        treeWithInterpreters = getListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        treeWithInterpreters.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected abstract void doLoad();

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected abstract void doLoadDefault();

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected abstract void doStore();

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
            try {
                createButtons(buttonBox);
            } catch (MalformedURLException e) {
                Log.log(e);
            }
            buttonBox.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    addMenuItem = null;
                    autoConfigMenuItem = null;
                    pipenvConfigMenuItem = null;
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
    public Tree getListControl(Composite parent) {
        if (treeWithInterpreters == null) {
            treeWithInterpreters = new Tree(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);

            treeWithInterpreters.setHeaderVisible(true);
            TreeColumn column1 = new TreeColumn(treeWithInterpreters, SWT.LEFT);
            column1.setText("Name");
            column1.setWidth(200);
            TreeColumn column2 = new TreeColumn(treeWithInterpreters, SWT.LEFT);
            column2.setText("Location");
            column2.setWidth(200);

            treeWithInterpreters.setFont(parent.getFont());
            treeWithInterpreters.addSelectionListener(getSelectionListener());
            treeWithInterpreters.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    treeWithInterpreters = null;
                }
            });
        } else {
            checkParent(treeWithInterpreters, parent);
        }
        return treeWithInterpreters;
    }

    /**
     * Creates and returns a new item for the list.
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @return the name and executable of the new item
     */
    protected abstract NameAndExecutable getNewInputObject(int configType);

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns this field editor's selection listener. The listener is created if nessessary.
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
     * This method is internal to the framework; subclassers should not call this method.
     * </p>
     *
     * @return the shell
     */
    protected Shell getShell() {
        if (addMenuItem == null) {
            return null;
        }
        return addMenuItem.getParent().getShell();
    }

    /**
     * This method is no longer used.
     */
    protected String[] parseString(String stringList) {
        throw new RuntimeException("doLoad/doStore should be overridden (so that it's not needed)");
    }

    /**
     * Notifies that the Remove button has been pressed.
     */
    protected void removePressed() {
        setPresentsDefaultValue(false);
        TreeItem[] selection = treeWithInterpreters.getSelection();
        if (selection != null && selection.length > 0) {
            for (TreeItem t : selection) {
                disposeOfTreeItem(t);
            }
            selectionChanged();
            updateTree();
        }
    }

    protected void disposeOfTreeItem(TreeItem t) {
        t.dispose();
    }

    /**
     * Notifies that the list selection has changed.
     */
    protected void selectionChanged() {
        int index = getSelectionIndex();
        int size = treeWithInterpreters.getItemCount();

        removeButton.setEnabled(index >= 0);
        upButton.setEnabled(size > 1 && index > 0);
        downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    public void setFocus() {
        if (treeWithInterpreters != null) {
            treeWithInterpreters.setFocus();
        }
    }

    protected int getSelectionIndex() {
        if (this.treeWithInterpreters.getSelectionCount() != 1) {
            return -1;
        }

        TreeItem[] selection = treeWithInterpreters.getSelection();
        int index = -1;
        if (selection != null && selection.length > 0) {
            index = treeWithInterpreters.indexOf(selection[0]);
        }
        return index;
    }

    /**
     * Moves the currently selected item up or down.
     *
     * @param up <code>true</code> if the item should move up, and <code>false</code> if it should move down
     */
    private void swap(boolean up) {
        setPresentsDefaultValue(false);
        int index = getSelectionIndex();
        int target = up ? index - 1 : index + 1;

        if (index >= 0) {
            TreeItem curr = treeWithInterpreters.getItem(index);
            TreeItem replace = treeWithInterpreters.getItem(target);

            //Just update the text!
            String col0 = replace.getText(0);
            String col1 = replace.getText(1);
            replace.setText(new String[] { curr.getText(0), curr.getText(1) });
            curr.setText(new String[] { col0, col1 });

            treeWithInterpreters.setSelection(treeWithInterpreters.getItem(target));
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
    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getListControl(parent).setEnabled(enabled);
        addMenuItem.setEnabled(enabled);
        autoConfigMenuItem.setEnabled(enabled);
        if (pipenvConfigMenuItem != null) {
            pipenvConfigMenuItem.setEnabled(enabled);
        }
        removeButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
    }
}
