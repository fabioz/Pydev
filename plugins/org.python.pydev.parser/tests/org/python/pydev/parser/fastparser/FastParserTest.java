/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Note: tests don't have the correct syntax on purpose!
 *
 * @author Fabio
 */
public class FastParserTest extends TestCase {

    public static void main(String[] args) {
        try {
            FastParserTest test = new FastParserTest();
            test.setUp();
            test.testCython1();
            test.tearDown();
            junit.textui.TestRunner.run(FastParserTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testGettingClassOrFunc() throws Exception {
        Document doc = new Document();
        doc.set("def bar(a):\n" +
                "\n" +
                "class \\\n" +
                "  Bar\n" +
                "    def mm\n" +
                "def\n" + //no space after
                "class\n" + //no space after
                "class \n" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(4, all.size());
        check(all, 0, 1, 1, 1, 5);
        check(all, 1, 3, 1, 3, 7);
        check(all, 2, 5, 5, 5, 9);
        check(all, 3, 8, 1, 8, 7);
    }

    public void testGettingClass() throws Exception {
        Document doc = new Document();
        doc.set("class Foo:\n" +
                "\n" +
                "class Bar(object):\n" +
                "\n" +
                "    class My\n" +
                "'''class Dont\n"
                +
                "class Dont2\n" +
                "\n" +
                "'''\n" +
                "class My2:\n" +
                "" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(4, all.size());
        check(all, 0, 1, 1, 1, 7);
        check(all, 1, 3, 1, 3, 7);
        check(all, 2, 5, 5, 5, 11);
        check(all, 3, 10, 1, 10, 7);

        stmtType found = FastParser.firstClassOrFunction(doc, 1, true, false);
        checkNode(3, 1, 3, 7, (ClassDef) found);

        found = FastParser.firstClassOrFunction(doc, 0, true, false);
        checkNode(1, 1, 1, 7, (ClassDef) found);

        found = FastParser.firstClassOrFunction(doc, 5, true, false);
        checkNode(10, 1, 10, 7, (ClassDef) found);

        found = FastParser.firstClassOrFunction(doc, 5, false, false);
        checkNode(5, 5, 5, 11, (ClassDef) found);

        found = FastParser.firstClassOrFunction(doc, -1, false, false);
        assertNull(found);

        found = FastParser.firstClassOrFunction(doc, 15, true, false);
        assertNull(found);

        found = FastParser.firstClassOrFunction(doc, 15, false, false);
        checkNode(10, 1, 10, 7, (ClassDef) found);

    }

    public void testGettingClass2() throws Exception {
        Document doc = new Document();
        doc.set("def GetClassesAndData():\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }

    public void testGettingClass3() throws Exception {
        Document doc = new Document();
        doc.set("class A(object):\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }

    public void testGettingClass4() throws Exception {
        Document doc = new Document();
        doc.set("\nclass A(object):\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }

    public void testGettingMethod() throws Exception {
        Document doc = new Document();
        doc.set("def a():\n" +
                "    curr_widget_def = 10\n" +
                "\n" +
                "" +
                "");

        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }

    public void testBackwardsUntil1stGlobal() throws Exception {
        Document doc = new Document();
        doc.set("def b():\n" +
                "    pass\n" +
                "\n" +
                "def a():\n" +
                "    curr_widget_def = 10\n" +
                "\n" +
                "" +
                "");

        List<stmtType> stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, 4);
        assertEquals(1, stmts.size());
        assertEquals("a", NodeUtils.getRepresentationString(stmts.get(0)));
    }

    public void testCython1() throws Exception {
        Document doc = new Document();
        doc.set("cdef extern int f1()\n" +
                "" +
                "");
        List<stmtType> stmts = FastParser.parseCython(doc);
        assertEquals(1, stmts.size());
        assertEquals("extern int f1()", NodeUtils.getRepresentationString(stmts.get(0)));
    }

    public void testCython2() throws Exception {
        Document doc = new Document();
        doc.set("ctypedef enum parrot_state:\n" +
                "" +
                "");
        List<stmtType> stmts = FastParser.parseCython(doc);
        assertEquals(1, stmts.size());
        assertEquals("enum parrot_state:", NodeUtils.getRepresentationString(stmts.get(0)));
    }

    public void testBackwardsUntil1stGlobal2() throws Exception {
        Document doc = new Document();
        doc.set("def b():\n" +
                "    pass\n" +
                "\n" +
                "def a():\n" +
                "    def f():\n" + //4
                "        curr_widget_def = 10\n" + //5
                "    def c():\n" + //6
                "        curr_widget_def = 10\n" + //7
                "    a = 10\n" + //8
                "    print a" + //9
                "" +
                "");

        List<stmtType> stmts;

        for (int i = 8; i <= 9; i++) {
            //8 and 9
            stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, i);
            assertEquals(1, stmts.size());
            assertEquals("a", NodeUtils.getRepresentationString(stmts.get(0)));
        }

        for (int i = 6; i <= 7; i++) {
            //6 and 7
            stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, i);
            assertEquals(2, stmts.size());
            assertEquals("a", NodeUtils.getRepresentationString(stmts.get(0)));
            assertEquals("c", NodeUtils.getRepresentationString(stmts.get(1)));
        }

        for (int i = 4; i <= 5; i++) {
            //4 and 5
            stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, i);
            assertEquals(2, stmts.size());
            assertEquals("a", NodeUtils.getRepresentationString(stmts.get(0)));
            assertEquals("f", NodeUtils.getRepresentationString(stmts.get(1)));
        }

        //3
        stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, 3);
        assertEquals(1, stmts.size());
        assertEquals("a", NodeUtils.getRepresentationString(stmts.get(0)));

        for (int i = 0; i <= 2; i++) {
            //2, 1 and 0
            stmts = FastParser.parseToKnowGloballyAccessiblePath(doc, i);
            assertEquals(1, stmts.size());
            assertEquals("b", NodeUtils.getRepresentationString(stmts.get(0)));
        }
    }

    private void check(List<stmtType> all, int position, int classBeginLine, int classBeginCol, int nameBeginLine,
            int nameBeginCol) {
        SimpleNode node = all.get(position);
        checkNode(classBeginLine, classBeginCol, nameBeginLine, nameBeginCol, node);
    }

    private void checkNode(int classBeginLine, int classBeginCol, int nameBeginLine, int nameBeginCol, SimpleNode node) {
        assertEquals(classBeginLine, node.beginLine);
        assertEquals(classBeginCol, node.beginColumn);

        SimpleNode name = NodeUtils.getNameTokFromNode(node);
        assertEquals(nameBeginLine, name.beginLine);
        assertEquals(nameBeginCol, name.beginColumn);
    }

}
