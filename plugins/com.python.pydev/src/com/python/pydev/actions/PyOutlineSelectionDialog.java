/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.actions;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.uiutils.DialogMemento;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;

/**
 * @author fabioz
 *
 */
final class PyOutlineSelectionDialog extends TreeSelectionDialog {

    private final DialogMemento memento;
    private boolean showParentHierarchy;
    private Label labelCtrlO;

    PyOutlineSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, DialogMemento memento) {
        super(parent, labelProvider, contentProvider);
        this.memento = memento;

        setMessage("Filter (press enter to go to selected element)");
        setTitle("PyDev: Quick Outline");
        setAllowMultiple(false);
        this.showParentHierarchy = false;
    }

    public boolean close() {
        memento.writeSettings(getShell());
        return super.close();
    }

    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }

    protected Point getInitialLocation(Point initialSize) {
        return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

    public Control createDialogArea(Composite parent) {
        memento.readSettings();
        Control ret = super.createDialogArea(parent);
        this.text.addKeyListener(new KeyListener() {

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if ((e.keyCode == 'o' || e.keyCode == 'O') && e.stateMask == SWT.CTRL) {
                    toggleShowParentHierarchy();
                }
            }
        });
        return ret;
    }

    protected void updateShowParentHierarchyMessage() {
        if (showParentHierarchy) {
            labelCtrlO.setText("Press Ctrl+O to hide parent hierarchy.");
        } else {
            labelCtrlO.setText("Press Ctrl+O to show parent hierarchy.");
        }
    }
    

    
    @Override
    protected int getDefaultMargins(){
        return 0;
    }
    
    @Override
    protected int getDefaultSpacing(){
        return 0;
    }

    
    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        labelCtrlO = new Label(parent, SWT.NONE);
        updateShowParentHierarchyMessage();
        return labelCtrlO;
    }

    protected void toggleShowParentHierarchy() {
        showParentHierarchy = !showParentHierarchy;
        updateShowParentHierarchyMessage();
    }
}

