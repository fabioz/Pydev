/*
 * Created on Apr 21, 2006
 */
package org.python.pydev.jythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;

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
    
    
    public void testJythonTests() throws Exception {
        //unittest.TestCase format: the __main__ is required in the global namespace 
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("__name__", "__main__");
        IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(false);
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        interpreter.setErr(stdErr);
        interpreter.setOut(stdOut);
        
        List<Throwable> errors = JythonPlugin.execAll(locals, "test", interpreter, 
                new File[]{
//                    new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/tests"),
                    new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/jysrc/tests"),
                    new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC+"pysrc/tests"),
                },
                new File[]{
                    new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/"),
                    new File(TestDependent.JYTHON_ANT_JAR_LOCATION),
                    new File(TestDependent.JYTHON_JUNIT_JAR_LOCATION),
                });
        
        System.out.println(stdOut);
        System.out.println(stdErr);
        failIfErrorsRaised(errors, stdErr);
    }
    
    
    public void testJythonTestsOnSeparateProcess() throws Exception {
        //has to be run on a separate process because it'll call exit()
        List<Throwable> errors = JythonTest.execAll("test", 
                new File[]{
                    new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/tests"),
        });
        if(errors.size() > 0){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
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

    
    
    
    public static List<Throwable> execAll(final String startingWith, File[] beneathFolders){
        List<Throwable> errors = new ArrayList<Throwable>();
        for (File file : beneathFolders) {
            if(file != null){
                if(!file.exists()){
                    String msg = "The folder:"+file+" does not exist and therefore cannot be used to " +
                                                "find scripts to run starting with:"+startingWith;
                    Log.log(IStatus.ERROR, msg, null);
                    errors.add(new RuntimeException(msg));
                }
                File[] files = JythonPlugin.getFilesBeneathFolder(startingWith, file);
                if(files != null){
                    for(File f : files){
                        Throwable throwable = exec(f);
                        if(throwable != null){
                            errors.add(throwable);
                        }
                    }
                }
            }
        }
        return errors;
    }

    private static Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));
        
        String sep = SimpleRunner.getPythonPathSeparator();
        String pythonpath = TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/"+sep+
            TestDependent.JYTHON_ANT_JAR_LOCATION + sep+
            TestDependent.JYTHON_JUNIT_JAR_LOCATION;

        Tuple<String, String> output = new SimpleJythonRunner().runAndGetOutputWithJar(
                new File(TestDependent.JAVA_LOCATION), f.toString(), 
                TestDependent.JYTHON_JAR_LOCATION, null, f.getParentFile(), null, null,
                pythonpath);
        
        System.out.println(StringUtils.format("stdout:%s\nstderr:%s", output.o1, output.o2));
        
        if(output.o2.toLowerCase().indexOf("failed") != -1 || output.o2.toLowerCase().indexOf("traceback") != -1){
            throw new AssertionError(output.toString());
        }
        return null;
    }

}
