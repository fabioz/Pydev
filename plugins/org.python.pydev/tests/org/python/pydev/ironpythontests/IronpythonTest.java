package org.python.pydev.ironpythontests;

import java.io.File;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.pythontests.AbstractBasicRunTestCase;
import org.python.pydev.runners.SimpleIronpythonRunner;

public class IronpythonTest extends AbstractBasicRunTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IronpythonTest.class);
    }

    protected Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));
        Tuple<String, String> output = new SimpleIronpythonRunner().runAndGetOutput(new String[] {
                TestDependent.IRONPYTHON_EXE, "-u", IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS, REF.getFileAbsolutePath(f) }, f.getParentFile(), null, null);
        
        System.out.println(StringUtils.format("stdout:%s\nstderr:%s", output.o1, output.o2));
        
        if(output.o2.toLowerCase().indexOf("failed") != -1 || output.o2.toLowerCase().indexOf("traceback") != -1){
            throw new AssertionError(output.toString());
        }
        return null;
    }

    
    /**
     * Runs the python tests available in this plugin and in the debug plugin.
     */
    public void testPythonTests() throws Exception {
        execAllAndCheckErrors("test", new File[]{
                new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/tests"),
            }
        );
    }

}


