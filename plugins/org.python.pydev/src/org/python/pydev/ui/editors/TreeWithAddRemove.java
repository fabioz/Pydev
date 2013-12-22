/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 30, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.editors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.dialogs.MapOfStringsInputDialog;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * @author Fabio Zadrozny
 */
@SuppressWarnings("unchecked")
public abstract class TreeWithAddRemove extends Composite {

    /**
     * The actual tree.
     */
    protected Tree tree;

    /**
     * Used if the initial items map to an array of strings.
     */
    public final static int EDITING_STYLE_ARRAY_OF_STRINGS = 0;

    /**
     * Used if the initial items map to a map with strings.
     */
    public final static int EDITING_STYLE_MAP_OF_STRINGS = 1;

    /**
     * Signals what's the editing mode we're using.
     */
    private int editingStyle;

    private TreeColumn[] columns;

    public TreeWithAddRemove(Composite parent, int style, Object initialItems) {
        this(parent, style, initialItems, false);
    }

    /**
     * @param initialItems: Can be a String[] or a HashMap<String, String> (if null it's considered String[])
     */
    public TreeWithAddRemove(Composite parent, int style, Object initialItems, boolean createEditButton) {
        super(parent, style);
        if (initialItems == null) {
            initialItems = new String[] {};
        }

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        this.setLayout(layout);

        GridData data = new GridData(GridData.FILL_BOTH);

        tree = new Tree(this, SWT.BORDER | SWT.MULTI);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        tree.setLayoutData(data);

        Composite buttonsSourceFolders = new Composite(this, SWT.NONE);
        buttonsSourceFolders.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonsSourceFolders.setLayout(layout);

        for (int i = 0; i < getNumberOfAddButtons(); i++) {
            createAddButton(buttonsSourceFolders, i);
        }

        createRemoveButton(buttonsSourceFolders);
        if (createEditButton) {
            this.createEditButton(buttonsSourceFolders);
        }

        if (initialItems instanceof String[]) {
            editingStyle = EDITING_STYLE_ARRAY_OF_STRINGS;

        } else if (initialItems instanceof Map) {
            editingStyle = EDITING_STYLE_MAP_OF_STRINGS;
            tree.setHeaderVisible(true);
            TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
            column1.setText("Key");
            column1.setWidth(200);
            TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
            column2.setText("Value");
            column2.setWidth(200);
            columns = new TreeColumn[] { column1, column2 };

        } else {
            throw new RuntimeException("Unexpected initial items: " + initialItems);
        }

        setTreeItems(initialItems);
    }

    public void fitToContents() {
        if (columns != null) {
            for (TreeColumn c : columns) {
                c.pack();
            }
        }

    }

