/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import junit.framework.TestCase;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class HierarchyViewerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HierarchyViewerTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    public void testIt() throws Exception {
        Display display = new Display ();
        Shell shell = open(display);
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        display.dispose();
    }
    
    public static Shell open(Display display) {
        final Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());
        
        HierarchyViewer viewer = new HierarchyViewer(shell, 0);
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
        
        
        viewer.setHierarchy(curr);
        
        shell.open();
        return shell;
    }

}
