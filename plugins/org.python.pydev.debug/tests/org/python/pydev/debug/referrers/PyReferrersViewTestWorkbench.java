/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.referrers;

import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.DebuggerTestUtils;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;

public class PyReferrersViewTestWorkbench extends AbstractWorkbenchTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(PyReferrersViewTestWorkbench.class.getName());

        suite.addTestSuite(PyReferrersViewTestWorkbench.class);

        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

    private IFile debugFile;
    private PyEdit debugEditor;
    private DebuggerTestUtils debuggerTestUtils;

    /**
     * Creates the debug file and editor.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        debugFile = initFile.getParent().getFile(new Path("debug_file.py"));
        String mod1Contents = "from pack1.pack2 import mod1\nprint mod1\nprint 'now'\n";
        debugFile.create(new ByteArrayInputStream(mod1Contents.getBytes()), true, null);
        debugFile.refreshLocal(IResource.DEPTH_ZERO, null);

        debugEditor = (PyEdit) PyOpenEditor.doOpenEditor(debugFile);

        debuggerTestUtils = new DebuggerTestUtils(debugEditor);
    }

    /**
     * Removes the debug file and closes the debug editor
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (debugFile != null) {
            debugFile.delete(true, null);
        }
        if (debugEditor != null) {
            debugEditor.close(false);
        }
    }

    public void testReferrersView() throws Exception {
        try {
            ReferrersView view = ReferrersView.getView(true);
            //goToManual();

        } catch (Exception e) {
            Log.log(e);
        }

    }

}
