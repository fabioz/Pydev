package org.python.pydev.pythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.runners.SimplePythonRunner;

public class PythonTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
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
        String cmdLine = SimplePythonRunner.getCommandLineAsString(new String[]{TestDependent.PYTHON_EXE, "-u", REF.getFileAbsolutePath(f)});
        Tuple<String, String> output = new SimplePythonRunner().runAndGetOutput(cmdLine, f.getParentFile());
        if(output.o2.indexOf("FAILED") != -1){
            throw new AssertionError(output.toString());
        }
        return null;
    }

    
    public void testPythonTestsOnDebugPlugin() throws Exception {
        List<Throwable> errors = execAll("test", new File[]{
                new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC+"pysrc/tests"),
                new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC+"pysrc/tests_python"),
            }
        );
        if(errors.size() > 0){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in python.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
    }
}


