/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.quick_outline.BaseQuickOutlineSelectionDialog;
import org.python.pydev.shared_ui.quick_outline.DataAndImageTreeNodeContentProvider;
import org.python.pydev.shared_ui.tree.LabelProviderWithDecoration;

import com.python.pydev.PydevPlugin;
import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * @author fabioz
 *
 */
public final class PyOutlineSelectionDialog extends BaseQuickOutlineSelectionDialog {

    /**
     * May be null (in which case the ast and nodeToModel should be used).
     */
    private PyEdit pyEdit;

    /**
     * May be null (in which case the pyedit should be used to calculate it).
     */
    private HashMap<SimpleNode, HierarchyNodeModel> nodeToModel;

    /**
     * May be null (in which case the pyedit should be used to calculate it).
     */
    private SimpleNode ast;

    protected final Job jobCalculateParents = new Job("Calculate parents") {

        @Override
        public IStatus run(IProgressMonitor monitor) {
            rootWithParents = root.createCopy(null);

            if (nodeToModel == null) {
                //Step 2: create mapping: classdef to hierarchy model.
                nodeToModel = new HashMap<SimpleNode, HierarchyNodeModel>();
                List<Tuple<ClassDef, DataAndImageTreeNode<Object>>> gathered = new ArrayList<Tuple<ClassDef, DataAndImageTreeNode<Object>>>();
                gatherClasses(rootWithParents, monitor, gathered);
                monitor.beginTask("Calculate parents", gathered.size() + 1);

                IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                IPyRefactoring2 r2 = (IPyRefactoring2) pyRefactoring;

                for (Tuple<ClassDef, DataAndImageTreeNode<Object>> t : gathered) {
                    SubProgressMonitor subProgressMonitor = new SubProgressMonitor(monitor, 1);
                    try {
                        ClassDef classDef = t.o1;
                        PySelection ps = new PySelection(pyEdit.getDocument(), classDef.name.beginLine - 1,
                                classDef.name.beginColumn - 1);
                        try {
                            RefactoringRequest refactoringRequest = PyRefactorAction.createRefactoringRequest(
                                    subProgressMonitor, pyEdit, ps);
                            HierarchyNodeModel model = r2.findClassHierarchy(refactoringRequest, true);
                            nodeToModel.put(((OutlineEntry) t.o2.data).node, model);
                        } catch (MisconfigurationException e) {
                            Log.log(e);
                        }
                    } finally {
                        subProgressMonitor.done();
                    }
                }
            }

            if (!monitor.isCanceled()) {
                fillHierarchy(rootWithParents);
            }

            if (!monitor.isCanceled()) {
                uiJobSetRootWithParentsInput.setPriority(Job.INTERACTIVE);
                uiJobSetRootWithParentsInput.schedule();
            } else {
                //Will be recalculated if asked again!
                rootWithParents = null;
            }
            monitor.done();

            return Status.OK_STATUS;
        }
    };

    /**
     * Handle the creation for earlier versions of Eclipse.
     */
    private static ILabelProvider createLabelProvider() {
        try {
            return new LabelProviderWithDecoration(new ShowOutlineLabelProvider(), PlatformUI.getWorkbench()
                    .getDecoratorManager().getLabelDecorator(), null);
        } catch (Throwable e) {
            return new ShowOutlineLabelProvider();
        }
    }

    /**
     * Constructor to be used if the pyedit is not available (info must be pre-calculated)
     */
    public PyOutlineSelectionDialog(Shell shell, SimpleNode ast, HashMap<SimpleNode, HierarchyNodeModel> nodeToModel) {
        super(shell, PydevPlugin.PLUGIN_ID, createLabelProvider(), new DataAndImageTreeNodeContentProvider(), true);
        this.ast = ast;
        this.nodeToModel = nodeToModel;
        calculateHierarchy();
        setInput(root);
    }

