/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathHelperTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonPathHelperTest.class);
    }

    public void testResolvePath(){
        PythonPathHelper helper = new PythonPathHelper();
        helper.setPythonPath("C:\\bin\\Python23\\lib| C:\\bin\\Python23\\lib\\site-packages| " +
        		"C:\\bin\\Python23\\lib\\site-packages\\Pythonwin|C:\\bin\\Python23\\lib\\plat-win");
        
        //note: this tests might run only in my computer... too much system dependence!!!
        assertEquals("unittest",helper.resolveModule("C:\\bin\\Python23\\lib\\unittest.py"));
        assertEquals("compiler.ast",helper.resolveModule("C:\\bin\\Python23\\lib\\compiler\\ast.py"));
//.pyc files are not supported, only source and compiled extensions.
//        assertEquals("compiler.ast",helper.resolveModule("C:\\bin\\Python23\\lib\\compiler\\ast.pyc"));
        
        //this happens because Pythonwin is not a module.
        assertEquals("email",helper.resolveModule("C:\\bin\\Python23\\lib\\email"));
        assertEquals("pywin",helper.resolveModule("C:\\bin\\Python23\\lib\\site-packages\\Pythonwin\\pywin"));
        assertEquals("pywin.debugger",helper.resolveModule("C:\\bin\\Python23\\lib\\site-packages\\Pythonwin\\pywin\\debugger"));
        assertEquals("pywin.debugger.fail",helper.resolveModule("C:\\bin\\Python23\\lib\\site-packages\\Pythonwin\\pywin\\debugger\\fail.py"));
        assertSame(null ,helper.resolveModule("C:\\bin\\Python23\\lib\\site-packages\\Pythonwin\\pywin\\debugger\\fail\\none.py"));
        assertSame(null ,helper.resolveModule("C:\\bin\\Python23\\lib\\site-packages\\Pythonwin\\pywin\\debugger\\fail\\none"));
        assertSame(null ,helper.resolveModule("C:\\bin\\Python23\\lib\\curses\\invalid"));
        assertSame(null ,helper.resolveModule("C:\\bin\\Python23\\lib\\invalid"));
    }
}
