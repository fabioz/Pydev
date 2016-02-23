/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

public class PyIndexView extends ViewPart {

    private Tree tree;
    private TreeViewer treeViewer;

    @Override
    public void createPartControl(Composite parent) {
        tree = new Tree(parent, 0);
        treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(new PyIndexContentProvider());
        treeViewer.setInput(new IndexRoot());

        tree.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.keyCode == SWT.F5) {
                    treeViewer.setInput(new IndexRoot());
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
    }

    @Override
    public void setFocus() {
        this.tree.setFocus();
    }

}
