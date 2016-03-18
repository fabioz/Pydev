/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tree;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author fabioz
 *
 */
public class PyFilteredTree extends FilteredTree {

    private PyFilteredTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
        super(parent, treeStyle, filter, useNewLook);
    }

    private PyFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
        super(parent, treeStyle, filter);
    }

    public static PyFilteredTree create(Composite parent, PatternFilter filter, boolean border) {
        int treeStyle = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;
        if (border) {
            treeStyle |= SWT.BORDER;
        }
        return create(parent, treeStyle, filter);
    }

    public static PyFilteredTree create(Composite parent, int treeStyle, PatternFilter filter) {
        PyFilteredTree ret;

        try {
            ret = new PyFilteredTree(parent, treeStyle, filter, true);
        } catch (Throwable e) {
            ret = new PyFilteredTree(parent, treeStyle, filter);
        }

        final TreeViewer viewer = ret.getViewer();
        ret.getFilterControl().addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                //The superclass will already do the focus, but we also treat it to select some element
                //when down is pressed.
                boolean hasItems = viewer.getTree().getItemCount() > 0;
                if (hasItems && e.keyCode == SWT.ARROW_DOWN) {
                    Tree tree = viewer.getTree();
                    updateSelectionIfNothingSelected(tree);
                    return;
                }
            }

            protected void updateSelectionIfNothingSelected(Tree tree) {
                TreeItem[] sel = tree.getSelection();
                if (sel == null || sel.length == 0) {
                    TreeItem[] items = tree.getItems();
                    if (items != null && items.length > 0) {
                        tree.setSelection(items[0]);
                    }
                }
            }

        });
        return ret;
    }

}
