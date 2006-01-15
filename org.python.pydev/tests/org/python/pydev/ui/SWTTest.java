/*
 * Created on Jan 15, 2006
 */
package org.python.pydev.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor;

import junit.framework.TestCase;

public class SWTTest extends TestCase{

    protected Shell shell;
    protected Display display;
    private void createSShell() {
        shell = new org.eclipse.swt.widgets.Shell();
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        BundleInfo.setBundleInfo(new BundleInfoStub());
        try {
            if(TestDependent.HAS_SWT_ON_PATH){
                display = new Display();
                createSShell();
            }
        } catch (UnsatisfiedLinkError e) {
            //ok, ignore it.
            e.printStackTrace();
        }
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
    protected void goToManual(Display display) {
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        System.out.println("finishing...");
        display.dispose();
    }

}
