/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.uiutils.DialogMemento;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;
import com.python.pydev.ui.hierarchy.TreeNode;
import com.python.pydev.ui.hierarchy.TreeNodeContentProvider;

/**
 * @author fabioz
 *
 */
public final class PyOutlineSelectionDialog extends TreeSelectionDialog {

    /**
     * Should we show the parents or not?
     */
    private boolean showParentHierarchy;

    /**
     * Label indicating what are we showing.
     */
    private Label labelCtrlO;

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

    /**
     * Structure without parents.
     */
    private TreeNode<OutlineEntry> root;

    /**
     * Structure with the parent methods.
     */
    private TreeNode<OutlineEntry> rootWithParents;

    /**
     * Helper to save/restore geometry.
     */
    private final DialogMemento memento;

    /**
     * Listener to handle the 2nd ctrl+O
     */
    private KeyListener ctrlOlistener;

    
    private PyOutlineSelectionDialog(Shell shell) {
        super(shell, createLabelProvider(), new TreeNodeContentProvider());
        if(CorePlugin.getDefault() != null){
            memento = new DialogMemento(getShell(), "com.python.pydev.actions.PyShowOutline");
        }else{
            memento = null;
        }

        setMessage("Filter (press enter to go to selected element)");
        setTitle("PyDev: Quick Outline");
        setAllowMultiple(false);
        this.showParentHierarchy = false;
    }

    
    /**
     * Handle the creation for earlier versions of Eclipse.
     */
    protected static ILabelProvider createLabelProvider() {
        try {
            return new LabelProviderWithDecoration(
                    new ShowOutlineLabelProvider(), 
                    PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), 
                    null);
        } catch (Throwable e) {
            return new ShowOutlineLabelProvider();
        }
    }

    /**
     * Constructor to be used if the pyedit is not available (info must be pre-calculated)
     */
    public PyOutlineSelectionDialog(Shell shell, SimpleNode ast, HashMap<SimpleNode, HierarchyNodeModel> nodeToModel) {
        this(shell);
        this.ast = ast;
        this.nodeToModel = nodeToModel;
        calculateHierarchy();
        setInput(root);
    }

    /**
     * Constructor to be used if the pyedit is available (in which case the info will be calculated on demand)
     */
    public PyOutlineSelectionDialog(Shell shell, PyEdit pyEdit) {
        this(shell);
        this.pyEdit = pyEdit;
        calculateHierarchy();
        setInput(root);
    }

    public boolean close() {
        if(memento != null){
            memento.writeSettings(getShell());
        }
        return super.close();
    }

    protected Point getInitialSize() {
        if(memento != null){
            return memento.getInitialSize(super.getInitialSize(), getShell());
        }
        return new Point(640, 480);
    }

    protected Point getInitialLocation(Point initialSize) {
        if(memento != null){
            return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
        }
        return new Point(250, 250);
    }

    public Control createDialogArea(Composite parent) {
        if(memento != null){
            memento.readSettings();
        }
        Control ret = super.createDialogArea(parent);
        ctrlOlistener = new KeyListener() {

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if ((e.keyCode == 'o' || e.keyCode == 'O') && e.stateMask == SWT.CTRL) {
                    toggleShowParentHierarchy();
                }
            }
        };
        this.text.addKeyListener(ctrlOlistener);
        this.getTreeViewer().getTree().addKeyListener(ctrlOlistener);
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
    protected int getDefaultMargins() {
        return 0;
    }

    @Override
    protected int getDefaultSpacing() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        labelCtrlO = new Label(parent, SWT.NONE);
        this.labelCtrlO.addKeyListener(ctrlOlistener);
        updateShowParentHierarchyMessage();
        return labelCtrlO;
    }

    protected void toggleShowParentHierarchy() {
        showParentHierarchy = !showParentHierarchy;
        updateShowParentHierarchyMessage();

        TreeViewer treeViewer = this.getTreeViewer();
        if (showParentHierarchy) {
            //Create the TreeNode structure if it's still not created...
            calculateHierarchyWithParents();
            treeViewer.setInput(rootWithParents);
        } else {
            calculateHierarchy();
            treeViewer.setInput(root);
        }
        
    }

    private void calculateHierarchy() {
        if (root != null) {
            return;
        }
        
        if(this.ast == null && pyEdit != null){
            this.ast = pyEdit.getAST();
        }

        if(ast == null){
            return;
        }
        
        DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create(ast);
        if (visitor == null) {
            return;
        }

        Map<ASTEntry, TreeNode<OutlineEntry>> entryToTreeNode = new HashMap<ASTEntry, TreeNode<OutlineEntry>>();

        //Step 1: create 'regular' tree structure from the nodes.
        TreeNode<OutlineEntry> root = new TreeNode<OutlineEntry>(null, null, null);

        for (Iterator<ASTEntry> it = visitor.getOutline(); it.hasNext();) {
            ASTEntry next = it.next();
            TreeNode<OutlineEntry> n;
            if (next.parent != null) {
                TreeNode<OutlineEntry> parent = entryToTreeNode.get(next.parent);
                if (parent == null) {
                    Log.log("Unexpected condition: child found before parent!");
                    parent = root;
                }
                n = new TreeNode<OutlineEntry>(parent, new OutlineEntry(next), null);

            } else {
                n = new TreeNode<OutlineEntry>(root, new OutlineEntry(next), null);
            }
            entryToTreeNode.put(next, n);
        }
        this.root = root;
    }

    private void calculateHierarchyWithParents() {
        if (rootWithParents != null) {
            return;
        }
        
        calculateHierarchy(); //make sure the root is OK
        
        if(root == null){
            return;
        }
        
        rootWithParents = root.createCopy(null);
        

        if(nodeToModel == null){
            //Step 2: create mapping: classdef to hierarchy model.
            nodeToModel = new HashMap<SimpleNode, HierarchyNodeModel>();
            fillClasses(rootWithParents, new NullProgressMonitor());
        }
        
        fillHierarchy(rootWithParents);
    }

    private void fillHierarchy(TreeNode<OutlineEntry> entry) {
        ArrayList<TreeNode<OutlineEntry>> copy = new ArrayList<TreeNode<OutlineEntry>>(entry.children);
        for (TreeNode<OutlineEntry> nextEntry : copy) {
            HierarchyNodeModel model = this.nodeToModel.get(nextEntry.data.node);
            addMethods(nextEntry, model);
            fillHierarchy(nextEntry);
        }
    }

    
    private void addMethods(TreeNode<OutlineEntry> nextEntry, HierarchyNodeModel model) {
        if(model == null || model.parents == null){
            return;
        }
        for(HierarchyNodeModel parent:model.parents){
            DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.createForChildren(parent.ast);
            if (visitor == null) {
                continue;
            }

            Iterator<ASTEntry> outline = visitor.getOutline();
            while(outline.hasNext()){
                ASTEntry entry = outline.next();
                if(entry.parent == null){
                    //only direct children...
                    new TreeNode<OutlineEntry>(nextEntry, new OutlineEntry(entry, parent), null);
                }
            }
            addMethods(nextEntry, parent);
        }
    }

    
    private void fillClasses(TreeNode<OutlineEntry> entry, IProgressMonitor monitor) {
        if (entry.children.size() == 0) {
            return;
        }

        //Iterate in a copy, since we may change the original...
        for (TreeNode<OutlineEntry> nextEntry : entry.children) {

            if (nextEntry.data.node instanceof ClassDef) {
                ClassDef classDef = (ClassDef) nextEntry.data.node;
                IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                if (pyRefactoring instanceof IPyRefactoring2) {
                    PySelection ps = new PySelection(pyEdit.getDocument(), classDef.name.beginLine - 1, classDef.name.beginColumn - 1);

                    RefactoringRequest refactoringRequest;
                    try {
                        refactoringRequest = PyRefactorAction.createRefactoringRequest(monitor, pyEdit, ps);
                        IPyRefactoring2 r2 = (IPyRefactoring2) pyRefactoring;
                        HierarchyNodeModel model = r2.findClassHierarchy(refactoringRequest, true);
                        nodeToModel.put(nextEntry.data.node, model);
                    } catch (MisconfigurationException e) {
                        Log.log(e);
                    }
                }
            }

            //Enter the leaf to fill it too.
            fillClasses(nextEntry, monitor);
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
                TreeNode<OutlineEntry> n = (TreeNode<OutlineEntry>) result[0];
                OutlineEntry outlineEntry = n.data;
                if(outlineEntry.model == null){
                    Location location = new Location(NodeUtils.getNameLineDefinition(outlineEntry.node) - 1,
                            NodeUtils.getNameColDefinition(outlineEntry.node) - 1);
                    new PyOpenAction().showInEditor(pyEdit, location, location);
                }else{
                    PyOpenAction pyOpenAction = new PyOpenAction();
                    IModule m = outlineEntry.model.module;
                    if(m instanceof SourceModule){
                        SourceModule sourceModule = (SourceModule) m;
                        File file = sourceModule.getFile();
                        if(file != null){
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
