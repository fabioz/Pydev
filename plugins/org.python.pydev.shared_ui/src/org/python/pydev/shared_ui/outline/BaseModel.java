/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.outline;

import java.util.ArrayList;

import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.model.ISimpleNode;

public abstract class BaseModel implements IOutlineModel {

    protected final IBaseEditor editor;

    protected final BaseOutlinePage outline;

    protected final IModelListener modelListener;

    protected IParsedItem root = null; // A list of top nodes in this document. Used as a tree root

    protected abstract IParsedItem createParsedItemFromSimpleNode(ISimpleNode ast);

    public BaseModel(BaseOutlinePage outline, IBaseEditor editor) {
        this.editor = editor;
        this.outline = outline;

        // The notifications are only propagated to the outline page
        //
        // Tell parser that we want to know about all the changes
        // make sure that the changes are propagated on the main thread
        modelListener = new IModelListener() {

            public void modelChanged(final ISimpleNode ast) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        synchronized (this) {
                            IParsedItem newRoot = createParsedItemFromSimpleNode(ast);
                            setRoot(newRoot);
                        }
                    }

                });
            }

            public void errorChanged(final ErrorDescription errorDesc) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        synchronized (this) {
                            IParsedItem newRoot = duplicateRootAddingError(errorDesc);
                            setRoot(newRoot);
                        }
                    }

                });
            }

        };

        root = this.createInitialRootFromEditor();

        editor.addModelListener(modelListener);
    }

    protected abstract IParsedItem createInitialRootFromEditor();

    protected abstract IParsedItem duplicateRootAddingError(ErrorDescription errorDesc);

    public void dispose() {
        editor.removeModelListener(modelListener);
    }

    public IParsedItem getRoot() {
        return root;
    }

    // patchRootHelper makes oldItem just like the newItem
    //   the differnce between the two is 
    private void patchRootHelper(IParsedItem oldItem, IParsedItem newItem, ArrayList<IParsedItem> itemsToRefresh,
            ArrayList<IParsedItem> itemsToUpdate) {

        IParsedItem[] newChildren = newItem.getChildren();
        IParsedItem[] oldChildren = oldItem.getChildren();

        // stuctural change, different number of children, can stop recursion
        if (newChildren.length != oldChildren.length) {

            //at this point, it'll recalculate the children...
            oldItem.updateTo(newItem);
            itemsToRefresh.add(oldItem);

        } else {

            // Number of children is the same, fix up all the children
            for (int i = 0; i < oldChildren.length; i++) {
                patchRootHelper(oldChildren[i], newChildren[i], itemsToRefresh, itemsToUpdate);
            }

            // see if the node needs redisplay
            String oldTitle = oldItem.toString();
            String newTitle = newItem.toString();
            if (!oldTitle.equals(newTitle) || !oldItem.sameNodeType(newItem)) {
                itemsToUpdate.add(oldItem);
            }

            oldItem.updateShallow(newItem);

        }
    }

    /**
     * Replaces current root
     */
    public void setRoot(IParsedItem newRoot) {
        // We'll try to do the 'least flicker replace'
        // compare the two root structures, and tell outline what to refresh
        try {
            if (root != null) {
                ArrayList<IParsedItem> itemsToRefresh = new ArrayList<IParsedItem>();
                ArrayList<IParsedItem> itemsToUpdate = new ArrayList<IParsedItem>();
                patchRootHelper(root, newRoot, itemsToRefresh, itemsToUpdate);
                if (outline != null) {
                    if (outline.isDisposed()) {
                        return;
                    }

                    //to update
                    int itemsToUpdateSize = itemsToUpdate.size();
                    if (itemsToUpdateSize > 0) {
                        outline.updateItems(itemsToUpdate.toArray(new IParsedItem[itemsToUpdateSize]));
                    }

                    //to refresh
                    int itemsToRefreshSize = itemsToRefresh.size();
                    if (itemsToRefreshSize > 0) {
                        outline.refreshItems(itemsToRefresh.toArray(new IParsedItem[itemsToRefreshSize]));
                    }
                }

            } else {
                Log.log("No old model root?");
            }
        } catch (Throwable e) {
            Log.log(e);
        }
    }

}
