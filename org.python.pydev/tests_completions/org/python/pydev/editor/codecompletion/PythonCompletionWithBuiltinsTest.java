/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;

public class PythonCompletionWithBuiltinsTest extends CodeCompletionTestsBase{
    
    public static void main(String[] args) {
        try {
            PythonCompletionWithBuiltinsTest builtins = new PythonCompletionWithBuiltinsTest();
            builtins.setUp();
            builtins.testBuiltinsInNamespace2();
            builtins.tearDown();
            
            junit.textui.TestRunner.run(PythonCompletionWithBuiltinsTest.class);

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
        
        ADD_MX_TO_FORCED_BUILTINS = false;

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES+"|"+
        		TestDependent.PYTHON_WXPYTHON_PACKAGES+"|"+TestDependent.PYTHON_MX_PACKAGES, false);
        
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
    
	public void testRecursion() throws FileNotFoundException, CoreException, BadLocationException, CompletionRecursionException{
		String file = TestDependent.TEST_PYSRC_LOC+"testrec3/rec.py";
		String strDoc = "RuntimeError.";
		File f = new File(file);
		try{
			nature.getAstManager().getCompletionsForToken(f, new Document(REF.getFileContents(f)), CompletionStateFactory.getEmptyCompletionState("RuntimeError", nature));
		}catch(CompletionRecursionException e){
			//that's ok... we're asking for it here...
		}
		requestCompl(f, strDoc, strDoc.length(), -1, new String[]{"__doc__", "__getitem__()", "__init__()", "__str__()"});   
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

    
    
    public void testBuiltinsInNamespace() throws BadLocationException, IOException, Exception{
        String s = "__builtins__.";
        requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});
    }
    
    public void testBuiltinsInNamespace2() throws BadLocationException, IOException, Exception{
        String s = "__builtins__.RuntimeError.";
        requestCompl(s, s.length(), -1, new String[]{"__doc__", "__getitem__()", "__init__()", "__module__", "__str__()"});
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
    
    public void testDeepNested10() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "from extendable.nested3 import hub2\n"+
        "hub2.c.a.";
        requestCompl(s, s.length(), -1, new String[] { "fun()"});
    }
    
    public void testRelativeOnSameProj() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "import prefersrc\n" +
        "prefersrc.";
        AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "foo";
        try {
			requestCompl(s, s.length(), -1, new String[] { "OkGotHere" }, nature2);
		} finally {
			AbstractModule.MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";
		}
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
    
    public void testDeepNested9() throws CoreException, BadLocationException{
        String s;
        s = "" +
        "from extendable.nested2 import hub\n"+
        "hub.C1.f.inexistant.";
        requestCompl(s, s.length(), -1, new String[] { });
    }
    
    public void testDictAssign() throws CoreException, BadLocationException{
    	String s;
    	s = "" +
    	"a = {}\n"+
    	"a.";
    	requestCompl(s, s.length(), -1, new String[] { "keys()" });
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
    

    public void testCompleteImportBuiltinReference2() throws BadLocationException, IOException, Exception{
    	String s;
    	if(TestDependent.HAS_WXPYTHON_INSTALLED){ //we can only test what we have
    		s = "" +
    		"from wx import ";
    		requestCompl(s, s.length(), -1, new String[]{"glcanvas"});
    	}
    }
    
    public void testGlu() throws IOException, CoreException, BadLocationException {
    	if(TestDependent.HAS_GLU_INSTALLED){
    	    final String s = "from OpenGL import ";
			requestCompl(s, s.length(), -1, new String[]{"GLU", "GLUT"});
    	}
    }
    
    public void testGlu2() throws IOException, CoreException, BadLocationException {
    	if(TestDependent.HAS_GLU_INSTALLED){
    		final String s = "from OpenGL.GL import ";
    		requestCompl(s, s.length(), -1, new String[]{"glPushMatrix"});
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
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/pysrc/simpleosimport.py");
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
