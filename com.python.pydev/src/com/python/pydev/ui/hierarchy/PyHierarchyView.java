package com.python.pydev.ui.hierarchy;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
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
    private Tree tree;

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
	}

	public void setHierarchy(HierarchyNodeModel model) {
		viewer.setHierarchy(model);
        for (HierarchyNodeView v:viewer.allAdded){
            //we want to listen to clicks
            v.addListener(this);
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
        Runnable r = new Runnable(){
            
            public void run() {
                synchronized(tree){
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
                                
                            }else if(entry.node instanceof ClassDef){
                                item = createTreeItem(c, entry);
                                item.setImage(PydevPlugin.getImageCache().get(UIConstants.CLASS_ICON));
                                
                            }else{
                                item = createTreeItem(c, entry);
                                item.setImage(PydevPlugin.getImageCache().get(UIConstants.PUBLIC_ATTR_ICON));
                            }
                            item.setText(entry.getName());
                            item.setExpanded(true);
                            tree.showItem(item);
                        }
                        
                    }
                }
            }
            
        };
        
        Display.getDefault().asyncExec(r);

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
