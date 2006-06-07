/*
 * Created on Jan 7, 2006
 */
package com.python.pydev.ui;

import java.net.MalformedURLException;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.TestDependent;

public class GetInfoDialogTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GetInfoDialogTest.class);
    }


    private Shell shell;
    protected Display display;

    private void createSShell() {
        shell = new org.eclipse.swt.widgets.Shell();
    }
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if(!TestDependent.HAS_SWT_ON_PATH){
            return;
        }

        display = new Display();
        createSShell();
    }


    /**
     * @throws MalformedURLException
     * 
     */
    public void testIt() throws MalformedURLException {
        if(display != null){
            GetInfoDialog editor = new GetInfoDialog(shell);
            editor.setBlockOnOpen(true);

            editor.open();
        }
    }
}
