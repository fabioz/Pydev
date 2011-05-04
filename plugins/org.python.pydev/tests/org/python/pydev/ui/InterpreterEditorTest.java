/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor;
import org.python.pydev.ui.pythonpathconf.PythonInterpreterEditor;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterEditorTest extends SWTTest {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterpreterEditorTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        AbstractInterpreterEditor.USE_ICONS = false;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        AbstractInterpreterEditor.USE_ICONS = true;
    }


    public void testIt(){
        if(display != null){
            PythonInterpreterEditor editor = new PythonInterpreterEditor("label", shell, new PythonInterpreterManager(new PreferenceStore()));
            shell.pack();
            shell.setSize(new org.eclipse.swt.graphics.Point(300, 300));
            shell.open();
            //goToManual(display);
        }
    }
}
