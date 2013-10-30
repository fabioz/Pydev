/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import junit.framework.TestCase;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyUnitViewTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyUnitViewTest.class);
    }

    public void testLineTracker() throws Exception {
        // PyUnitViewTest fails because it depends on org.eclipse.debug.ui.console.IConsoleLineTracker
        // being able to be loaded. But IConsoleLineTracker is in a plug-in with an activator that in
        // turn relies on the workbench being loaded, leading to a test error. This isn't a problem
        // when run within Eclipse as a (plain) JUint test because the Activator is skipped.
        // Since the classes under test rely on IConsoleLineTracker, the test must be run as a
        // GUI enabled Plug-in test (i.e workbench started), however if you do that the test fails
        // because of interactions with other services in the workbench.
        if (PydevPlugin.getDefault() != null) {
            if (SharedCorePlugin.skipKnownFailures()) {
                return;
            }
        }
        PyUnitView pyUnitView = new PyUnitView();
        PyUnitTestRun testRun = new PyUnitTestRun(null);
        String error = "File \"Y:\\test_python\\src\\mod1\\mod2\\test_it2.py\", line 45, in testAnotherCase";
        PyUnitTestResult result = new PyUnitTestResult(testRun, "fail", "c:\\temp.py", "TestCase.foo", "", "\n\n"
                + error + "\nfoo\n", "0");

        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        Shell composite = new Shell(display);
        composite.setLayout(new FillLayout());

        StyledText text = new StyledText(composite, 0);
        pyUnitView.setTextComponent(text);
        pyUnitView.getLineTracker().setOnlyCreateLinksForExistingFiles(false);
        pyUnitView.onSelectResult(result);

        //uncomment below to see results.
        //        composite.pack();
        //        composite.open();
        //
        //        while (!composite.isDisposed()) {
        //            if (!display.readAndDispatch()){
        //                display.sleep();
        //            }
        //        }

        StyleRange[] styleRanges = text.getStyleRanges();
        assertEquals(1, styleRanges.length);
        assertEquals(69, styleRanges[0].start);
        assertEquals(error.length(), styleRanges[0].length);
    }

}
