/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename.visitors;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;

public class FindCallVisitorTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            //            FindCallVisitorTest test = new FindCallVisitorTest();
            //            test.setUp();
            //            test.testRename1();
            //            test.tearDown();

            junit.textui.TestRunner.run(FindCallVisitorTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testFindCallVisitor() throws Exception {
        String s = "" +
                "from methoddef import Method1\n" +
                "Method1(10, param2=20)\n" +
                "Method1(param1=10, param2=20)\n" +
                "";
        Module root = (Module) parseLegalDocStr(s);
        Expr expr = (Expr) root.body[1];
        Call call = (Call) expr.value;
        Name name = (Name) call.func;
        FindCallVisitor visitor = new FindCallVisitor(name);
        visitor.traverse(root);
        assertSame(call, visitor.getCall());
        assertSame(call, FindCallVisitor.findCall(name, root));
    }

    public void testFindCallVisitor2() throws Exception {
        String s = "" +
                "class c:\n" +
                "    def m(self):\n" +
                "        Method1(10, param2=20)\n" +
                "        Method1(param1=10, param2=20)\n" +
                "";
        Module root = (Module) parseLegalDocStr(s);
        ClassDef classDef = (ClassDef) root.body[0];
        FunctionDef funcDef = (FunctionDef) classDef.body[0];
        Expr expr = (Expr) funcDef.body[1];
        Call call = (Call) expr.value;
        Name name = (Name) call.func;
        FindCallVisitor visitor = new FindCallVisitor(name);
        visitor.traverse(root);
        assertSame(call, visitor.getCall());
        assertSame(call, FindCallVisitor.findCall(name, root));
    }
}
