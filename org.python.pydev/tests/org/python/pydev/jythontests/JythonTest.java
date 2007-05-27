/*
 * Created on Apr 21, 2006
 */
package org.python.pydev.jythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import org.python.pydev.core.TestDependent;
import org.python.pydev.jython.IPythonInterpreter;
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
        //asserts will make the test fail
        List<Throwable> errors = JythonPlugin.execAll(null, "test", JythonPlugin.newPythonInterpreter(false), 
                new File[]{new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/jysrc/tests")});
        failIfErrorsRaised(errors, null); //errors will always arrive through exceptions and not through the err stream
    }
    
    public void testJythonTestsOnDebugPlugin() throws Exception {
        //unittest.TestCase format: the __main__ is required in the global namespace 
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("__name__", "__main__");
        IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(false);
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        interpreter.setErr(stdErr);
        
        List<Throwable> errors = JythonPlugin.execAll(locals, "test", interpreter, 
                new File[]{new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC+"pysrc/tests")});
        failIfErrorsRaised(errors, stdErr);
    }

    private void failIfErrorsRaised(List<Throwable> errors, ByteArrayOutputStream stdErr) throws IOException {
        if(errors.size() > 0){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
        if(stdErr != null){
            String errStr = new String(stdErr.toByteArray());
            if(errStr.length() > 0){
                if(errStr.indexOf("FAILED") != -1){
                    fail(errStr);
                }
            }
        }
    }

}
