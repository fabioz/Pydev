/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.JythonShell;

public class JythonCompletionWithBuiltinsTest extends JythonCodeCompletionTestsBase{
    
    private static JythonShell shell;
    
    public static void main(String[] args) {
        try {
            JythonCompletionWithBuiltinsTest test = new JythonCompletionWithBuiltinsTest();
            test.setUp();
            test.testCompleteImportBuiltin2();
            test.tearDown();
            
            junit.textui.TestRunner.run(JythonCompletionWithBuiltinsTest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if(shell == null){
            shell = new JythonShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, shell);
    
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
        AbstractShell.putServerShell(nature, AbstractShell.COMPLETION_SHELL, null);
    }

    public void testCompleteImportBuiltin2() throws BadLocationException, IOException, Exception{
        
        String s;
        s = "from java.lang import Class\n"+
        "Class.";
        requestCompl(s, s.length(), -1, new String[] { "forName(string)" });
    }
    
    public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception{
        String s;
        s = "from java import ";
        requestCompl(s, s.length(), -1, new String[] { "lang", "math", "util" });
    }
    
    /**
     * Test related to: http://sourceforge.net/tracker/index.php?func=detail&aid=1560823&group_id=85796&atid=577329
     */
    public void testStaticAccess() throws BadLocationException, IOException, Exception{
        String s;
        s = "" +
            "from javax import swing \n" +
            "print swing.JFrame.";
        requestCompl(s, s.length(), -1, new String[] { "EXIT_ON_CLOSE" });
    }


}
