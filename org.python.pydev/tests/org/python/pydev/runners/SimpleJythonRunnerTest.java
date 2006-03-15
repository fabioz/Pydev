/*
 * Created on 07/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.PythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCodeCompletionTestsBase;
import org.python.pydev.plugin.PydevPlugin;

public class SimpleJythonRunnerTest extends JythonCodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimpleJythonRunnerTest.class);
    }


    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setJythonInterpreterManager(new PythonInterpreterManagerStub(preferences));

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRun() throws CoreException, IOException {
        SimpleJythonRunner runner = new SimpleJythonRunner();
        File absoluteFile = PydevPlugin.getBundleInfo().getRelativePath(new Path("interpreterInfo.py")).getAbsoluteFile();
        String string = runner.runAndGetOutputWithJar(absoluteFile.getCanonicalPath(), TestDependent.JYTHON_JAR_LOCATION, null, null, null, new NullProgressMonitor()).o1;
//        String string = runner.runAndGetOutput(absoluteFile.getCanonicalPath(), (String)null, null);
        assertNotNull(string);
//        System.out.println(string);
    }
}
