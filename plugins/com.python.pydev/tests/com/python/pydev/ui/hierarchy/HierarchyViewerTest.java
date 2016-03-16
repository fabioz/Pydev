/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import junit.framework.TestCase;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.BundleInfoStub;

public class HierarchyViewerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HierarchyViewerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIt() throws Exception {
        if (!TestDependent.HAS_SWT_ON_PATH) {
            return;
        }
        Display display = new Display();
        Shell shell = open(display);
        //        while (!shell.isDisposed()) {
        //            if (!display.readAndDispatch()) display.sleep();
        //        }
        //        display.dispose();
    }

    public static Shell open(Display display) {
        final Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());

        HierarchyViewer viewer = new HierarchyViewer();
        viewer.createPartControl(shell);
        HierarchyNodeModel curr = new HierarchyNodeModel("curr");

        final HierarchyNodeModel par1pac1 = new HierarchyNodeModel("par1", "package1", null);
        final HierarchyNodeModel par1 = new HierarchyNodeModel("par1", "pack2", null);
        final HierarchyNodeModel super1 = new HierarchyNodeModel("super1", "pack3", null);
        final HierarchyNodeModel super2 = new HierarchyNodeModel("super2", "pack3", null);
        final HierarchyNodeModel par2 = new HierarchyNodeModel("par2", "pack3", null);
        final HierarchyNodeModel par3 = new HierarchyNodeModel("par3", "pack3", null);

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

        final HierarchyNodeModel c1 = new HierarchyNodeModel("child1", "pack3", null);
        curr.children.add(c1);
        curr.children.add(new HierarchyNodeModel("child2", "pack3", null));
        final HierarchyNodeModel c3 = new HierarchyNodeModel("child3", "pack3", null);
        c3.parents.add(par3); //does not show (we go straight to the top or to the bottom)
        curr.children.add(c3);

        c1.children.add(new HierarchyNodeModel("sub1", "pack3", null));

        viewer.setHierarchy(curr);

        shell.open();
        return shell;
    }

}
