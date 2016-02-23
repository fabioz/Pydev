/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.dialogs.MapOfStringsInputDialog;
import org.python.pydev.ui.editors.TreeWithAddRemove;

/**
 * This class creates a tab to show the variables in the passed tab folder.
 */
public class TabVariables {

    private TreeWithAddRemove treeVariables;
    private TabFolder tabFolder;

    public TabVariables(TabFolder tabFolder, Map<String, String> initialVariables) {
        this.tabFolder = tabFolder;
        createTabVariables(initialVariables);
    }

    private void createTabVariables(Map<String, String> initialVariables) {
        if (initialVariables == null) {
            initialVariables = new HashMap<String, String>();
        }
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("String Substitution Variables");
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.VARIABLE_ICON));
        Composite topComp = new Composite(tabFolder, SWT.None);
        topComp.setLayout(new GridLayout(1, false));

        GridData gd;
        GridData data;
        Label l2;
        l2 = new Label(topComp, SWT.None);
        l2.setText("String substitution variables are used to resolve:\n" + "  - source folders\n"
                + "  - external libraries\n" + "  - main module in launch configuration");

        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        final Map<String, String> vars = initialVariables;

        treeVariables = new TreeWithAddRemove(topComp, 0, vars, true) {

            @Override
            protected String getImageConstant() {
                return UIConstants.VARIABLE_ICON;
            }

            @Override
            protected void handleAddButtonSelected(int nButton) {
                if (nButton == 0) {
                    addItemWithDialog(new MapOfStringsInputDialog(getShell(), "Variable",
                            "Enter the variable name/value.", vars) {

                        @Override
                        protected boolean isExistingKeyEdit() {
                            return false;
                        }
                    });

                } else {
                    throw new AssertionError("Unexpected (only 0 should be available)");
                }
            }

            @Override
            protected void handleEdit() {
                TreeItem[] selection = this.tree.getSelection();
                if (selection.length != 1) {
                    return;
                }
                TreeItem treeItem = selection[0];
                if (treeItem == null) {
                    return;
                }

                final String fixedKeyText = treeItem.getText(0);

                //Overridden because we want the key to be fixed.
                MapOfStringsInputDialog dialog = new MapOfStringsInputDialog(getShell(), "Variable",
                        "Enter the variable name/value.", vars) {

                    @Override
                    protected org.eclipse.swt.widgets.Control createDialogArea(Composite parent) {
                        Control control = super.createDialogArea(parent);
                        this.keyField.setText(fixedKeyText);
                        this.keyField.setEditable(false);
                        this.valueField.setFocus();
                        String value = vars.get(fixedKeyText);
                        if (value == null) {
                            value = "";
                        }
                        this.valueField.setText(value);
                        return control;
                    }

                    @Override
                    protected boolean isExistingKeyEdit() {
                        return true;
                    };

                    @Override
                    protected String getInitialMessage() {
                        return null; //it starts in a valid state
                    };

                };

                if (dialog.open() == Window.OK) {
                    Tuple<String, String> keyAndValueEntered = dialog.getKeyAndValueEntered();
                    if (keyAndValueEntered != null) {
                        vars.put(keyAndValueEntered.o1, keyAndValueEntered.o2);
                        treeItem.setText(1, keyAndValueEntered.o2);
                    }
                }

            };

            @Override
            protected String getButtonLabel(int i) {
                if (i != 0) {
                    throw new RuntimeException("Expected only i==0. Received: " + i);
                }
                return "Add variable";
            }

            @Override
            protected int getNumberOfAddButtons() {
                return 1;
            }
        };

        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeVariables.setLayoutData(data);

        tabItem.setControl(topComp);
    }

    public void setTreeItemsFromMap(Map<String, String> treeVariables) {
        this.treeVariables.setTreeItems(treeVariables);
    }

    public Map<String, String> getTreeItemsAsMap() {
        return this.treeVariables.getTreeItemsAsMap();
    }

}
