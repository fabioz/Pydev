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
import com.python.pydev.analysis.visitors.Found;

public class ScopeAnalyzerVisitorTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ScopeAnalyzerVisitorTest.class);
    }

    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testIt() throws Exception {
        doc = new Document(
                "foo = 20\n"+
                "print foo\n"+
                "\n"
        );
        int line=0;
        int col=1;
        List<Found> occurrences = getOccurrences(line, col);
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
        
        
    }

	private List<Found> getOccurrences(int line, int col) throws Exception {
		SourceModule mod = (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0);
		PySelection ps = new PySelection(doc, line, col);
        ScopeAnalyzerVisitor visitor = new ScopeAnalyzerVisitor(nature, "mod1", mod, doc, new NullProgressMonitor(), ps);
        mod.getAst().accept(visitor);
        List<Found> occurrences = visitor.getOccurrences();
		return occurrences;
	}

}
