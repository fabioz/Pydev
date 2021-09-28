/**
 * Copyright (c) 2020 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyParser39Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser39Test test = new PyParser39Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser39Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_9);
    }

    public void testDecorator1() {
        SimpleNode node = parseLegalDocStr("buttons = []\n" +
                "\n" +
                "@buttons[0].clicked().connect()\n" +
                "def eggs():\n" +
                "    pass");
        assertTrue(node instanceof Module);

        Module m = (Module) node;
        assertEquals(2, m.body.length);

        assertTrue(m.body[0] instanceof Assign);
        Assign a = (Assign) m.body[0];
        assertTrue(a.value instanceof org.python.pydev.parser.jython.ast.List);

        assertTrue(m.body[1] instanceof FunctionDef);
        FunctionDef f = (FunctionDef) m.body[1];

        assertTrue(f.decs != null);
        assertEquals(1, f.decs.length);

        decoratorsType dec = f.decs[0];
        exprType it = dec.func;

        boolean valid = false;
        // go through the decorator and search for the buttons Subscript
        // i.e.; search for `buttons[0]` in decorator
        while (true) {
            if (it == null) {
                break;
            }
            if (it instanceof Attribute) {
                it = ((Attribute) it).value;
            } else if (it instanceof Call) {
                it = ((Call) it).func;
            } else if (it instanceof Subscript) {
                if ("buttons".equals(NodeUtils.getFullRepresentationString(it))) {
                    valid = true;
                }
                break;
            } else {
                break;
            }
        }
        assertTrue(valid);
    }
}
