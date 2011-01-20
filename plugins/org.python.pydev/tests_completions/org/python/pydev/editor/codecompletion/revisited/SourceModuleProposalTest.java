/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
///*
// * Created on Apr 14, 2005
// *
// * @author Fabio Zadrozny
// */
//package org.python.pydev.editor.codecompletion.revisited;
//
//import junit.framework.TestCase;
//
//import org.eclipse.jface.text.BadLocationException;
//import org.eclipse.jface.text.Document;
//import org.eclipse.jface.text.IDocument;
//import org.eclipse.jface.text.IRegion;
//import org.python.pydev.core.IPythonNature;
//import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
//import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
//import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
//
///**
// * @author Fabio Zadrozny
// */
//public class SourceModuleProposalTest extends TestCase {
//
//    public static void main(String[] args) {
//        try {
//            SourceModuleProposalTest test = new SourceModuleProposalTest();
//            test.setUp();
//            test.testOffset();
//            test.tearDown();
//            junit.textui.TestRunner.run(SourceModuleProposalTest.class);
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//    }
//
//    private IPythonNature nature;
//
//    /*
//     * @see TestCase#setUp()
//     */
//    protected void setUp() throws Exception {
//        super.setUp();
//        nature = CodeCompletionTestsBase.createStaticNature();
//    }
//
//    /*
//     * @see TestCase#tearDown()
//     */
//    protected void tearDown() throws Exception {
//        super.tearDown();
//    }
//    
//    public void testOffset() throws BadLocationException{
//        String docStr = ""+
//        "class A:                   \n" +
//        "    pass                   \n" +
//        "                           \n" +
//        "if __name__ == '__main__': \n" +
//        "    pass                   \n";
//        
//        checkProposal(docStr, 2, "A" , SourceModuleProposal.ADD_TO_LAST_CLASS_LINE);
//        checkProposal(docStr, 2, null, SourceModuleProposal.ADD_TO_LAST_LINE_BEFORE_MAIN);
//
//        docStr = ""+
//        "class A:                   \n" +
//        "    pass                   \n" +
//        "                           \n" +
//        "class B:                   \n" +
//        "    pass                   \n" +
//        "                           \n" +
//        "if __name__ == '__main__': \n" +
//        "    pass                   \n";
//        
//        checkProposal(docStr, 5, null, SourceModuleProposal.ADD_TO_LAST_LINE_BEFORE_MAIN);
//        checkProposal(docStr, 2, "A" , SourceModuleProposal.ADD_TO_LAST_CLASS_LINE);
//        
//    }
//
//    /**
//     * @param docStr
//     * @param line
//     * @param definition
//     * @param addTo
//     * @throws BadLocationException
//     */
//    private void checkProposal(String docStr, int line, String tokDef, int addTo) throws BadLocationException {
//        IDocument doc = new Document(docStr);
//        SourceModule mod = (SourceModule) AbstractModule.createModuleFromDoc("", null, doc, nature, 0);
//        SourceModuleProposal s = new SourceModuleProposal("",0,0,0,null,"",null,"",mod, IPyCompletionProposal.PRIORITY_DEFAULT);
//        s.doc = doc;
//        if(tokDef != null)
//            try {
//                s.definition = mod.findGlobalTokDef(CompletionStateFactory.getEmptyCompletionState(tokDef, null, new CompletionCache()), null);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        s.addTo = addTo;
//        IRegion region = doc.getLineInformation(line);
//        assertEquals(region.getOffset()+region.getLength(), s.getReplacementOffset());
//    }
//
//}
