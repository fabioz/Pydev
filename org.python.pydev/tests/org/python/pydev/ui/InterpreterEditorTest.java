/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.core.runtime.Preferences;
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
            PythonInterpreterEditor editor = new PythonInterpreterEditor("label", shell, new PythonInterpreterManager(new Preferences()));
            shell.pack();
            shell.setSize(new org.eclipse.swt.graphics.Point(300, 300));
            shell.open();
            //goToManual(display);
        }
    }
}
