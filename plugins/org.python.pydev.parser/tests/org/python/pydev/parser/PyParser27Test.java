/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * References:
 * http://docs.python.org/whatsnew/2.7.html
 * http://docs.python.org/2.7/reference/grammar.html?highlight=set%20grammar
 * http://docs.python.org/2.6/reference/grammar.html?highlight=set%20grammar
 */
public class PyParser27Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser27Test test = new PyParser27Test();
            test.setUp();
            test.testBom();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser27Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_7);
    }

    public void testWith() {
        String str = "def m1():\n" +
                "    with a, b, c:\n" +
                "        print a, b, c\n" +
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

    public void testSet() {
        String s = "" +
                "mutable_set = {1,2,3,4,5}\n" +
                "";

        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign assign = (Assign) m.body[0];
        Set set = (Set) assign.value;
        assertEquals(
                "Set[elts=[Num[n=1, type=Int, num=1], Num[n=2, type=Int, num=2], Num[n=3, type=Int, num=3], Num[n=4, type=Int, num=4], Num[n=5, type=Int, num=5]]]",
                set.toString());
    }

    public void testSetComp() {
        String s = "" +
                "mutable_set = {x for x in xrange(10)}\n" +
                "";

        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign assign = (Assign) m.body[0];
        assertTrue(assign.value instanceof SetComp);
    }

    public void testDictKept() {
        String s = "" +
                "d = {}\n" +
                "";

        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign assign = (Assign) m.body[0];
        assertTrue(assign.value instanceof Dict);
    }

    public void testDictComp() {
        String s = "" +
                "d = {i: i*2 for i in range(3)}\n" +
                "";

        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign assign = (Assign) m.body[0];
        assertTrue(assign.value instanceof DictComp);
    }

    public void testBom() throws BadLocationException, IOException {
        String base = "#comment\npass\n";
        String s = FileUtils.BOM_UTF8 + base;
        File file = new File(TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC
                +
                "/tests/org/python/pydev/parser/generated_data_test_utf8_with_bom.py");
        FileOutputStream out = new FileOutputStream(file);
        out.write(new String(FileUtils.BOM_UTF8).getBytes());
        out.write(base.getBytes());
        out.close();

        s = FileUtils.getFileContents(file);
        assertTrue(s.endsWith(base));
        assertTrue(s.startsWith(FileUtils.BOM_UTF8));

        assertEquals("utf-8", FileUtils.getPythonFileEncoding(file));
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Pass p = (Pass) m.body[0];

        assertTrue(s.startsWith(FileUtils.BOM_UTF8));

        ast = parseLegalDocStr(s);
        m = (Module) ast;
        p = (Pass) m.body[0];

    }

    @Override
    public void testEmpty() throws Throwable {
        String s = "";

        parseLegalDocStr(s);
    }

    public void testHugeIndentationLevels() throws Throwable {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("def m1():\n");
        for (int i = 1; i < 30; i++) {
            buf.appendN("  ", i);
            buf.append("if True:\n");
        }
        buf.appendN("  ", 30);
        buf.append("a=1\n");
        buf.append("\n");

        parseLegalDocStr(buf.toString());
    }

    public void testIllegal() throws Exception {
        String s = ""
                + "a = dict(\n"
                + " foo.bar = 1\n"
                + ")\n"
                + "";
        Throwable parseILegalDocStr = parseILegalDocStr(s);
        assertTrue(!parseILegalDocStr.toString().contains("ClassCastException"));
    }

}
