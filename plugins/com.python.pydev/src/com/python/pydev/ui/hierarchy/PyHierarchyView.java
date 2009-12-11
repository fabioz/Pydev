package com.python.pydev.ui.hierarchy;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import edu.umd.cs.piccolo.event.PInputEvent;

public class PyHierarchyView extends ViewPart implements HierarchyNodeViewListener {

    private static HierarchyViewer viewer;
    private MouseListener treeMouseListener;
    private Tree tree;
    private Object lock = new Object();

    @Override
    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        SashForm s = new SashForm(parent, SWT.VERTICAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        s.setLayoutData(layoutData);
        
        parent = s;
        viewer = new HierarchyViewer(parent, 0);
        
        tree = new Tree(parent, 0);
        treeMouseListener = new TreeMouseListener(tree);
    }

    public void setHierarchy(HierarchyNodeModel model) {
        viewer.setHierarchy(model);
        HierarchyNodeView initial = null;
        for (HierarchyNodeView v:viewer.allAdded){
            //we want to listen to clicks
            v.addListener(this);
            if(v.model == model){
                initial = v;
            }
        }
        if(initial != null){
            onClick(initial, null);
        }
    }

    @Override
    public void setFocus() {
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }

    public void onClick(final HierarchyNodeView view, final PInputEvent event) {
        if(event != null && event.getClickCount() == 2){
            IModule m = view.model.module;
            if(m != null && view.model.ast != null){
                ItemPointer pointer = new ItemPointer(m.getFile(), view.model.ast.name);
                new PyOpenAction().run(pointer);
            }
        }else{
            
            Runnable r = new Runnable(){
                

                public void run() {
                    synchronized(lock){
                        ClassDef ast = view.model.ast;
                        if(ast != null){
                            tree.removeAll();
                            DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create(ast);
                            Iterator<ASTEntry> outline = visitor.getOutline();
                            
                            HashMap<SimpleNode, TreeItem> c = new HashMap<SimpleNode, TreeItem>();
                            
                            while(outline.hasNext()){
                                ASTEntry entry = outline.next();
                                
                                TreeItem item = null;
                                if(entry.node instanceof FunctionDef){
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.PUBLIC_METHOD_ICON));
                                    if(view.model.module != null){
                                        item.setData(new ItemPointer(view.model.module.getFile(), ((FunctionDef)entry.node).name));
                                    }
                                    
                                }else if(entry.node instanceof ClassDef){
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.CLASS_ICON));
                                    if(view.model.module != null){
                                        item.setData(new ItemPointer(view.model.module.getFile(), ((ClassDef)entry.node).name));
                                    }
                                    
                                }else{
                                    item = createTreeItem(c, entry);
                                    item.setImage(PydevPlugin.getImageCache().get(UIConstants.PUBLIC_ATTR_ICON));
                                    if(view.model.module != null){
                                        item.setData(new ItemPointer(view.model.module.getFile(), entry.node));
                                    }
                                }
                                item.setText(entry.getName());
                                item.setExpanded(true);
                                tree.showItem(item);
                                tree.addMouseListener(treeMouseListener);
                            }
                        }
                    }
                }
                
            };
            
            Display.getDefault().asyncExec(r);
        }
    }

    /**
     * @param c
     * @param entry
     * @return
     */
    private TreeItem createTreeItem(HashMap<SimpleNode, TreeItem> c, ASTEntry entry) {
        TreeItem parent = null;
        
        ASTEntry par = entry.parent;
        if(par != null){
            parent = c.get(par.node);
        }
        
        TreeItem item;
        if(parent == null){
            item = new TreeItem(tree, 0);
        }else{
            item = new TreeItem(parent, 0);
        }
        c.put(entry.node, item);
        return item;
    }
}
