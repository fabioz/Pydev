/*
 * Created on 07/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.editor.codecompletion.revisited.InterpreterManagerStub;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.BundleInfoStub;

import junit.framework.TestCase;

public class SimpleJythonRunnerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimpleJythonRunnerTest.class);
    }

    private Preferences preferences;

    protected void setUp() throws Exception {
        super.setUp();
        BundleInfo.setBundleInfo(new BundleInfoStub());
        preferences = new Preferences();
        PydevPlugin.setPythonInterpreterManager(new InterpreterManagerStub(preferences));

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        BundleInfo.setBundleInfo(null);
    }

    public void testRun() throws CoreException, IOException {
        SimpleJythonRunner runner = new SimpleJythonRunner();
        File absoluteFile = BundleInfo.getBundleInfo().getRelativePath(new Path("interpreterInfo.py")).getAbsoluteFile();
        String string = runner.runAndGetOutput(absoluteFile.getCanonicalPath(), (String)null, null);
        assertNotNull(string);
        System.out.println(string);
    }
}
