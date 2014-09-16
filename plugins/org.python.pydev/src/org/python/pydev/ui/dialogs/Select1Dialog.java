/******************************************************************************
* Copyright (C) 2014  Brainwy Software LTDA.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.DialogMemento;

public class Select1Dialog {

    public void afterCreateButtonBar(Composite parent, Composite buttonBar) {
        //Hook for subclasses to override
    }

    public TreeNode<Object> selectElement(TreeNode<Object> emptyRoot) {
        Shell shell = EditorUtils.getShell();
        final DialogMemento memento = new DialogMemento(shell, "org.python.pydev.ui.dialogs.Select1Dialog.shell");

        TreeSelectionDialog dialog = new TreeSelectionDialog(shell, getLabelProvider(), getContentProvider()) {

            @Override
            public boolean close() {
                memento.writeSettings(getShell());
                return super.close();
            }

            @Override
            public Control createDialogArea(Composite parent) {
                memento.readSettings();
                Control ret = super.createDialogArea(parent);
                ret.addTraverseListener(new TraverseListener() {

                    public void keyTraversed(TraverseEvent e) {
                        if (e.detail == SWT.TRAVERSE_RETURN) {
                            okPressed();
                        }
                    }
                });
                return ret;
            }

            /* (non-Javadoc)
             * @see org.python.pydev.ui.dialogs.TreeSelectionDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
             */
            @Override
            protected Control createButtonBar(Composite parent) {
                Composite buttonBar = new Composite(parent, 0);

                GridLayout layout = new GridLayout();
                layout.numColumns = 2;
                buttonBar.setLayout(layout);

                afterCreateButtonBar(parent, buttonBar);

                GridData data = new GridData();
                data.horizontalAlignment = SWT.FILL;
                data.grabExcessHorizontalSpace = true;
                buttonBar.setLayoutData(data);
                return buttonBar;
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
                List<Object> list = selection.toList();
                if (list.size() == 1) {
                    setResult(list);
                } else {
                    Tree tree = getTreeViewer().getTree();
                    TreeItem[] items = tree.getItems();
                    list = new ArrayList<Object>();
                    //Now, if he didn't select anything, let's create tests with all that is currently filtered
                    //in the interface 
                    createListWithLeafs(items, list);
                    if (list.size() == 1) {
                        setResult(list);
                    }
                }
            }

            private void createListWithLeafs(TreeItem[] items, List<Object> leafObjectsList) {
                for (TreeItem item : items) {
                    TreeItem[] children = item.getItems();
                    if (children.length == 0) {
                        leafObjectsList.add(item.getData());
                    } else {
                        createListWithLeafs(children, leafObjectsList);
                    }
                }
            }

        };

        dialog.setTitle("PyDev: Select entry");
        dialog.setMessage("Select entry");
        dialog.setInitialFilter(getInitialFilter());
        dialog.setAllowMultiple(false);
        dialog.setInput(emptyRoot);

        int open = dialog.open();
        if (open != Window.OK) {
            return null;
        }
        Object[] result = dialog.getResult();
        if (result != null && result.length == 1) {
            return (TreeNode<Object>) result[0];
        }
        return null;
    }

    protected ILabelProvider getLabelProvider() {
        return new TreeNodeLabelProvider();
    }

    protected ITreeContentProvider getContentProvider() {
        return new TreeNodeContentProvider();
    }

    protected String getInitialFilter() {
        return "";
    }
}
