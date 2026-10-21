/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;

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
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TestDependent.PYTHON2_OPENGL_PACKAGES == null) {
            restorePythonPathWithSitePackages(false);
        } else {
            restorePythonPathWithCustomSystemPath(false, TestDependent.getCompletePythonLib(true, isPython3Test()) + "|"
                    + TestDependent.PYTHON2_OPENGL_PACKAGES);
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
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        shell.endIt();
    }

    public void testGetGlobalCompletions() throws Exception {
        List<String[]> list = shell.getImportCompletions("math", getPythonpath()).o2;

        Object[] element = null;
        element = list.get(0);
        assertEquals("__doc__", element[0]);
        assertTrue(list.size() >= 29);

    }

    public void testErrorOnCompletions() throws Exception {
        List<String[]> list = shell.getImportCompletions("dfjslkfjds\n\n", getPythonpath()).o2;
        assertEquals(0, list.size());
        //don't show completion errors!
    }

    /**
     * A module whose import aborts the interpreter kills the server while the request is processed: it's answered as
     * having no completions and counted as a failure, while the restarted server answers the next requests. It's not
     * requested again under the same PYTHONPATH (which would kill the restarted server), but each answer it then gets
     * without being sent is still counted. Under another PYTHONPATH, it's sent again.
     */
    public void testImportThatKillsTheServer() throws Exception {
        File dir = Files.createTempDirectory("pydev_shell_test").toFile();
        try {
            //Each import attempt leaves a mark, so that the test can tell whether the request was sent.
            File marks = new File(dir, "marks.txt");
            FileUtils.writeStrToFile("import os\n"
                    + "with open(os.path.join(os.path.dirname(__file__), 'marks.txt'), 'a') as f:\n"
                    + "    f.write('x')\n"
                    + "os.abort()\n", new File(dir, "pydev_shell_test_aborts.py"));
            List<String> pythonpath = new ArrayList<>(getPythonpath());
            pythonpath.add(dir.getAbsolutePath());

            long failures = AbstractShell.getFailureCount();
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", pythonpath).o2.size());
            assertEquals("x", FileUtils.getFileContents(marks));
            assertEquals(failures + 1, AbstractShell.getFailureCount());

            assertIsIn(shell.getImportCompletions("math", pythonpath).o2, "sqrt");

            //Answered without being sent, but still counted.
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", pythonpath).o2.size());
            assertEquals("x", FileUtils.getFileContents(marks));
            assertEquals(failures + 2, AbstractShell.getFailureCount());
            assertIsIn(shell.getImportCompletions("math", pythonpath).o2, "sqrt");

            //Another PYTHONPATH may resolve the module differently, so, it's sent again.
            List<String> otherPythonpath = new ArrayList<>(pythonpath);
            otherPythonpath.add(new File(dir, "other").getAbsolutePath());
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", otherPythonpath).o2.size());
            assertEquals("xx", FileUtils.getFileContents(marks));
            assertEquals(failures + 3, AbstractShell.getFailureCount());
            assertIsIn(shell.getImportCompletions("math", otherPythonpath).o2, "sqrt");
        } finally {
            FileUtils.deleteDirectoryTree(dir);
        }
    }

    /**
     * A server that died between requests is restarted, its PYTHONPATH restored, and the request resent to it.
     */
    public void testRequestIsResentWhenTheServerDiedBeforeIt() throws Exception {
        File dir = Files.createTempDirectory("pydev_shell_test").toFile();
        try {
            //Exits shortly after being imported, i.e.: not while processing a request.
            FileUtils.writeStrToFile("import os\nimport threading\nthreading.Timer(0.5, os._exit, (3,)).start()\n"
                    + "EXITS_LATER = 1\n", new File(dir, "pydev_shell_test_exits_later.py"));
            FileUtils.writeStrToFile("PLAIN = 1\n", new File(dir, "pydev_shell_test_plain.py"));
            List<String> pythonpath = new ArrayList<>(getPythonpath());
            pythonpath.add(dir.getAbsolutePath());

            long failures = AbstractShell.getFailureCount();
            assertIsIn(shell.getImportCompletions("pydev_shell_test_exits_later", pythonpath).o2, "EXITS_LATER");
            assertEquals(failures, AbstractShell.getFailureCount());
            Thread.sleep(3000);

            //Found only if the restarted server got the PYTHONPATH back.
            assertIsIn(shell.getImportCompletions("pydev_shell_test_plain", pythonpath).o2, "PLAIN");
            assertEquals(failures + 1, AbstractShell.getFailureCount());
        } finally {
            FileUtils.deleteDirectoryTree(dir);
        }
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

    public void testGlu() throws Exception {
        // Not sure why this fails, but it fails on (plain) JUnit for me
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON2_OPENGL_PACKAGES != null) {
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
        fail(StringUtils.format(
                "The string %s was not found in the returned completions", expected));
    }

}