/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

public class UnusedImportsTestAnalyzer extends CodeCompletionTestsBase { 

    public static void main(String[] args) {
        junit.textui.TestRunner.run(UnusedImportsTestAnalyzer.class);
    }



    private CompletionState state;
    private String sDoc;
    private Document doc;
    private IToken[] comps = null;
    private UnusedImportsAnalyzer analyzer;


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
        nature = new PythonNature();
        nature.setAstManager(new ASTManager());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
    }

    public void testCompletion(){
        doc = new Document("import testlib\n");
        analyzer = new UnusedImportsAnalyzer();
        IMessage[] msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(1, msgs.length);
        assertEquals(IMessage.WARNING, msgs[0].getType());
        assertEquals(IMessage.UNUSED_IMPORT, msgs[0].getSubType());

        doc = new Document("import testlib\nprint testlib");
        analyzer = new UnusedImportsAnalyzer();
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0));
        
        assertEquals(0, msgs.length);
    }

    
}
