package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

/**
 * Tests tokens gotten from imports to see if they really exist there
 */
public class ImportsOcurrencesAnalyzerTest extends AnalysisTestsBase { 

    public static void main(String[] args) {
        try {
        	ImportsOcurrencesAnalyzerTest analyzer2 = new ImportsOcurrencesAnalyzerTest();
            analyzer2.setUp();
            analyzer2.testModuleTokens2();
            analyzer2.tearDown();
            System.out.println("finished");
            
            
            junit.textui.TestRunner.run(ImportsOcurrencesAnalyzerTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testModuleTokens() throws Exception {
    	doc = new Document(
    			"from testlib.unittest import anothertest\n"+
				"print anothertest.unexistant\n"+
				"\n"+
				"\n"
        	);
        	analyzer = new OcurrencesAnalyzer();
        	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
        	
        	printMessages(msgs,1);
	}
    
    public void testModuleTokens2() throws Exception {
    	doc = new Document(
    			"from testlib.unittest import anothertest\n"+
    			"print anothertest.AnotherTest.__init__\n"+
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs);
    	
    	printMessages(msgs,0);
    }

}
