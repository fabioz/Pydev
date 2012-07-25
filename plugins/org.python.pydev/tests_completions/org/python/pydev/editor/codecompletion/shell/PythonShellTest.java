/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * These tests should run, however the directory where the tests are run must be correct.
 * 
 * @author Fabio Zadrozny
 */
public class PythonShellTest extends CodeCompletionTestsBase {

    private PythonShell shell;

    public static void main(String[] args) {
        try {
            PythonShellTest test = new PythonShellTest();
            test.setUp();
            test.testGlu();
            test.tearDown();
            junit.textui.TestRunner.run(PythonShellTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        if (TestDependent.PYTHON_OPENGL_PACKAGES == null) {
            restorePythonPathWithSitePackages(false);
        } else {
            restorePythonPathWithCustomSystemPath(false, TestDependent.GetCompletePythonLib(true) + "|"
                    + TestDependent.PYTHON_OPENGL_PACKAGES);
        }
        this.shell = startShell();
    }

    /**
     * @throws IOException
     * @throws CoreException
     */
    public static PythonShell startShell() throws IOException, Exception {
        PythonShell shell = new PythonShell();
        shell.startIt(nature);
        return shell;
    }

    /**
     * @throws IOException
     * @throws CoreException
     */
    public static IronpythonShell startIronpythonShell(IPythonNature nature) throws IOException, Exception {
        IronpythonShell shell = new IronpythonShell();
        shell.startIt(nature);
        return shell;
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        super.tearDown();
        shell.endIt();
    }

    public void testGetGlobalCompletions() throws IOException, CoreException {
        List<String[]> list = shell.getImportCompletions("math", getPythonpath()).o2;

        Object[] element = null;
        element = (Object[]) list.get(0);
        assertEquals("__doc__", element[0]);
        assertTrue(list.size() >= 29);

    }

    public void testErrorOnCompletions() throws IOException, CoreException {
        List<String[]> list = shell.getImportCompletions("dfjslkfjds\n\n", getPythonpath()).o2;
        assertEquals(0, list.size());
        //don't show completion errors!
    }

    /**
     * @return
     */
    private List<String> getPythonpath() {
        try {
            return nature.getAstManager().getModulesManager()
                    .getCompletePythonPath(nature.getProjectInterpreter(), nature.getRelatedInterpreterManager());
        } catch (PythonNatureWithoutProjectException e) {
            throw new RuntimeException(e);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void testGlu() throws IOException, CoreException {
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            List<String[]> list = shell.getImportCompletions("OpenGL.GLUT", getPythonpath()).o2;

            assertTrue("Expected the number of completions to be > 10. Found: \n" + list, list.size() > 10);
            assertIsIn(list, "glutInitDisplayMode");
        }
    }

    private void assertIsIn(List<String[]> list, String expected) {
        for (String[] o : list) {
            if (o[0].equals(expected)) {
                return;
            }
        }
        fail(StringUtils.format("The string %s was not found in the returned completions", expected));
    }

}