/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyParser26Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser26Test test = new PyParser26Test();
            test.setUp();
            test.testBinNumber();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser26Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_6);
    }

    public void testWith() {
        String str = "def m1():\n" +
                "    with a:\n" +
                "        print a\n" +
                "\n" +
                "";
        parseLegalDocStr(str);
    }

    public void testExceptAs() {
        String str = "" +
                "try:\n" +
                "    a = 10\n" +
                "except RuntimeError as x:\n" +
                "    print x\n" +
                "";
        parseLegalDocStr(str);
    }

    public void testBinaryObj() {
        String str = "" +
                "b'foo'\n" +
                "";
        parseLegalDocStr(str);
    }

    public void testOctal() {
        String str = "" +
                "0o700\n" +
                "0700\n" +
                "";
        assertEquals(
                "Module[body=[Expr[value=Num[n=448, type=Int, num=0o700]], Expr[value=Num[n=448, type=Int, num=0700]]]]",
                parseLegalDocStr(str).toString());
    }

    public void testFunctionCall() {
        String str = "" +
                "Call(1,2,3, *(4,5,6), keyword=13, **kwargs)\n" +
                "";
        parseLegalDocStr(str);
    }

    public void testFunctionCallWithListComp() {
        String str = "" +
                "any(cls.__subclasscheck__(c) for c in [subclass, subtype])\n" +
                "";
        parseLegalDocStr(str);
    }

    public void testClassDecorator() {
        String s = "" +
                "@classdec\n" +
                "@classdec2\n" +
                "class A:\n" +
                "    pass\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        ClassDef d = (ClassDef) m.body[0];
        assertEquals(2, d.decs.length);
        assertEquals("classdec", NodeUtils.getRepresentationString(d.decs[0].func));
        assertEquals("classdec2", NodeUtils.getRepresentationString(d.decs[1].func));
    }

    public void testCall() {
        String s = "fubar(*list, x=4)";

        parseLegalDocStr(s);
    }

    public void testCall2() {
        String s = "fubar(1, *list, x=4)";

        parseLegalDocStr(s);
    }

    public void testFuturePrintFunction() {
        String s = "" +
                "from __future__ import print_function\n" +
                "print('test', 'print function', sep=' - ')\n" +
                "";

        parseLegalDocStr(s);
    }

    public void testBinNumber() {
        String s = "" +
                "0b00010\n" +
                "0B00010\n" +
                "0b00010L\n" +
                "0B00010l\n" +
                "";

        parseLegalDocStr(s);
    }

    @Override
    public void testEmpty() throws Throwable {
        String s = "";

        parseLegalDocStr(s);
    }

}
