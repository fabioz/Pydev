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
            analyzer2.testCompletionGroupFromImport5();
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
        
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        
        assertEquals("from XXX import YYY\nYYY", doc.get());
    }
    
    public void testCompletionGroupFromImport() throws Exception {
        Document doc = new Document("from XXX import YYY\n");
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal("BBB", 
                doc.getLength(), 0, doc.getLength(), null, "Import BBB (from XXX)", null, 
                null, 0, "from XXX import BBB");
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import YYY, BBB\nBBB", doc.get());
    }
    
    
    public void testCompletionGroupFromImport2() throws Exception {
        Document doc = new Document("from XXX import (YYY, ZZZ)\n");
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal("BBB", 
                doc.getLength(), 0, doc.getLength(), null, "Import BBB (from XXX)", null, 
                null, 0, "from XXX import BBB");
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import (YYY, ZZZ, BBB)\nBBB", doc.get());
    }
    
    public void testCompletionGroupFromImport3() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document("from XXX import (YYY,                                                                     ZZZ)\n");
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal("BBB", 
                doc.getLength(), 0, doc.getLength(), null, "Import BBB (from XXX)", null, 
                null, 0, "from XXX import BBB");
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import (YYY,                                                                     ZZZ,\n\tBBB)\nBBB", doc.get());
    }
    
    
    public void testCompletionGroupFromImport4() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document("from XXX import YYY,                                                                     ZZZ\n");
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal("BBB", 
                doc.getLength(), 0, doc.getLength(), null, "Import BBB (from XXX)", null, 
                null, 0, "from XXX import BBB");
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import YYY,                                                                     ZZZ,\\\n\tBBB\nBBB", doc.get());
    }
    
    public void testCompletionGroupFromImport5() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document("from XXX import WWW,                                                                     RRR # comment\n" +
                "from XXX import YYY,                                                                     ZZZ\n");
        
        CtxInsensitiveImportComplProposal prop = new CtxInsensitiveImportComplProposal("BBB", 
                doc.getLength(), 0, doc.getLength(), null, "Import BBB (from XXX)", null, 
                null, 0, "from XXX import BBB");
        
        prop.indentString="\t";
        prop.apply(doc, '\n', 0, doc.getLength());
//        System.out.println(">>"+doc.get()+"<<");
        assertEquals("from XXX import WWW,                                                                     RRR # comment\n" +
                "from XXX import YYY,                                                                     ZZZ,\\\n\tBBB\nBBB", doc.get());
    }
    
    
}
