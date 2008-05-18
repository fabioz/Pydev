package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;

import junit.framework.TestCase;

public class ImportHandleTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

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
        lst.add("CCC");
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
        
        importHandle = new ImportHandle(null, "from .AAA import (BBB, #, DDD as CCC\nKKK)", 0, 0);
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
    }
    
    public void testImportMatches() throws Exception {
        ImportHandle importHandle = new ImportHandle(null, "from AAA import BBB", 0, 0);
        assertFalse(importHandle.contains("from  X import BBB"));
        assertFalse(importHandle.contains("from  XXX  import   BBB"));
        assertFalse(importHandle.contains("from  AAA  import   CCC"));
        assertTrue(importHandle.contains("from  AAA  import   BBB"));
        
        importHandle = new ImportHandle(null, "from AAA import BBB;from XXX import YYY", 0, 0);
        assertTrue(importHandle.contains("from  XXX  import   YYY"));
    }

}
