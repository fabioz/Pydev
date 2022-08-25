/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.decoratorsType;
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
    }

    public void testWith() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "def m1():\n" +
                    "    with a:\n" +
                    "        print(a)\n" +
                    "\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });
    }

    public void testExceptAs() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "try:\n" +
                    "    a = 10\n" +
                    "except RuntimeError as x:\n" +
                    "    print(x)\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });
    }

    public void testBinaryObj() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "b'foo'\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });
    }

    public void testOctal() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "0o700\n" +
                    "";
            assertEquals(
                    "Module[body=[Expr[value=Num[n=448, type=Int, num=0o700]]]]",
                    parseLegalDocStr(str).toString());
            return true;
        });
    }

    public void testFunctionCall() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "Call(1,2,3, *(4,5,6), keyword=13, **kwargs)\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });
    }

    public void testFunctionCallWithListComp() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "any(cls.__subclasscheck__(c) for c in [subclass, subtype])\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });
    }

    public void testClassDecorator() {
        checkWithAllGrammars((grammarVersion) -> {
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
            assertFalse(d.decs[0].isCall);
            assertFalse(d.decs[1].isCall);
            return true;
        });
    }

    public void testClassDecoratorCall() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "" +
                    "@classdec(1, 2)\n" +
                    "class A:\n" +
                    "    pass\n" +
                    "";
            SimpleNode ast = parseLegalDocStr(s);
            Module m = (Module) ast;
            ClassDef d = (ClassDef) m.body[0];
            assertEquals(1, d.decs.length);
            decoratorsType dec = d.decs[0];
            assertEquals("classdec", NodeUtils.getRepresentationString(dec.func));
            assertTrue(dec.isCall);
            assertEquals("1", ((Num) dec.args[0]).num);
            assertEquals("2", ((Num) dec.args[1]).num);
            return true;
        });
    }

    public void testCall() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "fubar(*list, x=4)";

            parseLegalDocStr(s);
            return true;
        });
    }

    public void testCall2() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "fubar(1, *list, x=4)";

            parseLegalDocStr(s);
            return true;
        });
    }

    public void testFuturePrintFunction() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "" +
                    "from __future__ import print_function\n" +
                    "print('test', 'print function', sep=' - ')\n" +
                    "";

            parseLegalDocStr(s);
            return true;
        });
    }

    public void testBinNumber() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "" +
                    "0b00010\n" +
                    "0B00010\n" +
                    "0b00010L\n" +
                    "0B00010l\n" +
                    "";

            parseLegalDocStr(s);
            return true;
        });
    }

    @Override
    public void testEmpty() throws Throwable {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "";

            parseLegalDocStr(s);
            return true;
        });
    }

}
