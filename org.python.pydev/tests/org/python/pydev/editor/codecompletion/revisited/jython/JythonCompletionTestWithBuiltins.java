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

public class JythonCompletionTestWithBuiltins extends JythonCodeCompletionTestsBase{
    
    private static JythonShell shell;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion(false);

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

    public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception{
        
        String s;
        s = "from java import ";
        requestCompl(s, s.length(), -1, new String[] { "lang", "math", "util" });

    }


}
