/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 22, 2006
 */
package com.python.pydev.ui.hierarchy;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;

public class TreeMouseListener implements MouseListener {

    private Tree tree;

    public TreeMouseListener(Tree tree) {
        this.tree = tree;
    }

    public void mouseDoubleClick(MouseEvent e) {
        TreeItem[] selection = tree.getSelection();
        if(selection.length > 0){
            ItemPointer p = (ItemPointer) selection[0].getData();
            if(p != null){
                new PyOpenAction().run(p);
            }
        }
    }

    public void mouseDown(MouseEvent e) {
    }

    public void mouseUp(MouseEvent e) {
    }

}
