/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.hierarchy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.quick_outline.DataAndImageTreeNodeContentProvider;
import org.python.pydev.shared_ui.tree.LabelProviderWithDecoration;
import org.python.pydev.ui.ViewPartWithOrientation;

import com.python.pydev.actions.ShowOutlineLabelProvider;

/**
 * @author fabioz
 */
public class HierarchyViewer {

    private final Object lock = new Object();

    /*default*/Tree treeMembers;
    /*default*/TreeViewer treeClassesViewer;

    private SashForm sash;

    private Composite fParent;

    public void setFocus() {
        if (this.treeClassesViewer != null) {
            this.treeClassesViewer.getTree().setFocus();
        }
    }

    public void dispose() {
        if (this.treeClassesViewer != null) {
            this.treeClassesViewer.getTree().dispose();
            this.treeClassesViewer = null;
        }

        if (this.treeMembers != null) {
            this.treeMembers.dispose();
            this.treeMembers = null;
        }
    }

    /**
     * Handle the creation for earlier versions of Eclipse.
     */
    protected static ILabelProvider createLabelProvider() {
        try {
            return new LabelProviderWithDecoration(new HierarchyLabelProvider(), PlatformUI.getWorkbench()
                    .getDecoratorManager().getLabelDecorator(), null);
        } catch (Throwable e) {
            return new ShowOutlineLabelProvider();
        }
    }

    public void createPartControl(Composite parent) {
        this.fParent = parent;
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        sash = new SashForm(parent, SWT.VERTICAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        sash.setLayoutData(layoutData);

        parent = sash;
        treeClassesViewer = new TreeViewer(parent);
        treeClassesViewer.setContentProvider(new DataAndImageTreeNodeContentProvider());
        treeClassesViewer.setLabelProvider(createLabelProvider());

        treeClassesViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                handleSelection(selection, 2);
            }
        });

        treeClassesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                handleSelection(selection, 1);
            }
        });

        treeMembers = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

        treeMembers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TreeItem[] selection = treeMembers.getSelection();
                if (selection.length > 0) {
                    Object data = selection[0].getData();
                    ItemPointer p = (ItemPointer) data;
                    if (p != null) {
                        new PyOpenAction().run(p);
                    }
                }
            }
        });

    }

    private static Image parentsImage;
    private static Image classImage;

    public void setHierarchy(HierarchyNodeModel model) {
        if (classImage == null) {
            classImage = PydevPlugin.getImageCache().get(UIConstants.CLASS_ICON);
        }

        DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
        DataAndImageTreeNode item = new DataAndImageTreeNode(root, model, classImage);

        DataAndImageTreeNode base = item;
        recursivelyAdd(model, base, true, new HashSet<HierarchyNodeModel>());

        if (parentsImage == null) {
            ImageDescriptor imageDescriptor = com.python.pydev.PydevPlugin.getImageDescriptor("icons/class_hi.gif");
            if (imageDescriptor != null) {
                parentsImage = imageDescriptor.createImage();
            }
        }

        DataAndImageTreeNode parents = new DataAndImageTreeNode(root, "Parents", parentsImage);
        recursivelyAdd(model, parents, false, new HashSet<HierarchyNodeModel>());

        treeClassesViewer.setInput(root);

        onClick(model, 1);
    }

    private void recursivelyAdd(HierarchyNodeModel model, DataAndImageTreeNode base, boolean addChildren,
            HashSet<HierarchyNodeModel> memo) {
        List<HierarchyNodeModel> items = addChildren ? model.children : model.parents;
        if (items != null) {
            for (HierarchyNodeModel modelNode : items) {
                if (memo.contains(modelNode)) {
                    new DataAndImageTreeNode(base, modelNode.name + " already added.", classImage);
                    continue;
                }
                memo.add(modelNode);
                DataAndImageTreeNode item = new DataAndImageTreeNode(base, modelNode, classImage);
                recursivelyAdd(modelNode, item, addChildren, memo);
            }
        }
    }

    private void onClick(final HierarchyNodeModel model, int clickCount) {
        if (clickCount == 2) {
            if (model != null) {
                IModule m = model.module;
                if (m != null && model.ast != null) {
                    ItemPointer pointer = new ItemPointer(m.getFile(), model.ast.name);
                    new PyOpenAction().run(pointer);
                }
            }
        } else {

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    synchronized (lock) {
                        if (treeMembers.getItemCount() > 0) {
                            treeMembers.removeAll();
                        }
                        if (model == null) {
                            return;
                        }
                        ClassDef ast = model.ast;
                        if (ast != null && treeMembers != null) {
                            DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create(ast);
                            Iterator<ASTEntry> outline = visitor.getOutline();

                            HashMap<SimpleNode, TreeItem> c = new HashMap<SimpleNode, TreeItem>();

                            boolean first = true;
                            while (outline.hasNext()) {
                                ASTEntry entry = outline.next();

                                if (first) {
                                    //Don't add the class itself.
                                    first = false;
                                    continue;
                                }

                                TreeItem item = null;
                                if (entry.node instanceof FunctionDef) {
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.METHOD_ICON));
                                    if (model.module != null) {
                                        item.setData(new ItemPointer(model.module.getFile(),
                                                ((FunctionDef) entry.node).name));
                                    }

                                } else if (entry.node instanceof ClassDef) {
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.CLASS_ICON));
                                    if (model.module != null) {
                                        item.setData(new ItemPointer(model.module.getFile(),
                                                ((ClassDef) entry.node).name));
                                    }

                                } else {
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.PUBLIC_ATTR_ICON));
                                    if (model.module != null) {
                                        item.setData(new ItemPointer(model.module.getFile(), entry.node));
                                    }
                                }
                                item.setText(entry.getName());
                                item.setExpanded(true);
                            }
                        }
                    }
                }

                private TreeItem createTreeItem(HashMap<SimpleNode, TreeItem> c, ASTEntry entry) {
                    TreeItem parent = null;

                    ASTEntry par = entry.parent;
                    if (par != null) {
                        parent = c.get(par.node);
                    }

                    TreeItem item;
                    if (parent == null) {
                        item = new TreeItem(treeMembers, 0);
                    } else {
                        item = new TreeItem(parent, 0);
                    }
                    c.put(entry.node, item);
                    return item;
                }
            };

            Display.getDefault().asyncExec(r);
        }
    }

    private void handleSelection(ISelection selection, int clickCount) {
        HierarchyNodeModel model = null;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection iStructuredSelection = (IStructuredSelection) selection;
            Object firstElement = iStructuredSelection.getFirstElement();
            if (firstElement instanceof DataAndImageTreeNode) {
                DataAndImageTreeNode treeNode = (DataAndImageTreeNode) firstElement;
                Object data = treeNode.data;
                if (data instanceof HierarchyNodeModel) {
                    model = (HierarchyNodeModel) data;
                }
            } else if (firstElement instanceof HierarchyNodeModel) {
                model = (HierarchyNodeModel) firstElement;
            }
        }
        onClick(model, clickCount);
    }

    public void setNewOrientation(int orientation) {
        if (sash != null && !sash.isDisposed() && fParent != null && !fParent.isDisposed()) {
            GridLayout layout = (GridLayout) fParent.getLayout();
            if (orientation == ViewPartWithOrientation.VIEW_ORIENTATION_HORIZONTAL) {
                sash.setOrientation(SWT.HORIZONTAL);
                layout.numColumns = 2;

            } else {
                sash.setOrientation(SWT.VERTICAL);
                layout.numColumns = 1;
            }
            fParent.layout();
        }
    }

}
