/*
 * Created on Jun 25, 2006
 * @author Fabio
 */
package org.python.pydev.runners;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * Extends CodeCompletionTestsBase so that we have the bundle set for getting the environment.
 */
public class SimpleExeRunnerTest extends CodeCompletionTestsBase{

    
    public void testIt() throws Exception {
        if(TestDependent.HAS_CYGWIN){
            SimpleExeRunner runner = new SimpleExeRunner();
            Tuple<String, String> tup = runner.runAndGetOutput(TestDependent.CYGWIN_CYGPATH_LOCATION, new String[]{TestDependent.CYGWIN_CYGPATH_LOCATION}, null);
            assertEquals(TestDependent.CYGWIN_UNIX_CYGPATH_LOCATION, tup.o1.trim());
            assertEquals("", tup.o2);
        }
    }
    
    public void testIt2() throws Exception {
    	if(TestDependent.HAS_CYGWIN){
	        SimpleExeRunner runner = new SimpleExeRunner();
	        List<String> ret = runner.convertToCygwinPath(TestDependent.CYGWIN_CYGPATH_LOCATION, TestDependent.CYGWIN_CYGPATH_LOCATION, "c:\\foo");
	        assertEquals(2, ret.size());
	        ArrayList<String> expected = new ArrayList<String>();
	        expected.add(TestDependent.CYGWIN_UNIX_CYGPATH_LOCATION);
	        expected.add("/cygdrive/c/foo");
	        assertEquals(expected, ret);
    	}
    }

}
