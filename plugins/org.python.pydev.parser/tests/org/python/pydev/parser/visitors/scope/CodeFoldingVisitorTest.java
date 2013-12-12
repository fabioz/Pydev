/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public class CodeFoldingVisitorTest extends TestCase {

    public static void main(String[] args) {
        try {
            CodeFoldingVisitorTest test = new CodeFoldingVisitorTest();
            test.setUp();
            test.testTryFinallyVersion25();
            test.tearDown();
            junit.textui.TestRunner.run(CodeFoldingVisitorTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testIfElifElse() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();
        String str = "if a:\n" +
                "    print 1\n" +
                "elif b:\n" +
                "    print 2\n" +
                "else:\n" +
                "    print 3\n" +
                "\n";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "If", 1, 1, 2, 0);
        check((ASTEntryWithChildren) iterator.next(), "If", 1, 3, 4, 0);
        check((ASTEntryWithChildren) iterator.next(), "If", 1, 5, 6, 0);
        assertTrue(iterator.hasNext() == false);

    }

    public void testIf() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "if a:\n" +
                "    print 1\n" +
                "    if b:\n" +
                "        print 2\n" +
                "    elif c:\n"
                +
                "        print 3\n" +
                "    else:\n" +
                "        print 4\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();

        ASTEntryWithChildren element = (ASTEntryWithChildren) iterator.next();
        check(element, "If", 1, 1, 8, 3);

        assertTrue(iterator.hasNext() == false);
        Iterator<ASTEntryWithChildren> iterator2 = element.children.iterator();

        check(iterator2.next(), "If", 5, 3, 4, 0);
        check(iterator2.next(), "If", 5, 5, 6, 0);
        check(iterator2.next(), "If", 1, 7, 8, 0);
    }

    public void testWith() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "from __future__ import with_statement\n" +
                "with a:\n" +
                "    print a\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_5));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "from __future__ import with_statement", 6, 1, 1, 0);
        check((ASTEntryWithChildren) iterator.next(), "With", 1, 2, 3, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testFor() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "for a in b:\n" +
                "    print 4\n" +
                "else:\n" +
                "    print 5\n" +
                "\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "For", 1, 1, 4, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testImport() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "from a import b\n" +
                "import b\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "from a import b", 6, 1, 1, 0);
        check((ASTEntryWithChildren) iterator.next(), "import b", 8, 2, 2, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testWhile() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "while True:\n" +
                "    print 4\n" +
                "else:\n" +
                "    print 5\n" +
                "\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "While", 1, 1, 4, 0);
        assertTrue(iterator.hasNext() == false);

    }

    public void testTry() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "try:\n" +
                "    print 4\n" +
                "except:\n" +
                "    print 5\n" +
                "\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "TryExcept", 1, 1, 4, 0);
        assertTrue(iterator.hasNext() == false);

    }

    public void testTryFinally() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "try:\n" +
                "    print 4\n" +
                "finally:\n" +
                "    print 5\n" +
                "\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_4));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "TryFinally", 1, 1, 4, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testTryFinallyVersion25() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "try:\n" +
                "    print 4\n" +
                "finally:\n" +
                "    print 5\n" +
                "\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_5));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "TryFinally", 1, 1, 4, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testString() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "'''\n" +
                "test\n" +
                "'''\n" +
                "";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_5));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        check((ASTEntryWithChildren) iterator.next(), "Str", 1, 1, 3, 0);
        assertTrue(iterator.hasNext() == false);
    }

    public void testTryFinally2() throws Exception {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();

        String str = "" +
                "def foo():\n" +
                "    try:\n" +
                "        pass\n" +
                "    except(Exception):\n"
                +
                "        pass\n" +
                "    finally:\n" +
                "        pass\n" +
                "            \n" +
                "    try:\n"
                +
                "        pass\n" +
                "    finally:\n" +
                "        pass\n" +
                "\n";
        ParseOutput objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str),
                IPythonNature.GRAMMAR_PYTHON_VERSION_2_5));
        SimpleNode root = (SimpleNode) objects.ast;
        root.accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator();
        ASTEntry method = iterator.next();
        check((ASTEntryWithChildren) method, "foo", 1, 1, 12, 2);
        List<ASTEntryWithChildren> children = ((ASTEntryWithChildren) method).children;
        ASTEntryWithChildren try1 = children.get(0);
        ASTEntryWithChildren try2 = children.get(1);
        ASTEntryWithChildren innerTry = try1.children.get(0);

        check(try1, "TryFinally", 5, 2, 7, 1);
        check(innerTry, "TryExcept", 5, 2, 5, 0);
        check(try2, "TryFinally", 5, 9, 12, 0);
        assertEquals(2, children.size());
        assertTrue(iterator.hasNext() == false);

    }

    private void check(ASTEntryWithChildren entry, String name, int col, int begLine, int endLine, int children) {
        assertEquals(name, entry.getName());
        assertEquals(col, entry.node.beginColumn);
        assertEquals(begLine, entry.node.beginLine);
        assertEquals(endLine, entry.endLine);
        if (children == 0 && entry.children == null) {
            return; //ok, null means 0 children
        }
        assertEquals(children, entry.children.size());
    }

}
