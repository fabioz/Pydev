/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.ui.BundleInfoStub;

/**
 * @author Fabio Zadrozny
 */
public class SimplePythonRunnerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimplePythonRunnerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        BundleInfo.setBundleInfo(new BundleInfoStub());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws CoreException
     * @throws IOException
     * 
     */
    public void testEnv() throws CoreException, IOException {
        
        File relativePath = BundleInfo.getBundleInfo().getRelativePath(new Path("PySrc/interpreterInfo.py"));
        String string = SimplePythonRunner.runAndGetOutput(CodeCompletionTestsBase.PYTHON_EXE+" "+relativePath.getCanonicalPath(), null);
        System.out.println(string);
    }
}
