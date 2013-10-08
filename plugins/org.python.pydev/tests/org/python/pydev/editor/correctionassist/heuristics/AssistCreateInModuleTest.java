/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * Might be useful when a decent implementation is given
 * 
 * @author Fabio Zadrozny
 */
public class AssistCreateInModuleTest extends CodeCompletionTestsBase {

    private IAssistProps assist;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AssistCreateInModuleTest.class);
    }

    //    /*
    //     * @see TestCase#setUp()
    //     */
    //    protected void setUp() throws Exception {
    //        super.setUp();
    //        restorePythonPath(false);
    //    }
    //
    //    /*
    //     * @see TestCase#tearDown()
    //     */
    //    protected void tearDown() throws Exception {
    //        super.tearDown();
    //    }
    //
    //    /**
    //     * @throws BadLocationException
    //     * 
    //     */
    //    public void testAssistMethod() throws BadLocationException {
    //        assist = new AssistCreateMethodInModule();
    //        String d = ""+
    //        "from testAssist import assist\n" +
    //        "assist.NewMethod(a,b)";
    //
    //        String res = "\n" +
    //        "def NewMethod(a,b):\n" +
    //        "    '''\n"+
    //        "    @param a:\n"+
    //        "    @param b:\n"+
    //        "    '''\n"+
    //        "    ";
    //
    //        checkCreateMethod(d, res, "testAssist.assist", true, -1, "NewMethod");
    //
    //        d = ""+
    //        "from testAssist import assist\n" +
    //        "NewMethod(a,b)";
    //        checkCreateMethod(d, res, "", true, -1, "NewMethod");
    //
    //        d = ""+
    //        "class NewClass(object): \n" +
    //        "                        \n" +
    //        "   def NewMethod(a,b):  \n" +
    //        "        pass            \n";
    //        checkCreateMethod(d, res, "", false, 0, "");
    //        checkCreateMethod(d, res, "", false, 2, "");
    //    }
    //
    //    
    //    /**
    //     * @param docStr
    //     * @param res
    //     * @param moduleName
    //     * @param isValid
    //     * @param line: offset is set to the end of the passed line
    //     * @throws BadLocationException
    //     */
    //    private void checkCreateMethod(String docStr, String res, String moduleName, boolean isValid, int line, String tok) throws BadLocationException {
    //        Document doc = new Document(docStr);
    //        
    //        int offset = 0;
    //        if (line == -1){
    //            offset = docStr.length();
    //        }else{
    //            IRegion lineInformation = doc.getLineInformation(line);
    //            offset = lineInformation.getOffset() + lineInformation.getLength();
    //        }
    //        
    //        PySelection ps = new PySelection(doc, new TextSelection(doc, offset, 0));
    //        String sel = PyAction.getLineWithoutComments(ps);
    //
    //        assertEquals(isValid, assist.isValid(ps, sel, null, offset));
    //        if(isValid){
    //            List props = assist.getProps(ps, null, null, nature, null, offset);
    //            assertEquals(1, props.size());
    //            SourceModuleProposal p = (SourceModuleProposal) props.get(0);
    //            
    //            
    //            assertEquals(res, p.getReplacementStr());
    //            assertEquals(moduleName, p.module.getName());
    //            assertTrue(p.getDisplayString().indexOf(tok) != -1);
    //        }
    //    }
    //
    //
    //    /**
    //     * @throws BadLocationException
    //     * 
    //     */
    //    public void testAssistClass() throws BadLocationException {
    //        String docStr = ""+
    //        "from testAssist import assist\n" +
    //        "assist.NewClass(a,b)";
    //        String moduleName = "testAssist.assist";
    //        int nProps = 1;
    //        assist = new AssistCreateClassInModule();
    //        checkAssistClass(docStr, moduleName, nProps,-1,0, "NewClass", true);
    //        
    //
    //        nProps = 1;
    //        moduleName = "";
    //        docStr = ""+
    //        "from testAssist import assist\n" +
    //        "newClass = NewClass(a,b)";
    //        checkAssistClass(docStr, moduleName, nProps,-1,0, "NewClass", true);
    //
    //        docStr = ""+
    //        "from testAssist import assist\n" +
    //        "newClass = NewClass()";
    //        checkAssistClass(docStr, moduleName, nProps,-1,0, "NewClass", false);
    //
    //        //same as before... different sel
    //        docStr = ""+
    //        "from testAssist import assist\n" + //30 chars
    //        "assist.Ignore(NewClass(a,b))   ";  //from 14 to +13  
    //        checkAssistClass(docStr, moduleName, nProps,30+14,13, "NewClass", true);
    //    }
    //
    //    /**
    //     * @param docStr
    //     * @param moduleName
    //     * @param nProps
    //     * @param t
    //     * @throws BadLocationException
    //     */
    //    private void checkAssistClass(String docStr, String moduleName, int nProps, int selStart, int selLength, String tok, boolean hasParams) throws BadLocationException {
    //        if (selStart == -1)
    //            selStart = docStr.length();
    //        Document doc = new Document(docStr);
    //        PySelection ps = new PySelection(doc, new TextSelection(doc, selStart, selLength));
    //        String sel = PyAction.getLineWithoutComments(ps);
    //
    //        assertEquals(true, assist.isValid(ps, sel, null, selStart));
    //        List props = assist.getProps(ps, null, null, nature, null, selStart);
    //        assertEquals(nProps, props.size());
    //        SourceModuleProposal p = (SourceModuleProposal) props.get(0);
    //        
    //        String res = "\n" +
    //        "class NewClass(object):\n" +
    //        "    '''\n"+
    //        "    '''\n"+
    //        "    \n";
    //        
    //        if (hasParams){
    //            res += ""+
    //            "    def __init__(self, a, b):\n"+
    //            "        '''\n"+
    //            "        @param a:\n"+
    //            "        @param b:\n"+
    //            "        '''\n"+
    //            "        ";
    //        }
    //        
    //        assertEquals(res, p.getReplacementStr());
    //        assertEquals(moduleName, p.module.getName());
    //        assertTrue(p.getDisplayString().indexOf(tok) != -1);
    //    }
    //
    //    /**
    //     * @throws BadLocationException
    //     * 
    //     */
    //    public void testAssistMethodInClass() throws BadLocationException {
    //        assist = new AssistCreateMethodInClass();
    //        String d = ""+
    //        "from testAssist import assist\n" +
    //        "ex = assist.ExistingClass()\n" +
    //        "ex.newMethod(c,d)";
    //
    //        Document doc = new Document(d);
    //        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
    //        String sel = PyAction.getLineWithoutComments(ps);
    //
    //        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
    //        List props = assist.getProps(ps, null, null, nature, null, d.length());
    //        assertEquals(1, props.size());
    //        SourceModuleProposal p = (SourceModuleProposal) props.get(0);
    //        
    //        String res = "\n" +
    //        "    def newMethod(self, c, d):\n"+
    //        "        '''\n"+
    //        "        @param c:\n"+
    //        "        @param d:\n"+
    //        "        '''\n"+
    //        "        ";
    //        assertEquals(res, p.getReplacementStr());
    //        assertEquals("testAssist.assist", p.module.getName());
    //        assertEquals("ExistingClass", p.definition.value);
    //        assertEquals(SourceModuleProposal.ADD_TO_LAST_CLASS_LINE, p.addTo);
    //        assertTrue(p.getDisplayString().indexOf("newMethod") != -1);
    //    }
}
