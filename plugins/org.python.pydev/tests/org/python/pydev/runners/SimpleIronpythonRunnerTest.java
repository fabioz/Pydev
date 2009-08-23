package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.IronpythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCodeCompletionTestsBase;
import org.python.pydev.plugin.PydevPlugin;

public class SimpleIronpythonRunnerTest extends JythonCodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimpleIronpythonRunnerTest.class);
    }


    public void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setIronpythonInterpreterManager(new IronpythonInterpreterManagerStub(getPreferences()));

    }


    public void testRun() throws CoreException, IOException {
        SimpleIronpythonRunner runner = new SimpleIronpythonRunner();
        File absoluteFile = PydevPlugin.getBundleInfo().getRelativePath(new Path("interpreterInfo.py")).getAbsoluteFile();
        String string = runner.runAndGetOutputWithInterpreter(TestDependent.IRONPYTHON_EXE, absoluteFile.getCanonicalPath(), null, null, null, new NullProgressMonitor()).o1;
//        String string = runner.runAndGetOutput(absoluteFile.getCanonicalPath(), (String)null, null);
        assertNotNull(string);
//        System.out.println(string);
    }
}
