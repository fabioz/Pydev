/*
 * Created on Apr 21, 2006
 */
package org.python.pydev.jythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import org.python.pydev.core.TestDependent;
import org.python.pydev.jython.JythonPlugin;

import junit.framework.TestCase;

public class JythonTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JythonTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        JythonPlugin.setDebugReload(false);
        JythonPlugin.IN_TESTS = true;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        JythonPlugin.setDebugReload(true);
    }
    
    public void testJythonTestsOnPydevPlugin() throws Exception {
        List<Throwable> errors = JythonPlugin.execAll(null, "test", JythonPlugin.newPythonInterpreter(false), 
                new File[]{new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/jysrc/tests")});
        if(errors.size() > 0){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
    }
    
    public void testJythonTestsOnDebugPlugin() throws Exception {
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("__name__", "__main__");
        List<Throwable> errors = JythonPlugin.execAll(locals, "test", JythonPlugin.newPythonInterpreter(false), 
                new File[]{new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC+"pysrc/tests")});
        if(errors.size() > 0){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
    }

}
