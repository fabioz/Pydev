/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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

public class PyUnitViewTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyUnitViewTest.class);
    }

    public void testLineTracker() throws Exception {
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
