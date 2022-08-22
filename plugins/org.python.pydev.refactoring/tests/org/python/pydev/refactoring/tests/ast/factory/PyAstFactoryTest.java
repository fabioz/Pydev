/******************************************************************************
* Copyright (C) 2009-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.tests.ast.factory;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;

public class PyAstFactoryTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyAstFactoryTest test = new PyAstFactoryTest();
            test.setUp();
            test.testCreateOverrideBody5();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyAstFactoryTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    PyAstFactory astFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        astFactory = new PyAstFactory(new AdapterPrefs("\n", new IGrammarVersionProvider() {

            @Override
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
            }

            @Override
            public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
                return null;
            }
        }));
    }

    public void testPyAstFactory() throws Exception {
        FunctionDef functionDef = astFactory.createFunctionDef("MyMethod");

        String expected = "" +
                "def MyMethod():\n" +
                "    pass\n" +
                "";
        checkExpected(functionDef, expected);
    }

    public void testCreateSetter() throws Exception {
        String expected = "" +
                "def setFoo(self,value):\n" +
                "    self.__foo = value\n" +
                "";
        checkExpected(astFactory.createSetterFunctionDef("setFoo", "foo"), expected);
    }

    public void testCreateAttribute() throws Exception {

        Attribute attribute = astFactory.createAttribute("a.b.c.d.e");
        checkExpected(attribute, "a.b.c.d.e\n");
    }

    public void testCreateConstructor() throws Exception {
        String expected = "" +
                "def __init__(self,arg,attribute):\n" +
                "    A.__init__(self,arg)\n"
                +
                "    self.attribute = attribute\n" +
                "";

        FunctionDef functionDef = astFactory.createFunctionDef("__init__");
        functionDef.args = astFactory.createArguments(true, "arg", "attribute");
        astFactory.setBody(functionDef, astFactory.createCall("A.__init__", "self", "arg"), astFactory.createAssign(
                astFactory.createAttribute("self.attribute"), new Name("attribute", Name.Load, false)));
        checkExpected(functionDef, expected);
    }

    public void testCreateOverrideBody() throws Exception {
        String expected = "" +
                "def test(self,arg,attribute):\n" +
                "    Parent.test(self,arg,attribute)\n" +
                "";

        FunctionDef functionDef = astFactory.createFunctionDef("test");
        functionDef.args = astFactory.createArguments(true, "arg", "attribute");
        astFactory.setBody(functionDef, astFactory.createOverrideBody(functionDef, "Parent", "Current"));
        checkExpected(functionDef, expected);
    }

    public void testCreateOverrideBody2() throws Exception {
        String expected = "" +
                "def test(arg,attribute,*args,**kwargs):\n"
                +
                "    return Parent.test(arg,attribute,*args,**kwargs)\n" +
                "";

        Module module = (Module) parseLegalDocStr(expected);
        FunctionDef functionDef = (FunctionDef) module.body[0];
        FunctionDef createdFunctionDef = astFactory.createFunctionDef("test");
        createdFunctionDef.args = functionDef.args.createCopy();
        astFactory.setBody(createdFunctionDef, astFactory.createOverrideBody(functionDef, "Parent", "Current"));
        checkExpected(createdFunctionDef, expected);
    }

    public void testCreateOverrideBody3() throws Exception {
        String base = "" +
                "def test(arg,attribute,a=10,b=20,*args,**kwargs):\n" +
                "    pass\n" +
                "";

        String expected = "" +
                "def test(arg,attribute,a=10,b=20,*args,**kwargs):\n"
                +
                "    Parent.test(arg,attribute,a=a,b=b,*args,**kwargs)\n" +
                "";

        Module module = (Module) parseLegalDocStr(base);
        FunctionDef functionDef = (FunctionDef) module.body[0];
        FunctionDef createdFunctionDef = astFactory.createFunctionDef("test");
        createdFunctionDef.args = functionDef.args.createCopy();
        astFactory.setBody(createdFunctionDef, astFactory.createOverrideBody(functionDef, "Parent", "Current"));
        checkExpected(createdFunctionDef, expected);
    }

    public void testCreateOverrideBody4() throws Exception {
        String base = "" +
                "@classmethod\n" +
                "def test(cls):\n" +
                "    pass\n" +
                "";

        String expected = "" +
                "@classmethod\n" +
                "def test(cls):\n" +
                "    super(Current,cls).test()\n" +
                "";
        //        Module m = (Module) parseLegalDocStr(expected);
        //        FunctionDef func = (FunctionDef) m.body[0];
        //        System.out.println(func.body[0]);

        Module module = (Module) parseLegalDocStr(base);
        FunctionDef functionDef = (FunctionDef) module.body[0];
        FunctionDef createdFunctionDef = functionDef.createCopy();
        astFactory.setBody(createdFunctionDef, astFactory.createOverrideBody(functionDef, "Parent", "Current"));
        checkExpected(createdFunctionDef, expected);
    }

    public void testCreateOverrideBody5() throws Exception {
        String base = "" +
                "@classmethod\n" +
                "def test(cls):\n" +
                "    #comment\n" +
                "    pass\n" +
                "";

        String expected = "" +
                "@classmethod\n" +
                "def test(cls):\n" +
                "    super(Current,cls).test()\n" +
                "";

        Module module = (Module) parseLegalDocStr(base);
        FunctionDef functionDef = (FunctionDef) module.body[0];
        FunctionDef createdFunctionDef = functionDef.createCopy(false);
        astFactory.setBody(createdFunctionDef, astFactory.createOverrideBody(functionDef, "Parent", "Current"));
        checkExpected(createdFunctionDef, expected);
    }

    private void checkExpected(SimpleNode functionDef, String expected) throws Exception {
        MakeAstValidForPrettyPrintingVisitor.makeValid(functionDef);
        PrettyPrinterV2 printer = new PrettyPrinterV2(new PrettyPrinterPrefsV2("\n", "    ",
                new IGrammarVersionProvider() {

                    @Override
                    public int getGrammarVersion() throws MisconfigurationException {
                        return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
                    }

                    @Override
                    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                            throws MisconfigurationException {
                        return null;
                    }
                }));
        String result = printer.print(functionDef);
        System.out.println("Result: >>" + result +
                "<<");
        assertEquals(expected, result);
    }

}
