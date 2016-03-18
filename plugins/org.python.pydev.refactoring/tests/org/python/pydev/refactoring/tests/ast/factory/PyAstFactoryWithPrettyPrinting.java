/******************************************************************************
* Copyright (C) 2009-2013  Fabio Zadrozny
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

import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.prettyprinter.AbstractPrettyPrinterTestBase;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyAstFactoryWithPrettyPrinting extends AbstractPrettyPrinterTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            PyAstFactoryWithPrettyPrinting test = new PyAstFactoryWithPrettyPrinting();
            test.setUp();
            test.testVarious22();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyAstFactoryWithPrettyPrinting.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testVarious22() throws Throwable {
        final String s = "" +
                "\n" +
                "\n" +
                "\n" +
                "[\n" +
                "    1, \n" +
                "    2,\n" +
                "    self.call(*a)\n" +
                "]\n"
                +
                "\n" +
                "";

        final String expected = "return [1,2,self.call(*a)]\n";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            @Override
            public Boolean call(Integer version) {
                Module module = (Module) parseLegalDocStr(s);
                exprType value = ((Expr) module.body[0]).value;
                Return node = new Return((exprType) value.createCopy());
                try {
                    MakeAstValidForPrettyPrintingVisitor.makeValid(node);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                String makePrint = makePrint(prefs, node);
                assertEquals(expected, makePrint);
                return true;
            }
        });
    }
}
