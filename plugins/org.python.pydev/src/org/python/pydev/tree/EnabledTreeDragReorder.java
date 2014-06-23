/**
 * Copyright (c) 2014 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.ICallback;

public class EnabledTreeDragReorder {

    public static final String DRAG_IMAGE_DATA_KEY = "DRAG_IMAGE";

    public static class DragData {

        public final String text;
        public final String image;

        public DragData(String text, String image) {
            this.text = text;
            this.image = image;
        }

        public void update(TreeItem item) {
            item.setText(text);
            if (image != null) {
                item.setImage(PydevPlugin.getImageCache().get(image));
                item.setData(DRAG_IMAGE_DATA_KEY, image);
            }
        }

    }

    /**
     * Based on SWT Snippet91.
     */
    public static void enableDrag(final Tree tree, final boolean acceptDropInTree,
            final ICallback<Object, Object> onDNDFinished) {

        Transfer[] types = new Transfer[] { TreeItemDragDataTransfer.getInstance() };
        int operations = DND.DROP_MOVE;// | DND.DROP_COPY | DND.DROP_LINK; //Note: disable copy and link.

        final DragSource source = new DragSource(tree, operations);
        source.setTransfer(types);
        final TreeItem[] dragSourceItem = new TreeItem[1];
        source.addDragListener(new DragSourceListener() {
            public void dragStart(DragSourceEvent event) {
                TreeItem[] selection = tree.getSelection();
                if (selection.length > 0 && selection[0].getItemCount() == 0) {
                    event.doit = true;
                    dragSourceItem[0] = selection[0];
                } else {
                    event.doit = false;
                }
            };

            public void dragSetData(DragSourceEvent event) {
                TreeItem treeItem = dragSourceItem[0];
                if (treeItem != null) {
                    event.data = new DragData(treeItem.getText(), (String) treeItem.getData(DRAG_IMAGE_DATA_KEY));
                }
            }

            public void dragFinished(DragSourceEvent event) {
                if (event.detail == DND.DROP_MOVE && dragSourceItem[0] != null) {
                    dragSourceItem[0].dispose();
                    onDNDFinished.call(null);
                }
                dragSourceItem[0] = null;
            }
        });

        DropTarget target = new DropTarget(tree, operations);
        target.setTransfer(types);
        target.addDropListener(new DropTargetAdapter() {

            @Override
            public void dropAccept(DropTargetEvent event) {
                TreeItem item = (TreeItem) event.item;
                if (item != null) {
                    TreeItem parent = item.getParentItem();
                    if (!acceptDropInTree) {
                        if (parent == null) {
                            if (dragSourceItem[0] != null) {
                                dragSourceItem[0] = null; //Don't dispose of it!
                            }
                        }
                    }
                }
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
                event.detail = DND.DROP_NONE; //This is what prevents a drop when it can't happen.
                if (event.item != null) {
                    TreeItem item = (TreeItem) event.item;

                    if (item.getParentItem() == null && !acceptDropInTree) {
                        event.feedback = DND.FEEDBACK_NONE;
                        return;
                    }
                    event.detail = DND.DROP_MOVE; //Enable the drop to happen as we have a valid item.

                    Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
                    Rectangle bounds = item.getBounds();
                    if (pt.y < bounds.y + bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
                    } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_AFTER;
                    } else {
                        event.feedback |= DND.FEEDBACK_SELECT;
                    }
                }
            }

            @Override
            public void drop(DropTargetEvent event) {
                System.out.println("dropDone");
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                DragData data = (DragData) event.data;
                if (event.item == null) {
                    TreeItem item = new TreeItem(tree, SWT.NONE);
                    data.update(item);
                } else {
                    TreeItem item = (TreeItem) event.item;
                    TreeItem parent = item.getParentItem();

                    Point pt = tree.getDisplay().map(null, tree, event.x, event.y);
                    Rectangle bounds = item.getBounds();
                    if (parent != null) {
                        TreeItem[] items = parent.getItems();
                        int index = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }
                        if (pt.y < bounds.y + bounds.height / 3) {
                            TreeItem newItem = new TreeItem(parent, SWT.NONE,
                                    index);
                            data.update(newItem);
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            TreeItem newItem = new TreeItem(parent, SWT.NONE,
                                    index + 1);
                            data.update(newItem);
                        } else {
                            TreeItem newItem = new TreeItem(item, SWT.NONE);
                            data.update(newItem);
                        }

                    } else {
                        if (!acceptDropInTree) {
                            return;
                        }
                        TreeItem[] items = tree.getItems();
                        int index = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == item) {
                                index = i;
                                break;
                            }
                        }
                        if (pt.y < bounds.y + bounds.height / 3) {
                            TreeItem newItem = new TreeItem(tree, SWT.NONE,
                                    index);
                            data.update(newItem);
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            TreeItem newItem = new TreeItem(tree, SWT.NONE,
                                    index + 1);
                            data.update(newItem);
                        } else {
                            TreeItem newItem = new TreeItem(item, SWT.NONE);
                            data.update(newItem);
                        }
                    }

                }
            }
        });
    }
}
