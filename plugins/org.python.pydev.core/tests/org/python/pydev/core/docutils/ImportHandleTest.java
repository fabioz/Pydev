/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;

import junit.framework.TestCase;

public class ImportHandleTest extends TestCase {

    public static void main(String[] args) {
        try {
            ImportHandleTest test = new ImportHandleTest();
            test.setUp();
            test.testImportHandleInfo3();
            test.tearDown();
            junit.textui.TestRunner.run(ImportHandleTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testImportHandleInfo2() throws Exception {
        ImportHandle importHandle = new ImportHandle(null, "import BBB", 0, 0);
        List<ImportHandleInfo> importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals(null, importInfo.get(0).getFromImportStr());
        ArrayList<String> lst = new ArrayList<String>();
        lst.add("BBB");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "import BBB, CCC", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals(null, importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("CCC");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "import BBB, CCC #Comment", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals(null, importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("CCC");
        assertEquals(lst, importInfo.get(0).getImportedStr());
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("");
        comments.add("#Comment");
        assertEquals(comments, importInfo.get(0).getCommentsForImports());

    }

    public void testImportHandleInfoLines() throws Exception {
        ImportHandle importHandle = new ImportHandle(null, "from AAA import (BBB, \nCCC);from XXX import YYY", 0, 1);
        List<ImportHandleInfo> importInfo = importHandle.getImportInfo();
        assertEquals(2, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        assertEquals("XXX", importInfo.get(1).getFromImportStr());
        assertEquals(0, importInfo.get(0).getStartLine());
        assertEquals(1, importInfo.get(0).getEndLine());
        assertEquals(1, importInfo.get(1).getStartLine());
        assertEquals(1, importInfo.get(1).getEndLine());
        assertTrue(importInfo.get(1).getStartedInMiddleOfLine());

    }

    public void testImportHandleInfo3() throws Exception {
        ImportHandle importHandle = new ImportHandle(null,
                "from wx.xrc import XML_ATTRIBUTE_NODE, XML_CDATA_SECTION_NODE,\\\n"
                        + "XML_DOCUMENT_FRAG_NODE, XML_DOCUMENT_TYPE_NODE, XML_ELEMENT_NODE,\\\n" + "XML_COMMENT_NODE",
                0, 0);

        List<ImportHandleInfo> importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("wx.xrc", importInfo.get(0).getFromImportStr());
        ArrayList<String> lst = new ArrayList<String>();
        lst.add("XML_ATTRIBUTE_NODE");
        lst.add("XML_CDATA_SECTION_NODE");
        lst.add("XML_DOCUMENT_FRAG_NODE");
        lst.add("XML_DOCUMENT_TYPE_NODE");
        lst.add("XML_ELEMENT_NODE");
        lst.add("XML_COMMENT_NODE");
        assertEquals(lst, importInfo.get(0).getImportedStr());
    }

    public void testImportHandleInfo() throws Exception {
        ImportHandle importHandle = new ImportHandle(null, "from AAA import BBB", 0, 0);
        List<ImportHandleInfo> importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        ArrayList<String> lst = new ArrayList<String>();
        lst.add("BBB");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "from AAA import BBB, CCC", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("CCC");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "from AAA import BBB, DDD as CCC", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("DDD as CCC");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "from AAA import BBB #, DDD as CCC", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        assertEquals(lst, importInfo.get(0).getImportedStr());
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("#, DDD as CCC");
        assertEquals(comments, importInfo.get(0).getCommentsForImports());

        importHandle = new ImportHandle(null, "from AAA.bbb.ccc import BBB", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA.bbb.ccc", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        assertEquals(lst, importInfo.get(0).getImportedStr());

        importHandle = new ImportHandle(null, "from AAA import (BBB, #, DDD as CCC\nKKK)", 0, 0);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals("AAA", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("KKK");
        comments = new ArrayList<String>();
        comments.add("#, DDD as CCC");
        comments.add("");
        assertEquals(lst, importInfo.get(0).getImportedStr());
        assertEquals(comments, importInfo.get(0).getCommentsForImports());

        importHandle = new ImportHandle(null, "from .AAA import (BBB, #, DDD as CCC\nKKK)", 0, 1);
        importInfo = importHandle.getImportInfo();
        assertEquals(1, importInfo.size());
        assertEquals(".AAA", importInfo.get(0).getFromImportStr());
        lst = new ArrayList<String>();
        lst.add("BBB");
        lst.add("KKK");
        comments = new ArrayList<String>();
        comments.add("#, DDD as CCC");
        comments.add("");
        assertEquals(lst, importInfo.get(0).getImportedStr());
        assertEquals(comments, importInfo.get(0).getCommentsForImports());
        assertEquals(0, importInfo.get(0).getStartLine());
        assertEquals(1, importInfo.get(0).getEndLine());
    }

    public void testImportMatches() throws Exception {
        ImportHandle importHandle = new ImportHandle(null, "from AAA import BBB", 0, 0);
        assertFalse(importHandle.contains(new ImportHandle.ImportHandleInfo("from  X import BBB")));
        assertFalse(importHandle.contains(new ImportHandle.ImportHandleInfo("from  XXX  import   BBB")));
        assertFalse(importHandle.contains(new ImportHandle.ImportHandleInfo("from  AAA  import   CCC")));
        assertTrue(importHandle.contains(new ImportHandle.ImportHandleInfo("from  AAA  import   BBB")));

        importHandle = new ImportHandle(null, "from AAA import BBB;from XXX import YYY", 0, 0);
        assertTrue(importHandle.contains(new ImportHandle.ImportHandleInfo("from  XXX  import   YYY")));

        importHandle = new ImportHandle(null, "from AAA.aaa\\\n   .bbb import BBB", 0, 0);
        assertTrue(importHandle.contains(new ImportHandle.ImportHandleInfo("from AAA.aaa.bbb import BBB")));
    }

}
