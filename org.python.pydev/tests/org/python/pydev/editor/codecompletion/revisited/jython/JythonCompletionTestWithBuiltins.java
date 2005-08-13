/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PythonShellTest;
import org.python.pydev.editor.codecompletion.revisited.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;

public class JythonCompletionTestWithBuiltins extends JythonCodeCompletionTestsBase{
    
    private static PythonShell shell;
    
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
            shell = PythonShellTest.startShell();
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
        s = "from datetime import datetime, date, MINYEAR,";
        requestCompl(s, s.length(), -1, new String[] { "date", "datetime", "MINYEAR", "MAXYEAR", "timedelta" });
        
        s = "from datetime.datetime import ";
        requestCompl(s, s.length(), -1, new String[] { "today", "now", "utcnow" });

    
    
        // Problem here is that we do not evaluate correctly if
        // met( ddd,
        //      fff,
        //      ccc )
        //so, for now the test just checks that we do not get in any sort of
        //look... 
        s = "" +
        
        "class bla(object):pass\n" +
        "\n"+
        "def newFunc(): \n"+
        "    callSomething( bla.__get#complete here... stack error \n"+
        "                  keepGoing) \n";

        //If we improve the parser to get the error above, uncomment line below to check it...
        //requestCompl(s, s.indexOf('#'), 1, new String[]{"__getattribute__"});
        requestCompl(s, s.indexOf('#'), 0, new String[]{});


        //check for builtins..1
        s = "" +
        "\n" +
        "";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

        //check for builtins..2
        s = "" +
        "from testlib import *\n" +
        "\n" +
        "";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

        //check for builtins..3 (builtins should not be available because it is an import request for completions)
        requestCompl("from testlib.unittest import  ", new String[]{"__init__", "anothertest"
                , "AnotherTest", "GUITest", "guitestcase", "main", "relative", "TestCase", "testcase", "TestCaseAlias"
                });

    }

    
    
    

    public void testCompleteImportBuiltinReference() throws BadLocationException, IOException, Exception{
        
        String s;

        if(TestDependent.HAS_WXPYTHON_INSTALLED){ //we can only test what we have
            s = "" +
            "import wxPython\n"+
            "                \n"+   
            "wxPython.";         
            requestCompl(s, s.length(), -1, new String[]{});
        }

        s = "" +
        "import os\n"+
        "                \n"+   
        "os.";         
        File file = new File("tests/pysrc/simpleosimport.py");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        requestCompl(file, s, s.length(), -1, new String[]{"path"});
        
        s = "" +
        "import os\n"+
        "                \n"+   
        "os.";         
        requestCompl(s, s.length(), -1, new String[]{"path"});
        
        if(TestDependent.HAS_QT_INSTALLED){ //we can only test what we have
            //check for builtins with reference..3
            s = "" +
            "from qt import *\n"+
            "                \n"+   
            "q = QLabel()    \n"+     
            "q.";         
            requestCompl(s, s.length(), -1, new String[]{"AlignAuto"});
        }

        //check for builtins with reference..3
        s = "" +
        "from testlib.unittest import anothertest\n"+
        "anothertest.";         
        requestCompl(s, s.length(), 2, new String[]{"AnotherTest","testcase"});

    }
    


}
