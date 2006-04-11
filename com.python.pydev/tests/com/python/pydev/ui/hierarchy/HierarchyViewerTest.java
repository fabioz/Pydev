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
        
        curr.parents.add(new HierarchyNodeModel("par1"));
        curr.parents.add(new HierarchyNodeModel("par2"));
        
        final HierarchyNodeModel c1 = new HierarchyNodeModel("child1");
		curr.children.add(c1);
        curr.children.add(new HierarchyNodeModel("child2"));
        
        c1.children.add(new HierarchyNodeModel("sub1"));
        
        
        viewer.setHierarchy(curr);
        
        shell.open();
        return shell;
    }

}