    public void setTreeItems(Object items) {
        this.tree.removeAll();

        switch (editingStyle) {
            case EDITING_STYLE_ARRAY_OF_STRINGS:
                String[] its = (String[]) items;
                for (int i = 0; i < its.length; i++) {
                    addTreeItem(its[i]);
                }
                break;

            case EDITING_STYLE_MAP_OF_STRINGS:
                Map<String, String> map = (Map<String, String>) items;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    addTreeItem(entry.getKey(), entry.getValue());
                }
                break;
        }
    }

    private void configButtonLayout(Button button) {
        GridData data;
        data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        button.setLayoutData(data);
    }

    protected void createAddButton(Composite buttonsSourceFolders, int buttonNumber) {
        Button buttonAddSourceFolder = new Button(buttonsSourceFolders, SWT.PUSH);
        customizeAddSomethingButton(buttonAddSourceFolder, buttonNumber);
        buttonAddSourceFolder.setText(getButtonLabel(buttonNumber));
        configButtonLayout(buttonAddSourceFolder);
    }

    protected void createRemoveButton(Composite buttonsSourceFolders) {
        Button buttonRemSourceFolder = new Button(buttonsSourceFolders, SWT.PUSH);
        customizeRemSourceFolderButton(buttonRemSourceFolder);
        configButtonLayout(buttonRemSourceFolder);
    }

    protected void createEditButton(Composite buttonsSourceFolders) {
        Button buttonEdit = new Button(buttonsSourceFolders, SWT.PUSH);
        customizeEditButton(buttonEdit);
        configButtonLayout(buttonEdit);
    }

    /**
     * Remove is almost always default
     */
    protected void customizeRemSourceFolderButton(Button buttonRem) {
        buttonRem.setText(getButtonRemoveText());
        buttonRem.setToolTipText("Remove the selected item");
        buttonRem.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                handleRemove();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }

    /**
     * Edit
     */
    protected void customizeEditButton(Button buttonEdit) {
        buttonEdit.setText(getButtonEditText());
        buttonEdit.setToolTipText("Edit the selected item");
        buttonEdit.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                handleEdit();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }

    private static String lastDirectoryDialogPath = null;
    private static String lastFileDialogPath = null;

    /**
     * @param addButton the button to be customized
     * @param nButton the number of the add button (1 for first, 2 for second)
     */
    protected void customizeAddSomethingButton(Button addButton, final int nButton) {
        addButton.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                handleAddButtonSelected(nButton);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }

    public void addItemWithDialog(MapOfStringsInputDialog dialog) {
        if (dialog.open() == Window.OK) {
            Tuple<String, String> keyAndValueEntered = dialog.getKeyAndValueEntered();
            if (keyAndValueEntered != null) {
                addTreeItem(keyAndValueEntered.o1, keyAndValueEntered.o2);
            }
        }
    }

    public void addItemWithDialog(InputDialog dialog) {
        dialog.open();
        String value = dialog.getValue();
        addTreeItem(value);
    }

    public void addItemWithDialog(FileDialog dialog) {
        dialog.setFilterPath(lastFileDialogPath);
        dialog.open();
        String[] fileNames = dialog.getFileNames();
        String parent = dialog.getFilterPath();
        if (fileNames != null && fileNames.length > 0) {
            for (String s : fileNames) {
                addTreeItem(FileUtils.getFileAbsolutePath(new File(parent, s)));
            }
        }
    }

    public void addItemWithDialog(DirectoryDialog dialog) {
        dialog.setFilterPath(lastDirectoryDialogPath);
        String filePath = dialog.open();
        if (filePath != null) {
            lastDirectoryDialogPath = filePath;
        }
        addTreeItem(filePath);
    }

    public void addItemWithDialog(SelectionDialog dialog, IProject project) {
        dialog.open();
        Object[] objects = dialog.getResult();
        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                Object object = objects[i];
                if (object instanceof IPath) {
                    IPath p = (IPath) object;
                    //IMPORTANT: get it relative to the workspace root, and not to the project!!
                    //(historical reasons)
                    String pathAsString = getPathAsString(p, project);
                    addTreeItem(pathAsString);
                } else if (object instanceof IFile) {
                    //IMPORTANT: get it relative to the workspace root, and not to the project!!
                    //(historical reasons)
                    IFile p = (IFile) object;
                    String pathAsString = getPathAsString(p.getProjectRelativePath(), project);
                    pathAsString = "/" + p.getProject().getName() + pathAsString;
                    if (FileTypesPreferencesPage.isValidZipFile(pathAsString)) {
                        addTreeItem(pathAsString);
                    }
                }
            }
        }
    }

    /**
     * @param project 
     * @return The passed path as a string (used for the selection dialog, as things come relative to the workspace).
     */
    private String getPathAsString(IPath p, IProject project) {
        String ret = p.toString();
        if (ret.startsWith("/") == false) {
            ret = "/" + ret;
        }
        return PyStructureConfigHelpers.convertToProjectRelativePath(project.getFullPath().toString(), ret);
    }

    /**
     * @param pathAsString
     */
    private void addTreeItem(String pathAsString) {
        if (pathAsString != null && pathAsString.trim().length() > 0) {

            // forbid duplicate selections
            TreeItem[] items = tree.getItems();
            for (int i = 0; i < items.length; i++) {
                if (items[i].getText().equals(pathAsString)) {
                    return;
                }
            }

            TreeItem item = new TreeItem(tree, 0);
            item.setText(pathAsString);
            item.setImage(PydevPlugin.getImageCache().get(getImageConstant()));
        }
    }

    protected void addTreeItem(String key, String value) {
        if (key != null && key.trim().length() > 0 && value != null && value.trim().length() > 0) {
            TreeItem item = new TreeItem(tree, 0);
            item.setText(new String[] { key, value });
            item.setImage(PydevPlugin.getImageCache().get(getImageConstant()));
        }
    }

    public String getTreeItemsAsStr() {
        if (editingStyle != EDITING_STYLE_ARRAY_OF_STRINGS) {
            throw new AssertionError("Can only call it if we're dealing with array of strings.");
        }
        FastStringBuffer ret = new FastStringBuffer();
        TreeItem[] items = tree.getItems();
        for (int i = 0; i < items.length; i++) {
            String text = items[i].getText();

            if (text != null && text.trim().length() > 0) {
                if (ret.length() > 0) {
                    ret.append("|");
                }
                ret.append(text);
            }
        }
        return ret.toString();
    }

    public Map<String, String> getTreeItemsAsMap() {
        if (editingStyle != EDITING_STYLE_MAP_OF_STRINGS) {
            throw new AssertionError("Can only call it if we're dealing with map of strings.");
        }
        HashMap<String, String> ret = new HashMap<String, String>();
        TreeItem[] items = tree.getItems();
        for (int i = 0; i < items.length; i++) {
            String text0 = items[i].getText(0);
            String text1 = items[i].getText(1);

            if (text0 != null && text0.trim().length() > 0 && text1 != null && text1.trim().length() > 0) {
                ret.put(text0.trim(), text1.trim());
            }
        }
        return ret;
    }

    //Things to customize (remove has a default implementation because it's usually the same)

    protected String getButtonRemoveText() {
        return "Remove";
    }

    protected String getButtonEditText() {
        return "Edit";
    }

    protected void handleRemove() {
        TreeItem[] selection = tree.getSelection();
        for (int i = 0; i < selection.length; i++) {
            selection[i].dispose();
        }
    }

    protected abstract String getButtonLabel(int i);

    protected abstract String getImageConstant();

    protected abstract void handleAddButtonSelected(int nButton);

    /**
     * If a subclass specified that edit should be treated, it should also implement the handleEdit.
     */
    protected void handleEdit() {
        throw new RuntimeException("Not implemented");
    }

    protected int getNumberOfAddButtons() {
        return 3;
    }

}
