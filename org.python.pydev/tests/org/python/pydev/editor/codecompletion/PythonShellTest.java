/*
 * Created on Sep 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

/**
 * These tests should run, however the directory where the tests are run must be correct.
 * 
 * @author Fabio Zadrozny
 */
public class PythonShellTest extends TestCase {

    private PythonShell shell;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonShellTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        this.shell = startShell();
    }

    /**
     * @throws IOException
     * @throws CoreException
     */
    public static PythonShell startShell() throws IOException, CoreException {
        File f = new File("PySrc/pycompletionserver.py");
        PythonShell shell = new PythonShell(f);
        shell.startIt();
        return shell;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        shell.endIt();
    }

    public void testGetGlobalCompletions() throws IOException, CoreException {
        List list = shell.getImportCompletions("math");

        Object[] element = null;
        
        element = (Object[]) list.get(0);
        assertEquals("__doc__", element[0]);

        assertEquals(29, list.size());
    }


    public void testErrorOnCompletions() throws IOException, CoreException {
        List list = shell.getImportCompletions("dfjslkfjds\n\n");
        assertEquals(1, list.size());
        Object object[] = (Object[]) list.get(0);
        assertEquals("SERVER_ERROR", object[0]);
    }

}