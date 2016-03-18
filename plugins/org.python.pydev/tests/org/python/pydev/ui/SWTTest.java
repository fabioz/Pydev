/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package org.python.pydev.ui;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.PydevPlugin;

public class SWTTest extends TestCase {

    protected Shell shell;
    protected Display display;

    private void createSShell() {
        shell = new org.eclipse.swt.widgets.Shell();
    }

    public void testIt() throws Exception {

    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
        try {
            if (TestDependent.HAS_SWT_ON_PATH) {
                display = createDisplay();
                createSShell();
            }
        } catch (UnsatisfiedLinkError e) {
            //ok, ignore it.
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    protected Display createDisplay() {
        return new Display();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PydevPlugin.setBundleInfo(null);
    }

    /**
     * @param display
     */
    protected void goToManual(Display display) {
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        System.out.println("finishing...");
        display.dispose();
    }

}
