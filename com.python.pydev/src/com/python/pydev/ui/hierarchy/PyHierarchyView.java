package com.python.pydev.ui.hierarchy;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import edu.umd.cs.piccolox.swt.PSWTCanvas;

public class PyHierarchyView extends ViewPart {

	private static PSWTCanvas viewer;

	@Override
	public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        SashForm s = new SashForm(parent, SWT.HORIZONTAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        s.setLayoutData(layoutData);
        
        parent = s;
        System.out.println("creating...");
    	viewer = new PSWTCanvas(parent, 0);
//        viewer = new HierarchyViewer(parent, 0);
//		createTempHierarchy();
        
        Tree tree = new Tree(parent, 0);
        TreeItem item = new TreeItem(tree, 0);
        item.setText("foo");
        item.setImage(PydevPlugin.getImageCache().get(UIConstants.ASSIST_NEW_METHOD));

        item = new TreeItem(tree, 0);
        item.setText("foo");
        item.setImage(PydevPlugin.getImageCache().get(UIConstants.ASSIST_NEW_METHOD));
        
        item = new TreeItem(tree, 0);
        item.setText("foo");
        item.setImage(PydevPlugin.getImageCache().get(UIConstants.ASSIST_NEW_METHOD));
	}

	private void createTempHierarchy() {
        HierarchyNodeModel curr = new HierarchyNodeModel("curr");
        
        final HierarchyNodeModel par1pac1 = new HierarchyNodeModel("par1", "package1");
        final HierarchyNodeModel par1 = new HierarchyNodeModel("par1");
        final HierarchyNodeModel super1 = new HierarchyNodeModel("super1");
        final HierarchyNodeModel super2 = new HierarchyNodeModel("super2");
        final HierarchyNodeModel par2 = new HierarchyNodeModel("par2");
        final HierarchyNodeModel par3 = new HierarchyNodeModel("par3");
        
        super1.parents.add(super2);
        super2.parents.add(par1pac1);
		par1.parents.add(super1);
		par1.parents.add(super2);
		par2.parents.add(super1);
		par3.parents.add(super2);
		
		curr.parents.add(par1);
		curr.parents.add(par2);
		curr.parents.add(par3);
		
        curr.parents.add(new HierarchyNodeModel("par4"));
        
        final HierarchyNodeModel c1 = new HierarchyNodeModel("child1");
		curr.children.add(c1);
        curr.children.add(new HierarchyNodeModel("child2"));
        final HierarchyNodeModel c3 = new HierarchyNodeModel("child3");
        c3.parents.add(par3); //does not show (we go straight to the top or to the bottom)
		curr.children.add(c3);
        
        c1.children.add(new HierarchyNodeModel("sub1"));
        
        
//        viewer.setHierarchy(curr);
	}

	@Override
	public void setFocus() {
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
}
