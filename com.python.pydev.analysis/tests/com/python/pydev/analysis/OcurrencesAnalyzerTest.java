/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.messages.IMessage;
import static com.python.pydev.analysis.IAnalysisPreferences.*;

public class OcurrencesAnalyzerTest extends CodeCompletionTestsBase { 

    public static void main(String[] args) {
        try {
            OcurrencesAnalyzerTest analyzer2 = new OcurrencesAnalyzerTest();
            analyzer2.setUp();
            analyzer2.testGlobal2();
            analyzer2.tearDown();
            System.out.println("finished");
            
            
            junit.textui.TestRunner.run(OcurrencesAnalyzerTest.class);
            
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }



    private String sDoc;
    private Document doc;
    private OcurrencesAnalyzer analyzer;
    private IMessage[] msgs;


    /**
     * @return Returns the manager.
     */
    protected ICodeCompletionASTManager getManager() {
        return (ICodeCompletionASTManager) nature.getAstManager();
    }
    private int severityForUnusedImport;
    private int severityForUnusedVariable;
    private int severityForUndefinedVariable;
    private int severityForDuplicatedSignature;
    private int severityForReimport;

    private IAnalysisPreferences prefs;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        restorePythonPath(false);
        prefs = createAnalysisPrefs();
        severityForUnusedImport = IMarker.SEVERITY_WARNING;
        severityForUnusedVariable = IMarker.SEVERITY_WARNING;
        severityForUndefinedVariable = IMarker.SEVERITY_ERROR;
        severityForDuplicatedSignature = IMarker.SEVERITY_ERROR;
        severityForReimport = IMarker.SEVERITY_WARNING;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
    }


