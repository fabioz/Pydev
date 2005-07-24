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

public class OcurrencesAnalyzerTest extends CodeCompletionTestsBase { 

    public static void main(String[] args) {
//        OcurrencesAnalyzerTest analyzer2 = new OcurrencesAnalyzerTest();
//        try {
//            analyzer2.setUp();
//            analyzer2.testSameName();
//            analyzer2.tearDown();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
        junit.textui.TestRunner.run(OcurrencesAnalyzerTest.class);
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

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        restorePythonPath(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
    }

    public void testUnusedImports(){
        doc = new Document("import testlib\n");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals("Unused import(s): testlib", msgs[0].getMessage());
        assertEquals(IMarker.SEVERITY_WARNING, msgs[0].getSeverity());
        assertEquals(IMessage.SUB_UNUSED_IMPORT, msgs[0].getSubType());

        doc = new Document("import testlib\nprint testlib");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(0, msgs.length);
        
        sDoc = "from testlib.unittest import *";
        doc = new Document(sDoc);
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals("Unused import(s): main, TestCase, AnotherTest, TestCaseAlias, GUITest, testcase", msgs[0].getMessage());

        sDoc = "from testlib.unittest import *\nprint TestCase";
        doc = new Document(sDoc);
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals("Unused import(s): main, AnotherTest, TestCaseAlias, GUITest, testcase", msgs[0].getMessage());
        
    
    }

    
    public void testUnusedVariable() {
        doc = new Document("a = 1");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals(IMessage.SUB_UNUSED_VARIABLE, msgs[0].getSubType());
        assertEquals("Unused variable: a", msgs[0].getMessage());
        
        doc = new Document("a = 1;print a");
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(0, msgs.length);
        
        //ignore the self
        doc = new Document(
"class Class1:         \n" +
"    def met1(self, a):\n" +
"        pass"
                );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals("Unused variable: a", msgs[0].getMessage());
        
    }

    public void testSameName() {
        //2 messages with token with same name
        doc = new Document(
            "a = 1\n"+
            "a = 2" );
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(2, msgs.length);
    }
    
    public void testOtherScopes() {
        //2 messages with token with same name
        doc = new Document(
"def eoueoau(  aeee  ): \n"+  
"    pass               \n"+
"def eoueoau(  afff  ): \n"+   
"    pass "             );                   
        analyzer = new OcurrencesAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(2, msgs.length);
    }
    
    /**
     * @param msgs
     */
    protected void printMessages(IMessage[] msgs) {
        for (int i = 0; i < msgs.length; i++) {
            System.out.println(msgs[i]);
        }
    }

    
}
