package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;

import junit.framework.TestCase;

public class PyAddBlockCommentTest extends TestCase {


    
    public void testBlock() throws Exception {
        Document doc = null;
        
        doc = new Document("cc");
        new PyAddBlockComment(10, true, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
                "#---------\r\n" +
                "# cc\r\n" +
                "#---------", doc.get());
        
        doc = new Document("\tcc");
        new PyAddBlockComment(10, true, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
                "#---------\r\n" +
                "#\tcc\r\n" +
                "#---------", doc.get());
        
        doc = new Document("class Foo(object):");
        new PyAddBlockComment(10, true, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
                "#---------\r\n" +
                "# Foo\r\n" +
                "#---------\r\n" +
                "class Foo(object):", doc.get());
        
        
        doc = new Document("class Information( UserDict.UserDict, IInformation ):");
        new PyAddBlockComment(10, true, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
        		"#---------\r\n" +
        		"# Information\r\n" +
        		"#---------\r\n" +
        		"class Information( UserDict.UserDict, IInformation ):", doc.get());
        
        //without class behavior
        doc = new Document("class Foo(object):");
        new PyAddBlockComment(10, true, false).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
                "#---------\r\n" +
                "# class Foo(object):\r\n" +
                "#---------" +
                "", doc.get());
        
        //aligned class
        doc = new Document("    class Foo(object):");
        new PyAddBlockComment(10, true, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("" +
                "    #-----\r\n" +
                "    # Foo\r\n" +
                "    #-----\r\n" +
                "    class Foo(object):" +
                "", doc.get());
        
        
    }
}
