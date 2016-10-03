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
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.List;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;

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
        FindScopeVisitor scopeVisitor = new FindScopeVisitor(line, col, null);
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
        String s = ""
                +
                "#file mod3.py \n"
                + //line = 1 (in ast)
                "class SomeA(object):\n" +
                "    def fun(self):\n" +
                "        pass\n" +
                "    \n" +
                "class C1(object):\n"
                +
                "  a = SomeA() #yes, these are class-defined\n" +
                "  \n" +
                "  def someFunct(self):\n"
                +
                "      pass\n" +
                "    \n" +
                "\n" +
                "";
        ILocalScope localScope = findLocalScope(s, 8, 3);
        assertTrue(localScope.getClassDef() != null);
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
