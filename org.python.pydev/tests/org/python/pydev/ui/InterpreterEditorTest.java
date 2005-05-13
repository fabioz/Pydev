/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.IBundleInfo;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterEditorTest extends TestCase {

    private Shell shell;
    private Display display;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InterpreterEditorTest.class);
    }

    private void createSShell() {
        shell = new org.eclipse.swt.widgets.Shell();
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        BundleInfo.setBundleInfo(new IBundleInfo(){

            public File getRelativePath(IPath relative) throws CoreException {
                if(relative.toString().indexOf("interpreterInfo.py") != -1){
                    return new File("./PySrc/interpreterInfo.py");
                }
                throw new RuntimeException("Not available info on: "+relative);
            }

            public String getPluginID() {
                return "plugin_id";
            }});
        display = new Display();
        createSShell();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        BundleInfo.setBundleInfo(null);
    }

    /**
     * @param display
     */
    private void goToManual(Display display) {
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        System.out.println("finishing...");
        display.dispose();
    }

    /**
     * 
     */
    public void testIt() {
        shell.open();

        InterpreterEditor editor = new InterpreterEditor("editor", "label", shell);
        shell.pack();
        shell.setSize(new org.eclipse.swt.graphics.Point(300, 300));
        goToManual(display);
        
    }
}
