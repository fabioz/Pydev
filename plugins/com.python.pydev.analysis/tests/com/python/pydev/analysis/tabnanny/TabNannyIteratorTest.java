/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.tabnanny;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.TabNannyDocIterator;

public class TabNannyIteratorTest extends TestCase {

    public static void main(String[] args) {
        try {
            TabNannyIteratorTest analyzer2 = new TabNannyIteratorTest();
            analyzer2.setUp();
            analyzer2.testIterator10();
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
                "");

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
        assertEquals("    ", it.next().o1);
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
        assertEquals("    ", it.next().o1);
        assertEquals("\t", it.next().o1);
        assertTrue(!it.hasNext()); //no indentations here...
    }

    public void testIterator4() throws Exception {
        String str = "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertTrue(!it.hasNext());
    }

    public void testIterator4a() throws Exception {
        String str = "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertTrue(!it.hasNext());
    }

    public void testIterator5() throws Exception {
        String str = "    #comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator5a() throws Exception {
        String str = "    #comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator5b() throws Exception {
        String str = " #comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals(" ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator5c() throws Exception {
        String str = "#comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator5d() throws Exception {
        String str = "#comment";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertTrue(!it.hasNext());
    }

    public void testIterator6() throws Exception {
        String str = "    #comment   what's happening\\\n" + //escape is in comment... (so, it's not considered the same line)
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator6b() throws Exception {
        String str = "    #comment   what's happening\\\n" + //escape is in comment... (so, it's not considered the same line)
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator6a() throws Exception {
        String str = "    #comment   what's happening\r\n" +
                "    #comment   what's happening2\r\n" +
                "    pass\r\n";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator6c() throws Exception {
        String str = "    #comment   what's happening\r\n" +
                "    #comment   what's happening2\r\n" +
                "    pass\r\n";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator7() throws Exception {
        String str = "    g g g \t g\\\n" + //escape considered 
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator7a() throws Exception {
        String str = "    g g g \t g\\\n" + //escape considered 
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator8() throws Exception {
        String str = "{g }\n" +
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator8a() throws Exception {
        String str = "{g }\n" +
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator9a() throws Exception {
        String str = "{g \n" +
                " ( ''' thnehouno '''\n" +
                "}\n" +
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator9() throws Exception {
        String str = "{g \n" +
                " ( ''' thnehouno '''\n" +
                "}\n" +
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator10() throws Exception {
        String str = "{g \n" + //error here
                " ( ''' thnehouno '''\n" +
                "\n" +
                "    pass";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d);
        assertEquals(" ", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator11() throws Exception {
        Document doc = new Document("" +
                "aaa\n" +
                "\t\n" +
                "ccc\n" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc);
        assertEquals("\t", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator11a() throws Exception {
        Document doc = new Document("" +
                "aaa\n" +
                "\t\n" +
                "ccc\n" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc, true, false);
        assertEquals("", it.next().o1);
        //        assertEquals("\t",it.next().o1); -- empty line
        assertEquals("", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator11b() throws Exception {
        Document doc = new Document("" +
                "aaa\n" +
                "\ta\n" +
                "ccc\n" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc, true, false);
        assertEquals("", it.next().o1);
        assertEquals("\t", it.next().o1);
        assertEquals("", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator12() throws Exception {
        Document doc = new Document("" +
                "{\n" +
                "\t\n" + //don't return this one -- inside of {}
                "}\n" +
                "pass\n" +
                "    pass" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIteratorWithEmptyIndents() throws Exception {
        Document doc = new Document("" +
                "{\n" +
                "\t\n" + //don't return this one -- inside of {}
                "}\n" +
                "pass\n" +
                "    pass" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc, true, false);
        assertEquals("", it.next().o1);
        assertEquals("", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIteratorWithEmptyIndents2() throws Exception {
        Document doc = new Document("" +
                "def m1:\n" +
                "\ta\n" +
                "\tpass\n" +
                "def m2:\n" +
                "    pass\n" +
                "");
        TabNannyDocIterator it = new TabNannyDocIterator(doc, true, false);
        assertEquals("", it.next().o1);
        assertEquals("\t", it.next().o1);
        assertEquals("\t", it.next().o1);
        assertEquals("", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertTrue(!it.hasNext());
    }

    public void testIterator3WithEmptyIndents() throws Exception {
        String str = "" +
                "d\n" +
                "    '''\r" +
                "    '''\r" +
                "\t" +
                "";
        Document d = new Document(str);
        TabNannyDocIterator it = new TabNannyDocIterator(d, true, false);
        assertEquals("", it.next().o1);
        assertEquals("    ", it.next().o1);
        assertEquals("\t", it.next().o1);
        assertTrue(!it.hasNext()); //no indentations here...
    }

}
