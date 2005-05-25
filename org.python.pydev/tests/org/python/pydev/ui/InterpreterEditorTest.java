/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.ui.pythonpathconf.InterpreterEditor;

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
        BundleInfo.setBundleInfo(new BundleInfoStub());
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
//        while (!shell.isDisposed()) {
//            if (!display.readAndDispatch())
//                display.sleep();
//        }
        System.out.println("finishing...");
        display.dispose();
    }

    /**
     * @throws MalformedURLException
     * 
     */
    public void testIt() throws MalformedURLException {
        shell.open();

        InterpreterEditor editor = new InterpreterEditor("label", shell, new InterpreterManager(new Preferences()));
        shell.pack();
        shell.setSize(new org.eclipse.swt.graphics.Point(300, 300));
        goToManual(display);
        
    }
}
