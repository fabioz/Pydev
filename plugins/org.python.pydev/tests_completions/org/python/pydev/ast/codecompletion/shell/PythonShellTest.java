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
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
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
        restoreTestPythonPath(false);
        this.shell = startShell();
    }

    private void restoreTestPythonPath(boolean force) {
        if (TestDependent.PYTHON2_OPENGL_PACKAGES == null) {
            restorePythonPathWithSitePackages(force);
        } else {
            restorePythonPathWithCustomSystemPath(force, TestDependent.getCompletePythonLib(true, isPython3Test()) + "|"
                    + TestDependent.PYTHON2_OPENGL_PACKAGES);
        }
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
     * A module whose import aborts the interpreter kills the server while the request is processed. The request is
     * resent once to the restarted server, which it kills again: it's answered as having no completions, and the
     * answer is counted as lost, while the restarted server answers the next requests. It's not requested again under
     * the same PYTHONPATH (which would kill the restarted server), but each answer it then gets without being sent is
     * still counted as lost. Under another PYTHONPATH, it's sent again.
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

            long lost = AbstractShell.getLostAnswerCount();
            long failures = AbstractShell.getFailureCount();
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", pythonpath).o2.size());
            assertEquals("xx", FileUtils.getFileContents(marks));
            assertEquals(lost + 1, AbstractShell.getLostAnswerCount());
            assertTrue(AbstractShell.getFailureCount() >= failures + 2);

            assertIsIn(shell.getImportCompletions("math", pythonpath).o2, "sqrt");
            assertEquals(lost + 1, AbstractShell.getLostAnswerCount());

            //Answered without being sent, but still counted.
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", pythonpath).o2.size());
            assertEquals("xx", FileUtils.getFileContents(marks));
            assertEquals(lost + 2, AbstractShell.getLostAnswerCount());
            assertIsIn(shell.getImportCompletions("math", pythonpath).o2, "sqrt");

            //Another PYTHONPATH may resolve the module differently, so, it's sent again.
            List<String> otherPythonpath = new ArrayList<>(pythonpath);
            otherPythonpath.add(new File(dir, "other").getAbsolutePath());
            assertEquals(0, shell.getImportCompletions("pydev_shell_test_aborts", otherPythonpath).o2.size());
            assertEquals("xxxx", FileUtils.getFileContents(marks));
            assertEquals(lost + 3, AbstractShell.getLostAnswerCount());
            assertIsIn(shell.getImportCompletions("math", otherPythonpath).o2, "sqrt");
            assertEquals(lost + 3, AbstractShell.getLostAnswerCount());
        } finally {
            FileUtils.deleteDirectoryTree(dir);
        }
    }

    /**
     * A server that dies once while processing a request (e.g.: of something other than the request) gets the request
     * resent after it's restarted, and the answer isn't lost.
     */
    public void testRequestIsResentWhenTheServerDiedProcessingItOnce() throws Exception {
        File dir = Files.createTempDirectory("pydev_shell_test").toFile();
        try {
            //Aborts only the first time it's imported.
            FileUtils.writeStrToFile("import os\n"
                    + "marks = os.path.join(os.path.dirname(__file__), 'marks.txt')\n"
                    + "if not os.path.exists(marks):\n"
                    + "    open(marks, 'w').close()\n"
                    + "    os.abort()\n"
                    + "ABORTS_ONCE = 1\n", new File(dir, "pydev_shell_test_aborts_once.py"));
            List<String> pythonpath = new ArrayList<>(getPythonpath());
            pythonpath.add(dir.getAbsolutePath());

            long lost = AbstractShell.getLostAnswerCount();
            long failures = AbstractShell.getFailureCount();
            assertIsIn(shell.getImportCompletions("pydev_shell_test_aborts_once", pythonpath).o2, "ABORTS_ONCE");
            assertEquals(lost, AbstractShell.getLostAnswerCount());
            assertTrue(AbstractShell.getFailureCount() > failures);

            //Not skipped later on.
            assertIsIn(shell.getImportCompletions("pydev_shell_test_aborts_once", pythonpath).o2, "ABORTS_ONCE");
            assertEquals(lost, AbstractShell.getLostAnswerCount());
        } finally {
            FileUtils.deleteDirectoryTree(dir);
        }
    }

    /**
     * The answers lost by a thread aren't counted for other threads.
     */
    public void testLostAnswersAreCountedPerThread() throws Exception {
        File dir = Files.createTempDirectory("pydev_shell_test").toFile();
        try {
            FileUtils.writeStrToFile("import os\nos.abort()\n", new File(dir, "pydev_shell_test_aborts_in_thread.py"));
            List<String> pythonpath = new ArrayList<>(getPythonpath());
            pythonpath.add(dir.getAbsolutePath());

            long lost = AbstractShell.getLostAnswerCount();
            long[] lostInThread = new long[2];
            Exception[] exception = new Exception[1];
            Thread thread = new Thread(() -> {
                try {
                    lostInThread[0] = AbstractShell.getLostAnswerCount();
                    shell.getImportCompletions("pydev_shell_test_aborts_in_thread", pythonpath);
                    lostInThread[1] = AbstractShell.getLostAnswerCount();
                } catch (Exception e) {
                    exception[0] = e;
                }
            });
            thread.start();
            thread.join();
            if (exception[0] != null) {
                throw exception[0];
            }
            assertEquals(lostInThread[0] + 1, lostInThread[1]);
            assertEquals(lost, AbstractShell.getLostAnswerCount());
            assertIsIn(shell.getImportCompletions("math", pythonpath).o2, "sqrt");
        } finally {
            FileUtils.deleteDirectoryTree(dir);
        }
    }

    /**
     * A compiled module doesn't keep tokens built from a lost answer: reading it asks the server again, on the reading
     * thread, which counts the answer as lost if it is again. After it's lost twice, the server isn't asked again, but
     * each read is still counted as a lost answer for the reading thread.
     */
    public void testCompiledModuleDoesNotKeepLostAnswer() throws Exception {
        File dir = Files.createTempDirectory("pydev_shell_test").toFile();
        try {
            File marks = new File(dir, "marks.txt");
            FileUtils.writeStrToFile("import os\n"
                    + "with open(os.path.join(os.path.dirname(__file__), 'marks.txt'), 'a') as f:\n"
                    + "    f.write('x')\n"
                    + "os.abort()\n", new File(dir, "pydev_shell_test_compiled_aborts.py"));
            restorePythonPathWithCustomSystemPath(true,
                    TestDependent.getCompletePythonLib(true, isPython3Test()) + "|" + dir.getAbsolutePath());
            AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);

            //Created (and its answer lost) on another thread, e.g.: PyDev's interpreter synchronization.
            CompiledModule[] module = new CompiledModule[1];
            long[] lostInThread = new long[2];
            Thread thread = new Thread(() -> {
                lostInThread[0] = AbstractShell.getLostAnswerCount();
                module[0] = new CompiledModule("pydev_shell_test_compiled_aborts",
                        nature.getAstManager().getModulesManager(), nature);
                lostInThread[1] = AbstractShell.getLostAnswerCount();
            });
            thread.start();
            thread.join();
            assertEquals(lostInThread[0] + 1, lostInThread[1]);
            assertEquals("xx", FileUtils.getFileContents(marks));

            //Reading it asks the server again (which skips the request that killed it), and counts it as lost.
            long lost = AbstractShell.getLostAnswerCount();
            assertEquals(0, module[0].getGlobalTokens().size());
            assertEquals(lost + 1, AbstractShell.getLostAnswerCount());

            //Given up on, but still counted.
            assertFalse(module[0].isInDirectGlobalTokens("x", null));
            assertEquals(lost + 2, AbstractShell.getLostAnswerCount());
            assertEquals("xx", FileUtils.getFileContents(marks));
        } finally {
            AbstractShell.putServerShell(nature, AbstractShell.getShellId(), null);
            restoreTestPythonPath(true);
            FileUtils.deleteDirectoryTree(dir);
        }
    }

    /**
     * A running process isn't taken to be dumping core.
     */
    public void testIsDumpingCoreOnLiveProcess() throws Exception {
        Process process = new ProcessBuilder("sleep", "30").start();
        try {
            assertFalse(new ProcessCreationInfo(new String[] { "sleep", "30" }, null, null, process).isDumpingCore());
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * A server that died between requests is restarted, its PYTHONPATH restored, and the request resent to it,
     * without losing its answer.
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

            long lost = AbstractShell.getLostAnswerCount();
            long failures = AbstractShell.getFailureCount();
            assertIsIn(shell.getImportCompletions("pydev_shell_test_exits_later", pythonpath).o2, "EXITS_LATER");
            Thread.sleep(3000);

            //Found only if the restarted server got the PYTHONPATH back.
            assertIsIn(shell.getImportCompletions("pydev_shell_test_plain", pythonpath).o2, "PLAIN");
            assertEquals(lost, AbstractShell.getLostAnswerCount());
            assertTrue(AbstractShell.getFailureCount() > failures);
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