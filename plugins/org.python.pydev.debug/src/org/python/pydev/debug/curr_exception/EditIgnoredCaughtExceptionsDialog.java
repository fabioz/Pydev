/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.curr_exception;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.dialogs.DialogMemento;
import org.python.pydev.ui.editors.TreeWithAddRemove;

/**
 * @author fabioz
 */
public class EditIgnoredCaughtExceptionsDialog extends TrayDialog {

    private Button okButton;
    private Button cancelButton;
    private HashMap<String, String> map;
    private TreeWithAddRemove treeWithAddRemove;
    private DialogMemento memento;
    private Map<String, String> finalMap;

    EditIgnoredCaughtExceptionsDialog(Shell shell, HashMap<String, String> map) {
        super(shell);
        this.map = map;
        setHelpAvailable(false);
        memento = new DialogMemento(shell, "org.python.pydev.debug.curr_exception.EditIgnoredCaughtExceptionsDialog");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    public boolean close() {
        memento.writeSettings(getShell());
        return super.close();
    }

    @Override
    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        memento.readSettings();
        Composite area = (Composite) super.createDialogArea(parent);
        treeWithAddRemove = new TreeWithAddRemove(area, 0, map) {

            @Override
            protected void handleAddButtonSelected(int nButton) {
                throw new RuntimeException("not implemented: no add buttons");
            }

            @Override
            protected String getImageConstant() {
                return UIConstants.PUBLIC_ATTR_ICON;
            }

            @Override
            protected String getButtonLabel(int i) {
                throw new RuntimeException("not implemented: no add buttons");
            }

            @Override
            protected int getNumberOfAddButtons() {
                return 0;
            }
        };

        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeWithAddRemove.setLayoutData(data);
        treeWithAddRemove.fitToContents();
        return area;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Edit Ignored Thrown Exceptions");
    }

    @Override
    protected void okPressed() {
        this.finalMap = treeWithAddRemove.getTreeItemsAsMap();
        super.okPressed();
    }

    public Map<String, String> getResult() {
        return finalMap;
    }

}
