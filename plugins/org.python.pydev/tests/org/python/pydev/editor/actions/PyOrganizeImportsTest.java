/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 21, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyOrganizeImportsTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyOrganizeImportsTest test = new PyOrganizeImportsTest();
            test.setUp();
            test.testPerform11();
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
        ImportsPreferencesPage.groupImportsForTests = false;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        ImportsPreferencesPage.groupImportsForTests = true; //default
    }

    public void testPerform() {
        String d = "" +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import c\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import b\n" +
                "from a import c\n" +
                "from b import d\n" +
                "import a\n"
                +
                "import b\n" +
                "\n";

        assertEquals(result, doc.get());

    }

    public void testPerformWithGrouping() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import c\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import b, c\n" +
                "from b import d\n" +
                "import a\n" +
                "import b\n" +
                "\n";

        assertEquals(result, doc.get());

    }

    public void testPerformWithGroupingAndWild() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import *\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import *\n" +
                "from b import d\n" +
                "import a\n" +
                "import b\n" +
                "\n";

        //        System.out.println(doc.get());
        assertEquals(result, doc.get());

    }

    public void testPerformWithGroupingAndComments() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "import b #comment\n" +
                "import a\n" +
                "\n" +
                "from a import c #comment\n"
                +
                "from a import f #comment2\n" +
                "from a import e\n" +
                "from b import d\n" +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import b, e, c #comment\n" +
                "from a import f #comment2\n" +
                "from b import d\n"
                +
                "import a\n" +
                "import b #comment\n" +
                "\n";

        assertEquals(result, doc.get());
    }

    public void testPerformWithGroupingWithAs() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "from a import c as d\n" +
                "from a import f as g\n" +
                "import e as g\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import c as d, f as g\n" +
                "import e as g\n" +
                "";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWraps() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "from a import cccccccccccccccccccccccccccccccccccccccccccccccccc\n"
                + //50 * 'c'
                "from a import eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n"
                +
                "from a import ffffffffffffffffffffffffffffffffffffffffffffffffff";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import (cccccccccccccccccccccccccccccccccccccccccccccccccc, \n"
                +
                "    eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee, \n"
                +
                "    ffffffffffffffffffffffffffffffffffffffffffffffffff)\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWrapsLong() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = ""
                +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n"
                +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import ffffffffffffffffffffffffffffffffffffffffffffffffff";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import (\n"
                +
                "    eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee, \n"
                +
                "    ffffffffffffffffffffffffffffffffffffffffffffffffff)\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWraps3() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = ""
                +
                "from a import cccccccccc\n"
                + //10 * 'c'
                "from a import bbbbbbbbbb\n" +
                "from a import aaaaaaaaaa\n" +
                "from a import dddddddddd\n"
                +
                "from a import eeeeeeeeee\n" +
                "from a import ffffffffff\n" +
                "from a import gggggggggg\n"
                +
                "from a import hhhhhhhhhh\n" +
                "";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import (aaaaaaaaaa, bbbbbbbbbb, cccccccccc, dddddddddd, eeeeeeeeee, \n"
                +
                "    ffffffffff, gggggggggg, hhhhhhhhhh)\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWraps2() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "from a import cccccccccccccccccccccccccccccccccccccccccccccccccc\n"
                + //50 * 'c'
                "from a import eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee #comment 1\n"
                +
                "from a import ffffffffffffffffffffffffffffffffffffffffffffffffff #comment 2";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from a import (cccccccccccccccccccccccccccccccccccccccccccccccccc, \n"
                +
                "    eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee) #comment 1\n"
                +
                "from a import ffffffffffffffffffffffffffffffffffffffffffffffffff #comment 2\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWraps4() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = ""
                +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n"; //50 * 'c'

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import (\n"
                +
                "    eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee)\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerformGroupingWithWraps5() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = ""
                +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc \\\nimport eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n"; //50 * 'c'

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from cccccccccccccccccccccccccccccccccccccccccccccccccc import (\n"
                +
                "    eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee)\n";

        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(result, doc.get());
    }

    public void testPerform2() {

        String header = "" +
                "'''\n" +
                "from fff import xxx #ignore\n" +
                "import ggg #ignore\n"
                +
                "import aaa #ignore\n" +
                "'''\n";

        String d = "" + header +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import c\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" + header +
                "from a import b\n" +
                "from a import c\n" +
                "from b import d\n" +
                "import a\n"
                +
                "import b\n" +
                "\n";

        assertEquals(result, doc.get());

    }

    public void testPerform3() {

        String header = "" +
                "'''\n" +
                "from fff import xxx #ignore\n" +
                "import ggg #ignore\n"
                +
                "import aaa #ignore'''\n";

        String d = "" + header +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import c\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" + header +
                "from a import b\n" +
                "from a import c\n" +
                "from b import d\n" +
                "import a\n"
                +
                "import b\n" +
                "\n";

        assertEquals(result, doc.get());

    }

    public void testPerform4() {

        String header = "" +
                "'''ignore'''\n" +
                "from a import aaa\n";

        String d = "" + header +
                "import b\n" +
                "import a\n" +
                "\n" +
                "from a import c\n" +
                "from b import d\n"
                +
                "from a import b";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" + header +
                "from a import b\n" +
                "from a import c\n" +
                "from b import d\n" +
                "import a\n"
                +
                "import b\n" +
                "\n";

        assertEquals(result, doc.get());

    }

    public void testPerform5() {

        String d = "" +
                "import sys\n" +
                "from os import (pipe,\n" +
                "path)\n" +
                "import time\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from os import (pipe,\n" +
                "path)\n" +
                "import sys\n" +
                "import time\n";
        assertEquals(result, doc.get());

    }

    public void testPerform7() {

        String d = "" +
                "import sys\n" +
                "from ...os.path import pipe,\\\n" +
                "path\n" +
                "import time\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String result = "" +
                "from ...os.path import pipe,\\\n" +
                "path\n" +
                "import sys\n" +
                "import time\n";
        assertEquals(result, doc.get());

    }

    public void testPerform6() {

        String d = "" +
                "import sys #comment1\n" +
                "import sys2 #comment2\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        assertEquals(d, doc.get());
    }

    public void testPerform8() {
        String d = "" +
                "from __future__ import with_statement\n" + //the __future__ imports must always come first
                "from __a import b\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        assertEquals(d, doc.get());
    }

    public void testPerform9() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "from __future__ import division\n" + //the __future__ imports must always come first
                "from .backends.common import NoSuchObject\n";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        assertEquals(d, doc.get());
    }

    public void testPerform10() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "from a import b\n" +
                "from a import c ;something\n" +
                "from a import c\n" +
                "";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        String expected = "" +
                "from a import b, c\n" +
                "from a import c ;something\n" +
                "";
        assertEquals(expected, doc.get());
    }

    public void testPerform11() {
        ImportsPreferencesPage.groupImportsForTests = true;
        String d = "" +
                "a = 10; from a import b\n" +
                "from a import c ;something\n" +
                "";

        Document doc = new Document(d);
        PyOrganizeImports.performArrangeImports(doc, "\n", "    ");

        assertEquals(d, doc.get());
    }

    public void testPerformSort() {
        String s = "" +
                "line4\n" +
                "line1\n" +
                "line3\n" + //end the selection
                "line2\n";

        String result = "" +
                "line1\n" +
                "line3\n" +
                "line4\n" +
                "line2\n"; //not changed

        Document doc = new Document(s);
        PyOrganizeImports.performSimpleSort(doc, "\n", 0, 2);

        assertEquals(result, doc.get());
    }

    public void testPerformSort2() {
        //should take into account that lines ending with \ may not be sorted
        String s = "" +
                "line4\\\n" +
                "line1\n" +
                "line3\n" + //end the selection
                "line2\n";

        String result = "" +
                "line2\n" +
                "line3\n" +
                "line4\\\n" +
                "line1\n";

        Document doc = new Document(s);
        PyOrganizeImports.performSimpleSort(doc, "\n", 0, 3);

        assertEquals(result, doc.get());
    }

}
