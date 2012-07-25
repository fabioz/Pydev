/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

/**
 * @author Fabio
 */
public class EasyASTIteratorTest extends TestCase {

    public static void main(String[] args) {
        EasyASTIteratorTest test = new EasyASTIteratorTest();
        try {
            test.setUp();
            test.testDecorator();
            test.tearDown();

            junit.textui.TestRunner.run(EasyASTIteratorTest.class);
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

    /**
     * @throws Exception
     * 
     */
    public void testClassesMethods() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "class C:\n" +
                "    def met1(self):pass\n" +
                "\n" +
                "if True:\n" +
                "    print 't'\n" +
                "\n"
                +
                "class D:\n" +
                "    pass\n" +
                "class E:\n" +
                "    '''t1\n" +
                "    t2\n" +
                "    '''\n" +
                "c = C()\n"
                +
                "";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 2);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 2);
        check((ASTEntry) iterator.next(), "D", 1, 7, 8);
        check((ASTEntry) iterator.next(), "E", 1, 9, 12);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testMultiline() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "class C:        \n" +
                "    def d(self):\n" +
                "        c = \\\n" +
                "'''             \n"
                +
                "a               \n" +
                "b               \n" +
                "c               \n" +
                "'''             \n";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 8);
        check((ASTEntry) iterator.next(), "d", 5, 2, 8);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testMultiline2() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "class C:          \n" +
                "    def d(self):  \n" +
                "        c = '''   \n"
                +
                "                  \n" +
                "c                 \n" +
                "'''               \n" +
                "                  \n"
                +
                "class E:          \n" +
                "    '''t1         \n" +
                "    t2            \n" +
                "    '''           \n";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "d", 5, 2, 6);
        check((ASTEntry) iterator.next(), "E", 1, 8, 11);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testImports() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "import test.lib\n" +
                "from test.lib import test\n" +
                "from test.lib import *\n"
                +
                "from test.lib import test as alias\n" +
                "";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "import test.lib", 8, 1, 1);
        check((ASTEntry) iterator.next(), "from test.lib import test", 6, 2, 2);
        check((ASTEntry) iterator.next(), "from test.lib import *", 6, 3, 3);
        check((ASTEntry) iterator.next(), "from test.lib import test as alias", 6, 4, 4);
        assertFalse(iterator.hasNext());
    }

    public void testDecorator() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "class D:\n" +
                "    @foo\n" +
                "    def mmm(self):\n" +
                "        pass\n" +
                "\n" +
                "\n";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        if (objects.o2 != null) {
            throw new RuntimeException(objects.o2);
        }
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "D", 1, 1, 4);
        check((ASTEntry) iterator.next(), "mmm", 5, 2, 4);
        assertFalse(iterator.hasNext());

    }

    /**
     * @throws Exception
     * 
     */
    public void testAttributes() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
                "class C:\n" +
                "    def met1(self):\n" +
                "        self.attr1=1\n" +
                "        self.attr2=2\n"
                +
                "\n" +
                "    classAttr = 10\n" +
                "pass";

        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = objects.o1;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        check((ASTEntry) iterator.next(), "attr1", 14, 3, 3);
        check((ASTEntry) iterator.next(), "attr2", 14, 4, 4);
        check((ASTEntry) iterator.next(), "classAttr", 5, 6, 6);
        assertFalse(iterator.hasNext());

        iterator = visitor.getClassesIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        assertFalse(iterator.hasNext());

        iterator = visitor.getClassesAndMethodsIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        assertFalse(iterator.hasNext());

        iterator = visitor.getIterator(ClassDef.class);
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        assertFalse(iterator.hasNext());

        iterator = visitor.getIterator(new Class[] { ClassDef.class, FunctionDef.class });
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        assertFalse(iterator.hasNext());
    }

    private void check(ASTEntry entry, String name, int col, int begLine, int endLine) {
        assertEquals(name, entry.getName());
        assertEquals(col, entry.node.beginColumn);
        assertEquals(begLine, entry.node.beginLine);
        assertEquals(endLine, entry.endLine);
    }

}