    /**
     * Constructor to be used if the pyedit is available (in which case the info will be calculated on demand)
     */
    public PyOutlineSelectionDialog(Shell shell, PyEdit pyEdit) {
        super(shell, PydevPlugin.PLUGIN_ID, createLabelProvider(), new DataAndImageTreeNodeContentProvider(), true);
        this.pyEdit = pyEdit;
        PySelection ps = this.pyEdit.createPySelection();
        startLineIndex = ps.getStartLineIndex() + 1; //+1 because the ast starts at 1
        calculateHierarchy();
        setInput(root);

        //After creating the tree viewer (and setting the input), let's set the initial selection!
        if (initialSelection != null) {
            this.setInitialSelections(new Object[] { initialSelection });
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ret = super.createContents(parent);
        org.python.pydev.plugin.PydevPlugin.setCssId(parent, "py-outline-selection-dialog", true);
        return ret;
    }

    @Override
    protected void calculateHierarchy() {
        if (root != null) {
            return;
        }

        if (this.ast == null && pyEdit != null) {
            this.ast = pyEdit.getAST();
        }

        if (ast == null) {
            return;
        }

        DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create(ast);
        if (visitor == null) {
            return;
        }

        Map<ASTEntry, DataAndImageTreeNode<Object>> entryToTreeNode = new HashMap<ASTEntry, DataAndImageTreeNode<Object>>();

        //Step 1: create 'regular' tree structure from the nodes.
        DataAndImageTreeNode<Object> root = new DataAndImageTreeNode<Object>(null, null, null);

        for (Iterator<ASTEntry> it = visitor.getOutline(); it.hasNext();) {
            ASTEntry next = it.next();
            DataAndImageTreeNode<Object> n;
            if (next.parent != null) {
                DataAndImageTreeNode<Object> parent = entryToTreeNode.get(next.parent);
                if (parent == null) {
                    Log.log("Unexpected condition: child found before parent!");
                    parent = root;
                }
                n = new DataAndImageTreeNode<Object>(parent, new OutlineEntry(next), null);

            } else {
                n = new DataAndImageTreeNode<Object>(root, new OutlineEntry(next), null);
            }

            if (((OutlineEntry) n.data).node.beginLine <= startLineIndex) {
                initialSelection = n;
            }

            entryToTreeNode.put(next, n);
        }
        this.root = root;
    }

    @Override
    protected void calculateHierarchyWithParents() {
        if (rootWithParents != null) {
            uiJobSetRootWithParentsInput.setPriority(Job.INTERACTIVE);
            uiJobSetRootWithParentsInput.schedule();
            return;
        }

        calculateHierarchy(); //make sure the root is OK

        if (root == null) {
            return;
        }

        jobCalculateParents.setPriority(Job.INTERACTIVE);
        jobCalculateParents.schedule();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void fillHierarchy(DataAndImageTreeNode<Object> entry) {
        ArrayList<DataAndImageTreeNode<Object>> copy = new ArrayList(entry.getChildren());
        for (DataAndImageTreeNode<Object> nextEntry : copy) {
            HierarchyNodeModel model = this.nodeToModel.get(((OutlineEntry) nextEntry.data).node);
            addMethods(nextEntry, model);
            fillHierarchy(nextEntry);
        }
    }

    private void addMethods(DataAndImageTreeNode<Object> nextEntry, HierarchyNodeModel model) {
        if (model == null || model.parents == null) {
            return;
        }
        for (HierarchyNodeModel parent : model.parents) {
            DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.createForChildren(parent.ast);
            if (visitor == null) {
                continue;
            }

            Iterator<ASTEntry> outline = visitor.getOutline();
            while (outline.hasNext()) {
                ASTEntry entry = outline.next();
                if (entry.parent == null) {
                    //only direct children...
                    new DataAndImageTreeNode<Object>(nextEntry, new OutlineEntry(entry, parent), null);
                }
            }
            addMethods(nextEntry, parent);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void gatherClasses(DataAndImageTreeNode<Object> entry, IProgressMonitor monitor,
            List<Tuple<ClassDef, DataAndImageTreeNode<Object>>> gathered) {
        List children = entry.getChildren();
        if (children.size() == 0) {
            return;
        }
        //Iterate in a copy, since we may change the original...
        for (Object o : children) {
            DataAndImageTreeNode<Object> nextEntry = (DataAndImageTreeNode<Object>) o;
            if (((OutlineEntry) nextEntry.data).node instanceof ClassDef) {
                ClassDef classDef = (ClassDef) ((OutlineEntry) nextEntry.data).node;
                gathered.add(new Tuple<ClassDef, DataAndImageTreeNode<Object>>(classDef, nextEntry));
            }

            //Enter the leaf to fill it too.
            gatherClasses(nextEntry, monitor, gathered);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#open()
     */
    @Override
    public int open() {
        int ret = super.open();
        if (ret == OK) {
            Object[] result = getResult();
            if (result != null && result.length > 0) {
                @SuppressWarnings("unchecked")
                DataAndImageTreeNode<Object> n = (DataAndImageTreeNode<Object>) result[0];
                OutlineEntry outlineEntry = (OutlineEntry) n.data;
                if (outlineEntry.model == null) {
                    Location location = new Location(NodeUtils.getNameLineDefinition(outlineEntry.node) - 1,
                            NodeUtils.getNameColDefinition(outlineEntry.node) - 1);
                    EditorUtils.showInEditor(pyEdit, location, location);
                } else {
                    PyOpenAction pyOpenAction = new PyOpenAction();
                    IModule m = outlineEntry.model.module;
                    if (m instanceof SourceModule) {
                        SourceModule sourceModule = (SourceModule) m;
                        File file = sourceModule.getFile();
                        if (file != null) {
                            ItemPointer p = new ItemPointer(file, outlineEntry.node);
                            pyOpenAction.run(p);
                        }
                    }
                }
            }
        }
        return ret;
    }

}
