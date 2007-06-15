/*
 * Created on Feb 21, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyOrganizeImportsTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyOrganizeImportsTest test = new PyOrganizeImportsTest();
            test.setUp();
            test.testPerform6();
            test.tearDown();
            junit.textui.TestRunner.run(PyOrganizeImportsTest.class);
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

    public void testPerform() {
        String d = ""+
"import b\n"+
"import a\n"+
"\n"+
"from a import c\n"+
"from b import d\n"+
"from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
String result = ""+
"from a import b\n"+
"from a import c\n"+
"from b import d\n"+
"import a\n"+
"import b\n"+
"\n";

		assertEquals(result, doc.get());

    }

    public void testPerform2() {
        
        String header = ""+
"'''\n"+
"from fff import xxx #ignore\n"+
"import ggg #ignore\n"+
"import aaa #ignore\n"+
"'''\n";

        String d = ""+header+
"import b\n"+
"import a\n"+
"\n"+
"from a import c\n"+
"from b import d\n"+
"from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
String result = ""+header+
"from a import b\n"+
"from a import c\n"+
"from b import d\n"+
"import a\n"+
"import b\n"+
"\n";

		assertEquals(result, doc.get());

    }

    public void testPerform3() {
        
        String header = ""+
"'''\n"+
"from fff import xxx #ignore\n"+
"import ggg #ignore\n"+
"import aaa #ignore'''\n";

        String d = ""+header+
"import b\n"+
"import a\n"+
"\n"+
"from a import c\n"+
"from b import d\n"+
"from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
String result = ""+header+
"from a import b\n"+
"from a import c\n"+
"from b import d\n"+
"import a\n"+
"import b\n"+
"\n";

		assertEquals(result, doc.get());

    }

    public void testPerform4() {
        
        String header = ""+
"'''ignore'''\n"+
"from a import aaa\n";

        String d = ""+header+
"import b\n"+
"import a\n"+
"\n"+
"from a import c\n"+
"from b import d\n"+
"from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
String result = ""+header+
"from a import b\n"+
"from a import c\n"+
"from b import d\n"+
"import a\n"+
"import b\n"+
"\n";

		assertEquals(result, doc.get());

    }
    
    public void testPerform5() {
        
        
        String d = ""+
        "import sys\n"+
        "from os import (pipe,\n"+
        "path)\n"+
        "import time\n";
        
        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
        String result = ""+
        "from os import (pipe,\n"+
        "path)\n"+
        "import sys\n"+
        "import time\n";
        assertEquals(result, doc.get());
        
    }
    
    public void testPerform6() {
        
        
        String d = ""+
        "import sys #comment1\n"+
        "import sys2 #comment2\n";
        
        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n");
        
        assertEquals(d, doc.get());
        
    }

    
    public void testPerformSort(){
        String s = ""+
"line4\n"+
"line1\n"+
"line3\n"+ //end the selection
"line2\n";

        
        String result = ""+
"line1\n"+
"line3\n"+
"line4\n"+
"line2\n"; //not changed

        Document doc = new Document(s);
        PyOrganizeImports.performSimpleSort(doc, "\n",0,2);

		assertEquals(result, doc.get());
    }
}
