package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.TestDependent;
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
//            analyzer2.testUnresolvedImport();
            analyzer2.tearDown();
            System.out.println("finished");
            
            
            junit.textui.TestRunner.run(ImportsOcurrencesAnalyzerTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testModuleTokensErr() throws Exception {
    	doc = new Document(
			"from testlib.unittest import anothertest\n"+
			"print anothertest.unexistant\n"+
			"\n"+
			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,1);
    	assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
    	assertEquals(19, msgs[0].getStartCol(doc));
	}
    
    public void testModuleTokensErr2() throws Exception {
    	doc = new Document(
    			"from testlib.unittest import anothertest\n"+
    			"print anothertest.unexistant()\n"+
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,1);
    	assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
    	assertEquals(19, msgs[0].getStartCol(doc));
    }
    
    public void testModuleTokensErr3() throws Exception {
    	doc = new Document(
    			"from testlib.unittest import anothertest\n"+
    			"print anothertest.AnotherTest.unexistant()\n"+
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,1);
    	assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
    	assertEquals(31, msgs[0].getStartCol(doc));
    }
    
    public void testModuleTokens2() throws Exception {
    	doc = new Document(
    			"from testlib.unittest import anothertest\n"+
    			"print anothertest.AnotherTest.__init__\n"+
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,0);
    }

    public void testQtInit() throws Exception {
    	if(TestDependent.HAS_QT_INSTALLED){
	    	doc = new Document(
	    			"import qt\n"+
	    			"print qt.QWidget.__init__\n"+
	    			"\n"+
	    			"\n"
	    	);
	    	analyzer = new OcurrencesAnalyzer();
	    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
	    	
	    	printMessages(msgs,0);
    	}		
	}
    
    
    public void testTokenFromWildImport() throws Exception {
		doc = new Document(
				"from testlib.unittest.anothertest import *\n"+
				"AnotherTest.__init__\n"+
				"\n"+
				"\n"
		);
		analyzer = new OcurrencesAnalyzer();
		msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
		
		printMessages(msgs,1); //unused imports
    }
    
    
    public void testRedefinedToken() throws Exception {
    	doc = new Document(
			"from testlib.unittest import anothertest\n"+
			"anothertest = anothertest.AnotherTest()\n" +
			"print anothertest.__init__\n"+
			"\n"+
			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,0); 
    }
    
    public void testMethod() throws Exception {
    	doc = new Document(
			"from testlib.unittest import anothertest\n"+
			"print anothertest.AnotherTest().another.__class__\n" + //we should just get to the AnotherTest() part
			"\n"+
			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,0); 
    }
    
    public void testUnresolvedImport() throws Exception {
    	doc = new Document(
    			"from testlib import notexistant\n"+ //it is not resolved, 
    			"print notexistant.foo\n" +   //as we cannot resolve it, the foo will throw a warning
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,2); 
    }
    
    public void testSilencedUnresolvedImport() throws Exception {
    	doc = new Document(
    			"from testlib import notexistant #@UnresolvedImport\n"+ //it is not resolved, so, let's signal this
    			"print notexistant.foo\n" +   //after silencing the unresolved import, this should also be silenced
    			"\n"+
    			"\n"
    	);
    	analyzer = new OcurrencesAnalyzer();
    	msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), prefs, doc);
    	
    	printMessages(msgs,1); 
    }
    

}
