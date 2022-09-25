/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 10, 2006
 * @author Fabio
 */
package org.python.pydev.ast.codecompletion.revisited.visitors;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.shared_core.model.ISimpleNode;

public class FindScopeVisitorTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            FindScopeVisitorTest test = new FindScopeVisitorTest();
            test.setUp();
            test.testFindAssertInLocalScope2();
            test.tearDown();
            junit.textui.TestRunner.run(FindScopeVisitorTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method tries to find a local scope given some code, the current line and col
     *
     * @param s the code
     * @param line starts at 1
     * @param col starts at 1
     * @return the local scope found
     */
    private ILocalScope findLocalScope(String s, int line, int col) {
        SimpleNode ast = parseLegalDocStr(s);
        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col, null, null);
        if (ast != null) {
            try {
                ast.accept(scopeVisitor);
            } catch (Exception e) {
                Log.log(e);
            }
        }
        ILocalScope localScope = scopeVisitor.scope;
        return localScope;
    }

    public void testFindLocalScope() throws Exception {
        String s = "" +
                "#file mod3.py \n" + //line = 1 (in ast)
                "class SomeA(object):\n" +
                "    def fun(self):\n" +
                "        pass\n" +
                "    \n" +
                "class C1(object):\n" +
                "  a = SomeA() #yes, these are class-defined\n" +
                "  \n" + // line 8
                "  def someFunct(self):\n" +
                "      pass\n" +
                "    \n" +
                "\n" +
                "";
        ILocalScope localScope = findLocalScope(s, 8, 3);
        assertTrue(localScope.getClassDef() != null);
    }

    public void testFindLocalScope2() throws Exception {
        String s = "" +
                "def method():\n" +
                "    a = 1\n" +
                "";
        ILocalScope localScope = findLocalScope(s, 2, 2);
        Iterator<ISimpleNode> iterator = localScope.iterator();
        assertTrue(iterator.next() instanceof FunctionDef);
        assertTrue(iterator.next() instanceof org.python.pydev.parser.jython.ast.Module);
        assertTrue(!iterator.hasNext());
    }

    public void testFindLocalScope3() throws Exception {
        String s = "def func(arg, *, arg2=None):\n" +
                "    ar" +
                "";
        ILocalScope localScope = findLocalScope(s, 2, 6);
        Iterator<ISimpleNode> iterator = localScope.iterator();
        assertTrue(iterator.next() instanceof FunctionDef);
        assertTrue(iterator.next() instanceof org.python.pydev.parser.jython.ast.Module);
        assertTrue(!iterator.hasNext());
    }

    public void testFindLocalScope5() throws Exception {
        String s = "class A:\n" +
                "    def method1(self, *args, **kwargs):\n" +
                "        pass";
        ILocalScope localScope = findLocalScope(s, 3, 8);
        Iterator<ISimpleNode> iterator = localScope.iterator();
        assertTrue("Found: " + localScope, iterator.next() instanceof FunctionDef);
        assertTrue(iterator.next() instanceof ClassDef);
        assertTrue(iterator.next() instanceof org.python.pydev.parser.jython.ast.Module);
        assertTrue(!iterator.hasNext());
    }

    public void testFindLocalScope4() throws Exception {
        String s;
        s = ""
                + "class F:\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "x = dict(((i, str(i)) for i in [F(1), F(2)]))\n"
                + "for k, v in x.iteritems():\n"
                + "    k"
                + "";
        ILocalScope localScope = findLocalScope(s, 6, 6);
        Iterator<ISimpleNode> iterator = localScope.iterator();
        assertTrue(iterator.next() instanceof org.python.pydev.parser.jython.ast.Module);
        assertTrue(!iterator.hasNext());
    }

    public void testFindAssertInLocalScope() throws Exception {
        String s = "def m1(a):\n" +
                "    assert isinstance(a, str)\n" +
                "    ";

        ILocalScope localScope = findLocalScope(s, 2, 1);
        List<ITypeInfo> found = localScope.getPossibleClassesForActivationToken("a");
        assertEquals(1, found.size());
        assertEquals("str", found.get(0).getActTok());
    }

    public void testFindAssertInLocalScope2() throws Exception {
        String s = "def m1(a):\n" +
                "    assert isinstance(a, (list, tuple))\n" +
                "    ";

        ILocalScope localScope = findLocalScope(s, 2, 1);
        List<ITypeInfo> found = localScope.getPossibleClassesForActivationToken("a");
        assertEquals(2, found.size());
        assertEquals("list", found.get(0).getActTok());
        assertEquals("tuple", found.get(1).getActTok());
    }
}
