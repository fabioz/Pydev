package com.python.pydev.analysis.tabnanny;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;

public class TabNannyIteratorTest extends TestCase {

    public static void main(String[] args) {
        try {
            TabNannyIteratorTest analyzer2 = new TabNannyIteratorTest();
            analyzer2.setUp();
            analyzer2.testIterator11();
            analyzer2.tearDown();
            System.out.println("finished");
            
            junit.textui.TestRunner.run(TabNannyIteratorTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testIterator() throws Exception {
        Document doc = new Document("" +
        		"aaa\\\n" +
        		"bbbb\n" +
        		"ccc\n" +
        		""
                );
        
        TabNannyDocIterator iterator = new TabNannyDocIterator(doc);
        assertTrue(!iterator.hasNext()); //no indentations here...
    }
    

    public void testIterator2() throws Exception {
        String str = "" +
        "d\n" +
        "    pass\r" +
        "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); //no indentations here...
    }
    
    public void testIterator3() throws Exception {
        String str = "" +
        "d\n" +
        "    '''\r" +
        "    '''\r" +
        "\t" +
        "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertEquals("\t",it.next().o1);
        assertTrue(!it.hasNext()); //no indentations here...
    }
    
    public void testIterator4() throws Exception {
        String str = "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator5() throws Exception {
        String str = "    #comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator6() throws Exception {
        String str = 
                "    #comment   what's happening\\\n" + //escape is in comment... (so, it's not considered the same line)
        		"    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
   
    
    public void testIterator7() throws Exception {
        String str = 
            "    g g g \t g\\\n" + //escape considered 
            "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator8() throws Exception {
        String str = 
            "{g }\n" +
            "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator9() throws Exception {
        String str = 
            "{g \n" +
            " ( ''' thnehouno '''\n" +
            "}\n" +
            "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator10() throws Exception {
        String str = 
            "{g \n" + //error here
            " ( ''' thnehouno '''\n" +
            "\n" +
            "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertTrue(!it.hasNext()); 
    }
    
    public void testIterator11() throws Exception {
        Document doc = new Document("" +
                "aaa\n" +
                "\t\n" +
                "ccc\n" +
                ""
                );
        TabNannyDocIterator it = new TabNannyDocIterator(doc);
        assertEquals("\t",it.next().o1);
        assertTrue(!it.hasNext()); 
    }
}
