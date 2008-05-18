package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import junit.framework.TestCase;

public class CtxInsensitiveImportComplProposalTest extends TestCase {
    
    public static void main(String[] args) {
        try {
            CtxInsensitiveImportComplProposalTest analyzer2 = new CtxInsensitiveImportComplProposalTest();
            analyzer2.setUp();
            analyzer2.tearDown();
            System.out.println("finished");
            
            
            junit.textui.TestRunner.run(CtxInsensitiveImportComplProposalTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCompletionDontAddFromImport() throws Exception {
        Document doc = new Document("from XXX import YYY\n");
        
        String replacementString="YYY";
        int replacementOffset=doc.getLength();
        int cursorPosition=doc.getLength();
        int replacementLength=0;
        String realImportRep="from XXX import YYY";
        int priority=0;
        String additionalProposalInfo=null;
        IContextInformation contextInformation=null;
        String displayString="Import YYY (from XXX)";
        Image image = null;
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal(replacementString, 
                replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, 
                additionalProposalInfo, priority, realImportRep);
        
        
        prop.apply(doc, '\n', 0, doc.getLength());
        
        assertEquals("from XXX import YYY\nYYY", doc.get());
    }
//    
//    public void testCompletionDontAddFromImport() throws Exception {
//        Document doc = new Document("from XXX import YYY\n");
//        
//        String replacementString="BBB";
//        int replacementOffset=doc.getLength();
//        int cursorPosition=doc.getLength();
//        int replacementLength=0;
//        String realImportRep="from XXX import BBB";
//        int priority=0;
//        String additionalProposalInfo=null;
//        IContextInformation contextInformation=null;
//        String displayString="Import BBB (from XXX)";
//        Image image = null;
//        
//        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal(replacementString, 
//                replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, 
//                additionalProposalInfo, priority, realImportRep);
//        
//        
//        prop.apply(doc, '\n', 0, doc.getLength());
//        
//        System.out.println(doc.get());
//        assertEquals("from XXX import YYY, BBB\nBBB", doc.get());
//    }
}