    public void testUnusedImports(){
            
        severityForUnusedImport = IMarker.SEVERITY_ERROR;
        doc = new Document("import testlib\n");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals("Unused import: testlib", msgs[0].getMessage());
        assertEquals(IMarker.SEVERITY_ERROR, msgs[0].getSeverity());
        assertEquals(TYPE_UNUSED_IMPORT, msgs[0].getType());

        //-----------------
        doc = new Document("import testlib\nprint testlib");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
        
        //-----------------
        sDoc = "from testlib.unittest import *";
        doc = new Document(sDoc);
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals(IMarker.SEVERITY_ERROR, msgs[0].getSeverity());
        assertEquals("Unused import: main, TestCase, AnotherTest, TestCaseAlias, GUITest, testcase", msgs[0].getMessage());

        
        //-----------------
        severityForUnusedImport = IMarker.SEVERITY_WARNING;
        sDoc = "from testlib.unittest import *\nprint TestCase";
        doc = new Document(sDoc);
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals(IMarker.SEVERITY_WARNING, msgs[0].getSeverity());
        assertEquals("Unused import: main, AnotherTest, TestCaseAlias, GUITest, testcase", msgs[0].getMessage());
        
        //-----------------
        severityForUnusedImport = SEVERITY_IGNORE;
        sDoc = "from testlib.unittest import *\nprint TestCase";
        doc = new Document(sDoc);
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);//ignored
        
    
    }

    public void testUnusedImports2(){
        
        doc = new Document(
            "from simpleimport import *\n" +
            "print xml"
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: xml.dom.domreg, xml.dom", msgs);
        assertEquals(1, msgs[0].getStartLine(doc));
    }
 
    public void testUnusedImports3(){
        
        doc = new Document(
            "import os.path as otherthing\n" +
            ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: otherthing", msgs);
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(-1, msgs[0].getEndCol(doc));
    }
    
    public void testUnusedImports4(){
        
        doc = new Document(
                "def m():\n" +
                "    import os.path as otherthing\n" +
                ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: otherthing", msgs);
        assertEquals(5, msgs[0].getStartCol(doc));
        assertEquals(-1, msgs[0].getEndCol(doc));
    }
    
    public void testReimport4(){
        
        doc = new Document(
            "from testlib.unittest.relative import toImport\n" +
            "from testlib.unittest.relative import toImport\n" +
            ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
        assertContainsMsg("Unused import: toImport", msgs);
        assertContainsMsg("Import redefinition: toImport", msgs);
    }
    
    public void testReimport(){
        
        doc = new Document(
            "import os \n"+
            "import os \n"+
            "print os  \n"+
            ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Import redefinition: os", msgs);
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(2, msgs[0].getStartLine(doc));
    }
    
    public void testReimport2(){
        
        doc = new Document(
                "import os \n"+
                "import os \n"+
                ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
        assertContainsMsg("Import redefinition: os", msgs);
        assertContainsMsg("Unused import: os", msgs);
    }
    
    public void testReimport3(){
        
        doc = new Document(
            "import os      \n"+
            "def m1():      \n"+
            "    import os  \n"+
            "    print os   \n"+
            "\n"+
            ""
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Import redefinition: os", msgs, 3);
        assertContainsMsg("Unused import: os", msgs, 1);
    }
    
    /**
     * @return
     */
    private IAnalysisPreferences createAnalysisPrefs() {
        return new IAnalysisPreferences(){

            public int getSeverityForType(int type) {
                if (type == TYPE_UNUSED_IMPORT){
                    return severityForUnusedImport;
                }
                if (type == TYPE_UNUSED_VARIABLE){
                    return severityForUnusedVariable;
                }
                if (type == TYPE_UNDEFINED_VARIABLE){
                    return severityForUndefinedVariable;
                }
                if (type == TYPE_DUPLICATED_SIGNATURE){
                    return severityForDuplicatedSignature;
                }
                if (type == TYPE_REIMPORT){
                    return severityForReimport;
                }
                throw new RuntimeException("unable to get severity for type "+type);
            }

            public boolean makeCodeAnalysis() {
                return true;
            }};
    }

    
    public void testUnusedVariable() {
        doc = new Document(
                "def m1():    \n" +
                "    a = 1      ");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNUSED_VARIABLE, msgs[0].getType());
        assertEquals("Unused variable: a", msgs[0].getMessage());
        
        doc = new Document("a = 1;print a");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
        
        //ignore the self
        doc = new Document(
            "class Class1:         \n" +
            "    def met1(self, a):\n" +
            "        pass"
                );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals("Unused variable: a", msgs[0].getMessage());
        
    }
    
    public void test2UnusedVariables() {
        doc = new Document(
                "def m1():          \n"+  
                "    result = 10    \n"+       
                "                   \n"+      
                "    if False:      \n"+       
                "        result = 20\n"+        
                "                   \n"     
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
    }
    
    public void testNotUnusedVariable() {
        doc = new Document(
            "def m1():          \n"+  
            "    result = 10    \n"+       
            "                   \n"+      
            "    if False:      \n"+       
            "        result = 20\n"+        
            "                   \n"+     
            "    print result   \n"      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
        
    }
    
    public void testNotUnusedVariable4() {
        doc = new Document(
            "def m1():             \n"+  
            "    result = 10       \n"+       
            "                      \n"+
            "    while result > 0: \n"+
            "        result = 0    \n"+      
            "                      \n"+     
            ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
        
    }
    
    public void testNotUnusedVariable5() {
        doc = new Document(
            "def m():         \n"+  
            "    try:         \n"+       
            "        c = 'a'  \n"+
            "    except:      \n"+
            "        c = 'b'  \n"+      
            "    print c      \n"+     
            ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
        
    }
    
    public void testNotUnusedVariable6() {
        doc = new Document(
                "def m():         \n"+  
                "    try:         \n"+       
                "        c = 'a'  \n"+
                "    finally:     \n"+
                "        c = 'b'  \n"+      
                "    print c      \n"+     
                ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
        
    }
    
    public void testUnusedVariable8() {
        doc = new Document(
        "def outer(show=True):     \n"+  
        "    def inner(show):      \n"+       
        "        print show        \n"+
        ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused variable: show", msgs, 1);
    }
    
    public void testUnusedVariable9() {
        doc = new Document(
                "def outer(show=True):        \n"+  
                "    def inner(show=show):    \n"+       
                "        pass                 \n"+
                ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused variable: show", msgs, 2);
    }
    
    public void testUnusedVariable10() {
        doc = new Document(
                "def outer(show):        \n"+  
                "    def inner(show):    \n"+       
                "        pass            \n"+
                ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Unused variable: show", msgs, 1);
        assertContainsMsg("Unused variable: show", msgs, 2);
    }
    
    public void testUnusedVariable6() {
        doc = new Document(
                "def m():         \n"+  
                "    try:         \n"+       
                "        c = 'a'  \n"+
                "    finally:     \n"+
                "        c = 'b'  \n"+      
                ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,2 );
        assertEquals(2, msgs.length);
        
    }
    
    public void testUnusedVariable7() {
        doc = new Document(
            "def m( a, b ):       \n"+  
            "    def m1( a, b ):  \n"+       
            "        print a, b   \n"+
            "    if a:            \n"+
            "        print 'ok'   \n"+      
            ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,1 );
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused variable: b", msgs, 1);
        
    }
    
    public void testNotUnusedVariable2() {
        doc = new Document(
            "def GetValue( option ):         \n"+  
            "    return int( option ).Value()\n"+       
            ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
        
    }
    
    public void testNotUnusedVariable3() {
        doc = new Document(
            "def val(i):    \n"+  
            "    i = i + 1  \n"+       
            "    print i    \n"+       
            ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testUndefinedVar() {
        doc = new Document(
                "def GetValue():         \n"+  
                "    return int( option ).Value()\n"+       
                ""      
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Undefined variable: option", msgs);
    }
    
    
    
    public void testScopes() {
        //2 messages with token with same name
        doc = new Document(
            "def m1():       \n"+   
            "    def m2():   \n"+     
            "        print a \n"+      
            "    a = 10        "   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testScopes2() {
        doc = new Document(
            "class Class1:              \n"+  
            "    c = 1                  \n"+      
            ""   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testScopes3() {
        doc = new Document(
                "class Class1:              \n"+  
                "    def __init__( self ):  \n"+
                "        print Class1       \n"+
                ""   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testScopes4() {
        doc = new Document(
                "def rec():           \n"+
                "    def rec2():      \n"+
                "        return rec2  \n"+
                ""   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testScopes5() {
        doc = new Document(
                "class C:       \n"+
                "    class I:   \n"+
                "        print I\n"+
                ""   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testScopes6() {
        doc = new Document(
            "def ok():          \n"+
            "    print col      \n"+
            "def rowNotEmpty(): \n"+
            "    col = 1        \n"+
            ""   
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 2);
        assertContainsMsg("Undefined variable: col", msgs, 2);
        assertContainsMsg("Unused variable: col", msgs, 4);
    }
    
    public void testSameName() {
        //2 messages with token with same name
        doc = new Document(
        "def m1():\n"+
        "    a = 1\n"+
        "    a = 2" );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
    }
    
    public void testVarArgs() {
        doc = new Document(
                "def m1(*args): \n"+
                "    print args   " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
    }
    
    public void testVarArgsNotUsed() {
        doc = new Document(
                "\n" +
                "def m1(*args): \n"+
                "    pass         " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused variable: args", msgs, 2);
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(-1, msgs[0].getEndCol(doc));
    }
    
    public void testKwArgs() {
        doc = new Document(
            "def m1(**kwargs): \n"+
            "    print kwargs    " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testKwArgs2() {
        doc = new Document(
                "def m3():             \n" +
                "    def m1(**kwargs): \n"+
                "        pass            " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused variable: kwargs", msgs, 2);
        assertEquals(5, msgs[0].getStartCol(doc));
    }
    

    public void testOtherScopes() {
        //2 messages with token with same name
        doc = new Document(
            "def m1(  aeee  ): \n"+  
            "    pass               \n"+
            "def m2(  afff  ): \n"+   
            "    pass "             );                   
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
    }
    
    
    public void testUndefinedVariable() {
        //2 messages with token with same name
        doc = new Document(
            "print a" 
            );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNDEFINED_VARIABLE, msgs[0].getType());
        assertEquals("Undefined variable: a", msgs[0].getMessage());
    }
    
    public void testUndefinedVariable2() {
        doc = new Document(
            "a = 10      \n"+  //global scope - does not give msg
            "def m1():   \n"+ 
            "    a = 20  \n"+ 
            "    print a \n"+ 
            "\n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testUndefinedVariable3() {
        doc = new Document(
                "a = 10      \n"+ //global scope - does not give msg
                "def m1():   \n"+ 
                "    a = 20  \n"+ 
                "\n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,1);
        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNUSED_VARIABLE, msgs[0].getType());
        assertEquals("Unused variable: a", msgs[0].getMessage());
        assertEquals(3, msgs[0].getStartLine(doc));
    }
    
    public void testOk() {
        //all ok...
        doc = new Document(
            "import os   \n" +
            "            \n" +
            "def m1():   \n" +
            "    print os\n" +
            "            \n" +
            "print m1    \n"  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testImportAfter() {
        doc = new Document(
                "def met():          \n" +
                "    print os.path   \n" +
                "import os.path      \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testImportAfter2() {
        doc = new Document(
                "def met():          \n" +
                "    print os.path   \n" +
                "import os           \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testImportPartial() {
        doc = new Document(
                "import os.path   \n" +
                "print os         \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: os.path", msgs);
    }
    
    public void testImportAs() {
        doc = new Document(
                "import os.path as bla   \n" +
                "print bla               \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testImportAs2() {
        doc = new Document(
                "import os.path as bla   \n" +
                "print os.path           \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs,2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Undefined variable: os", msgs);
        assertContainsMsg("Unused import: bla", msgs);
    }
    
    private void assertNotContainsMsg(String msg, IMessage[] msgs2) {
        if(containsMsg(msg, msgs2)){
            fail("The message "+msg+" was found within the messages (it should not have been found).");
        }
    }
    private void assertContainsMsg(String msg, IMessage[] msgs2) {
        assertContainsMsg(msg, msgs2, -1);
    }

    private void assertContainsMsg(String msg, IMessage[] msgs2, int line) {
        boolean found = containsMsg(msg, msgs2, line);
        
        if(found){
            return;
        }
        
        StringBuffer msgsAvailable = new StringBuffer();
        for (IMessage message : msgs2) {
            msgsAvailable.append(message.getMessage());
            msgsAvailable.append("\n");
        }
        fail(String.format("No message named %s could be found. Available: %s", msg, msgsAvailable));
    }

    /**
     * Checks if a specific message is contained within the messages passed
     */
    private boolean containsMsg(String msg, IMessage[] msgs2) {
        return containsMsg(msg, msgs2, -1);
    }
    
    /**
     * Checks if a specific message is contained within the messages passed
     */
    private boolean containsMsg(String msg, IMessage[] msgs2, int line) {
        boolean foundMsg = false;
        boolean foundMsgInLine = false;
        for (IMessage message : msgs2) {
            if(message.getMessage().equals(msg)){
                foundMsg = true;
                if(line != -1 && foundMsgInLine == false){
                    foundMsgInLine = line == message.getStartLine(doc);
                }
            }
        }
        
        if(foundMsg && line != -1){
            assertTrue("The message :"+msg+" was not found in the specified line ("+line+")",foundMsgInLine);
        }
        return foundMsg;
    }

    public void testImportAs3() {
        doc = new Document(
                "import os.path as bla   \n" +
                "print os                \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
        assertContainsMsg("Undefined variable: os", msgs);
        assertContainsMsg("Unused import: bla", msgs);
    }
    

    public void testAttributeImport() {
        //all ok...
        doc = new Document(
            "import os.path      \n" +
            "print os.path       \n" +
            ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testGlobal() {
        //no need to warn if global variable is unused (and it should be defined at the global declaration)
        doc = new Document(
                "def m():                         \n" +
                "    global __progress            \n" +
                "    __progress = __progress + 1  \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testGlobal2() {
        //no need to warn if global variable is unused (and it should be defined at the global declaration)
        doc = new Document(
                "def m():                         \n" +
                "    global __progress            \n" +
                "    __progress = 1               \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeImportAccess() {
        //all ok...
        doc = new Document(
                "import os           \n" +
                "print os.path       \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeAccessMsg() {
        //all ok...
        doc = new Document(
                "s.a = 10            \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: s", msgs[0].getMessage());
    }
    
    public void testAttributeAccess() {
        //all ok...
        doc = new Document(
                "def m1():               \n" +
                "    class Stub():pass   \n" +
                "    s = Stub()          \n" +
                "    s.a = 10            \n" +
                "    s.b = s.a           \n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeAccess2() {
        //all ok...
        doc = new Document(
            "class TestCase:                                    \n" +
            "    def test(self):                                \n" +
            "        db = 10                                    \n" +
            "        comp = db.select(1)                        \n" +
            "        aa.bbb.cccc[comp.id].hasSimulate = True    \n" +
            "                                                   \n" +
            ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        //comp should be used
        //aa undefined (check pos)
        assertNotContainsMsg("Unused variable: comp", msgs);
        assertContainsMsg("Undefined variable: aa", msgs, 5);
        assertEquals(1, msgs.length);
        assertEquals(9, msgs[0].getStartCol(doc));
    }
    
    public void testAttributeErrorPos() {
        //all ok...
        doc = new Document(
            "print message().bla\n" +
            ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: message", msgs[0].getMessage());
        assertEquals(7, msgs[0].getStartCol(doc));
        assertEquals(14, msgs[0].getEndCol(doc));
    }
    
    public void testAttributeErrorPos2() {
        //all ok...
        doc = new Document(
                "lambda x: os.rmdir( x )\n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: os", msgs[0].getMessage());
        assertEquals(11, msgs[0].getStartCol(doc));
        assertEquals(13, msgs[0].getEndCol(doc));
    }
    
    public void testAttributeErrorPos3() {
        //all ok...
        doc = new Document(
                "os.rmdir( '' )\n" +
                ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: os", msgs[0].getMessage());
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(3, msgs[0].getEndCol(doc));
    }

    
    private void printMessages(IMessage[] msgs, int i) {
        if(msgs.length != i){
            printMessages(msgs);
        }
    }

    public void testImportAttr() {
        //all ok...
        doc = new Document(
            "import os.path                 \n" +
            "if os.path.isfile( '' ):pass   \n" +
            ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    
    public void testSelf() {
        //all ok...
        doc = new Document(
            "class C:            \n" +
            "    def m1(self):   \n" +
            "        print self  \n" +
            ""  
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testDefinitionLater() {
        doc = new Document(
            "def m1():     \n" +
            "    print m2()\n" +
            "              \n" +
            "def m2():     \n" +
            "    pass      \n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testDefinitionLater2() {
        doc = new Document(
            "def m():                \n" +
            "    AroundContext().m1()\n" +
            "                        \n" +
            "class AroundContext:    \n" +
            "    def m1(self):       \n" +
            "        pass            \n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
 
    }
    
    public void testNotDefinedLater() {
        doc = new Document(
                "def m1():     \n" +
                "    print m2()\n" +
                "              \n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
    }
    
    public void testNotDefinedLater2() {
        doc = new Document(
            "def m1():     \n" +
            "    print c   \n" +
            "    c = 10    \n" +
            "              \n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(2, msgs.length);
    }
    
    public void testUndefinedVariableBuiltin() {
        doc = new Document(
                "print False" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
    }
    
    public void testUndefinedVariableBuiltin2() {
        doc = new Document(
            "print __file__" //source folder always has the builtin __file__
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testUndefinedVariableBuiltin3() {
        doc = new Document(
                "print [].__str__" //[] is a builtin 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testSelfAttribute() {
        doc = new Document(
            "class C:                          \n" +
            "    def m2(self):                 \n" +
            "        self.m1 = ''              \n" +
            "        print self.m1.join('a').join('b')   \n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testBuiltinAcess() {
        doc = new Document(
                "print file.read" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
    }
    
    public void testDictAcess() {
        doc = new Document(
            "def m1():\n" +
            "    k = {}                   \n" +
            "    print k[0].append(10)   " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttribute1() {
        doc = new Document(
            "def m1():\n" +
            "    file( 10, 'r' ).read()" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeFloat() {
        doc = new Document(
                "def m1():\n" +
                "    v = 1.0.__class__\n" +
                "    print v            " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeString() {
        doc = new Document(
                "def m1():\n" +
                "    v = 'r'.join('a')\n" +
                "    print v            " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testAttributeString2() {
        doc = new Document(
                "def m1():\n" +
                "    v = 'r.a.s.b'.join('a')\n" +
                "    print v            " 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testListComprehension() {
        doc = new Document(
                "def m1():\n" +
                "    print [i for i in range(10)]" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testTupleVar() {
        doc = new Document(
            "def m1():\n" +
            "    print (0,0).__class__" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testTupleVar2() {
        doc = new Document(
            "def m1():\n" +
            "    print (10 / 10).__class__" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        printMessages(msgs);
        assertEquals(0, msgs.length);
    }
    
    public void testDuplicatedSignature() {
        //2 messages with token with same name
        doc = new Document(
            "class C:             \n" +
            "    def m1(self):pass\n" +
            "    def m1(self):pass\n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(1, msgs.length);
        assertEquals(TYPE_DUPLICATED_SIGNATURE, msgs[0].getType());
        assertEquals("Duplicated signature: m1", msgs[0].getMessage());
        assertEquals(9, msgs[0].getStartCol(doc));

        //ignore
        severityForDuplicatedSignature = IAnalysisPreferences.SEVERITY_IGNORE;
        doc = new Document(
                "class C:             \n" +
                "    def m1(self):pass\n" +
                "    def m1(self):pass\n" 
        );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        
        assertEquals(0, msgs.length);
        
    }
    
    /**
     * @param msgs
     */
    protected void printMessages(IMessage ... msgs) {
        for (int i = 0; i < msgs.length; i++) {
            System.out.println(msgs[i]);
        }
    }

    
}
