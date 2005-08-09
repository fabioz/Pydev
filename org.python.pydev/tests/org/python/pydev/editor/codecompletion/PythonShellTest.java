/*
 * Created on Sep 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.InterpreterManagerStub;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.interpreters.IInterpreterManager;

/**
 * These tests should run, however the directory where the tests are run must be correct.
 * 
 * @author Fabio Zadrozny
 */
public class PythonShellTest extends CodeCompletionTestsBase{

    private PythonShell shell;

    public static void main(String[] args) {
        try {
//            PythonShellTest test = new PythonShellTest();
//            test.setUp();
//            test.testGlu();
//            test.tearDown();
            junit.textui.TestRunner.run(PythonShellTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        restorePythonPathWithSitePackages(false);
        this.shell = startShell();
    }

    /**
     * @throws IOException
     * @throws CoreException
     */
    public static PythonShell startShell() throws IOException, Exception {
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
        List list = shell.getImportCompletions("math", new ArrayList());

        Object[] element = null;
        element = (Object[]) list.get(0);
        assertEquals("__doc__", element[0]);
        assertEquals(29, list.size());

        
    }


    public void testErrorOnCompletions() throws IOException, CoreException {
        List list = shell.getImportCompletions("dfjslkfjds\n\n", getPythonpath());
        assertEquals(0, list.size());
        //don't show completion errors!
    }

    /**
     * @return
     */
    private List getPythonpath() {
        return nature.getAstManager().getProjectModulesManager().getCompletePythonPath();
    }

    public void testGlu() throws IOException, CoreException {
        List list = shell.getImportCompletions("OpenGL.GLUT", getPythonpath());
        
        assertTrue(list.size() > 10);
        assertIsIn(list, "glutInitDisplayMode");
    }

    private void assertIsIn(List list, String expected) {
        for (Object object : list) {
            Object o[] = (Object[]) object;
            if(o[0].equals(expected)){
                return;
            }
        }
        fail(String.format("The string %s was not found in the returned completions", expected));
    }
    
}