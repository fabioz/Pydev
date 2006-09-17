/*
 * Created on Mar 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCodeCompletionTest.class);
    }
    
    PyCodeCompletion completion;
    
    public void doTest(String s, String expected){
        Document doc = new Document(s);
        int length = s.length();
        String tipperStr = PyCodeCompletion.getImportsTipperStr(doc, length);
        assertEquals(expected, tipperStr);
        
    }
    public void testIt(){
        completion = new PyCodeCompletion();
        
        doTest("import unittest.bar.f, os.path, sy", " ");
        doTest("import unittest.bar.f, a.", "a");
        doTest("import unittest.bar.f, ", " ");
        
        doTest("import unittest.", "unittest");
        doTest("import unittest", " ");
        doTest("import unittest.bar.f", "unittest.bar");
        
        doTest("from .. import ", "..");
        doTest("from ..bar import ", "..bar");
        
        doTest("from . import unittest , ", ".");
        doTest("from .. import unittest , ", "..");
        
        doTest("from datetime import datetime, date, MINYEAR,", "datetime");
        doTest("    from datetime import datetime, date, MINYEAR,", "datetime");
        doTest("no    from datetime import datetime, date, MINYEAR,", "");
        
        doTest("from datetime.datetime import ", "datetime.datetime");
        doTest("    from datetime.datetime import ", "datetime.datetime");
        
        doTest("from testlib import unittest , ", "testlib");
        doTest("    from testlib import unittest , ", "testlib");

        doTest("from datetime.datetime import to", "datetime.datetime");
        doTest("    from datetime.datetime import to", "datetime.datetime");

        doTest("from this space", "");
        doTest("from ", " ");
    }

}
