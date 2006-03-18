/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;

public class PythonCompletionTestWithBuiltins extends CodeCompletionTestsBase{
    
    public static void main(String[] args) {
        try {
            PythonCompletionTestWithBuiltins builtins = new PythonCompletionTestWithBuiltins();
            builtins.setUp();
            builtins.testDeepNested8();
            builtins.tearDown();
            
            junit.textui.TestRunner.run(PythonCompletionTestWithBuiltins.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private static PythonShell shell;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES+"|"+TestDependent.PYTHON_WXPYTHON_PACKAGES, false);
        codeCompletion = new PyCodeCompletion();

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
                , "AnotherTest", "GUITest", "guitestcase", "main", "relative", "t", "TestCase", "testcase", "TestCaseAlias"
                });

    }

    
    
    public void testPreferForcedBuiltin() throws BadLocationException, IOException, Exception{
        if(TestDependent.HAS_MX_DATETIME){
            String s = ""+
            "from mx import DateTime\n"+
            "DateTime.";
            requestCompl(s, s.length(), -1, new String[]{"now()"});
        }
    }
    
    public void testDeepNested6() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.c1.f.";
        requestCompl(s, s.length(), -1, new String[] { "curdir"});
    }
    
    public void testDeepNested7() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.c1.f.curdir.";
        requestCompl(s, s.length(), -1, new String[] { "upper()"});
    }
    
    public void testDeepNested8() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.C1.f.altsep.";
        requestCompl(s, s.length(), -1, new String[] { "upper()"});
    }
    

    public void testPreferSrc() throws BadLocationException, IOException, Exception{
        String s = ""+
        "import prefersrc\n"+
        "prefersrc.";
        requestCompl(s, s.length(), -1, new String[]{"PreferSrc"});
    }
    
    public void testWxPython1() throws BadLocationException, IOException, Exception{
        if(TestDependent.HAS_WXPYTHON_INSTALLED){ //we can only test what we have
            String s = ""+
            "from wxPython.wx import *\n"+
            "import wx\n"+
            "class HelloWorld(wx.App):\n"+
            "   def OnInit(self):\n"+
            "       frame = wx.Frame(None,-1,\"hello world\")\n"+
            "       frame.Show(True)\n"+
            "       self.SetTopWindow(frame)\n"+
            "       b=wx.Button(frame,-1,\"Button\")\n"+
            "       return True\n"+
            "app = HelloWorld(0)\n"+
            "app.MainLoop()\n"+
            "app.";
            requestCompl(s, s.length(), -1, new String[]{"MainLoop()"});
        }
    }
    

    public void testCompleteImportBuiltinReference() throws BadLocationException, IOException, Exception{
        
        String s;

        if(TestDependent.HAS_WXPYTHON_INSTALLED){ //we can only test what we have
            s = "" +
            "from wxPython.wx import wxButton\n"+
            "                \n"+   
            "wxButton.";         
            requestCompl(s, s.length(), -1, new String[]{"Close()"});

            s = "" +
            "import wxPython\n"+
            "                \n"+   
            "wxPython.";         
            requestCompl(s, s.length(), -1, new String[]{"wx"});
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
        requestCompl(s, s.length(), 2, new String[]{"AnotherTest","t"});

    }
    


}
