/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.AnalysisTestsBase;

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
        SourceModule mod = (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0);
        new ScopeAnalyzerVisitor(nature, "foo", mod, doc, new NullProgressMonitor());
    }

}
