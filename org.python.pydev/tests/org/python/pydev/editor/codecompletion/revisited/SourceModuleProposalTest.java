/*
 * Created on Apr 14, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

/**
 * @author Fabio Zadrozny
 */
public class SourceModuleProposalTest extends TestCase {

    public static void main(String[] args) {
        try {
            SourceModuleProposalTest test = new SourceModuleProposalTest();
            test.setUp();
            test.testOffset();
            test.tearDown();
            junit.textui.TestRunner.run(SourceModuleProposalTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testOffset() throws BadLocationException{
        String docStr = ""+
        "class A:                   \n" +
        "    pass                   \n" +
        "                           \n" +
        "if __name__ == '__main__': \n" +
        "    pass                   \n";
        
        checkProposal(docStr, 2, "A" , SourceModuleProposal.ADD_TO_LAST_CLASS_LINE);
        checkProposal(docStr, 2, null, SourceModuleProposal.ADD_TO_LAST_LINE_BEFORE_MAIN);

        docStr = ""+
        "class A:                   \n" +
        "    pass                   \n" +
        "                           \n" +
        "class B:                   \n" +
        "    pass                   \n" +
        "                           \n" +
        "if __name__ == '__main__': \n" +
        "    pass                   \n";
        
        checkProposal(docStr, 5, null, SourceModuleProposal.ADD_TO_LAST_LINE_BEFORE_MAIN);
        checkProposal(docStr, 2, "A" , SourceModuleProposal.ADD_TO_LAST_CLASS_LINE);
        
    }

    /**
     * @param docStr
     * @param line
     * @param definition
     * @param addTo
     * @throws BadLocationException
     */
    private void checkProposal(String docStr, int line, String tokDef, int addTo) throws BadLocationException {
        IDocument doc = new Document(docStr);
        SourceModule mod = (SourceModule) AbstractModule.createModuleFromDoc("", null, doc, null, 0);
        SourceModuleProposal s = new SourceModuleProposal("",0,0,0,null,"",null,"",mod, IPyCompletionProposal.PRIORITY_DEFAULT);
        s.doc = doc;
        if(tokDef != null)
            s.definition = mod.findGlobalTokDef(CompletionState.getEmptyCompletionState(tokDef, null), null);
        s.addTo = addTo;
        IRegion region = doc.getLineInformation(line);
        assertEquals(region.getOffset()+region.getLength(), s.getReplacementOffset());
    }

}
