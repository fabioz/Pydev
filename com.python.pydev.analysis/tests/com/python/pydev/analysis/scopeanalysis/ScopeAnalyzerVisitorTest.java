/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.AnalysisTestsBase;
import com.python.pydev.analysis.messages.AbstractMessage;
import com.python.pydev.analysis.visitors.Found;

public class ScopeAnalyzerVisitorTest extends AnalysisTestsBase {

    public static void main(String[] args) {
    	try {
			ScopeAnalyzerVisitorTest test = new ScopeAnalyzerVisitorTest();
			test.setUp();
			test.testIt3();
			test.tearDown();
			junit.textui.TestRunner.run(ScopeAnalyzerVisitorTest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testIt2() throws Exception {
    	doc = new Document(
    			"import os\n"+
    			"print os\n"+
    			"\n"
    	);
    	int line=0;
    	int col=8;
    	List<IToken> tokenOccurrences = getTokenOccurrences(line, col);
    	assertEquals(2, tokenOccurrences.size());
    	
    	assertEquals(0, AbstractMessage.getStartLine(tokenOccurrences.get(0), doc)-1);
    	assertEquals(7, AbstractMessage.getStartCol(tokenOccurrences.get(0), doc)-1);
    	
    	assertEquals(1, tokenOccurrences.get(1).getLineDefinition()-1);
    	assertEquals(6, tokenOccurrences.get(1).getColDefinition()-1);
    }    	
    
    
    public void testIt3() throws Exception {
    	doc = new Document(
    			"import os.path\n"+
    			"print os.path\n"+
    			"\n"
    	);
    	int line=0;
    	int col=12;
    	checkTest3Results(line, col);
    	
    	line=1;
    	col=12; //same thing, but now checking through one of the references
    	checkTest3Results(line, col);
    }

	private void checkTest3Results(int line, int col) throws Exception {
		List<IToken> tokenOccurrences = getTokenOccurrences(line, col);
    	assertEquals(2, tokenOccurrences.size());
    	
    	IToken tok0 = tokenOccurrences.get(0);
    	assertEquals("path", tok0.getRepresentation());
		assertEquals(0, AbstractMessage.getStartLine(tok0, doc)-1);
    	assertEquals(10, AbstractMessage.getStartCol(tok0, doc)-1);
    	
    	IToken tok1 = tokenOccurrences.get(1);
    	assertEquals("path", tok1.getRepresentation());
		assertEquals(1, AbstractMessage.getStartLine(tok1, doc)-1);
    	assertEquals(9, AbstractMessage.getStartCol(tok1, doc)-1);
	}    	
    
    
    public void testIt() throws Exception {
        doc = new Document(
                "foo = 20\n"+
                "print foo\n"+
                "\n"
        );
        int line=0;
        int col=1;
        List<Found> occurrences = getFoundOccurrences(line, col);
        assertEquals(1, occurrences.size());
        Found f = occurrences.get(0);
        
        IToken generator = f.getSingle().generator;
		assertEquals(0, generator.getLineDefinition()-1);
        assertEquals(0, generator.getColDefinition()-1);
        
        List<IToken> references = f.getSingle().references;
        assertEquals(1, references.size());
        
        IToken reference = references.get(0);
		assertEquals(1, reference.getLineDefinition()-1);
        assertEquals(6, reference.getColDefinition()-1);
        
        List<IToken> tokenOccurrences = getTokenOccurrences(line, col);
        assertEquals(2, tokenOccurrences.size());
        assertEquals(0, tokenOccurrences.get(0).getLineDefinition()-1);
        assertEquals(1, tokenOccurrences.get(1).getLineDefinition()-1);
    }

	private List<IToken> getTokenOccurrences(int line, int col) throws Exception {
		ScopeAnalyzerVisitor visitor = doVisit(line, col);
		return visitor.getTokenOccurrences();
	}

	private List<Found> getFoundOccurrences(int line, int col) throws Exception {
		ScopeAnalyzerVisitor visitor = doVisit(line, col);
		List<Found> occurrences = visitor.getFoundOccurrences();
		return occurrences;
	}

	private ScopeAnalyzerVisitor doVisit(int line, int col) throws Exception {
		SourceModule mod = (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0);
		PySelection ps = new PySelection(doc, line, col);
        ScopeAnalyzerVisitor visitor = new ScopeAnalyzerVisitor(nature, "mod1", mod, doc, new NullProgressMonitor(), ps);
        mod.getAst().accept(visitor);
		return visitor;
	}

}
