/**
 * Copyright (c) 2022 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.util.Iterator;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyParser311Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser311Test test = new PyParser311Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser311Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_11);
    }

    public void testMatchExceptionGroups() {
        String s = "try:\n"
                + "    pass\n"
                + "except* TypeError:\n"
                + "    pass\n"
                + "";

        SimpleNode ast = parseLegalDocStr(s);
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(ast, true);
        Iterator<ASTEntry> it = visitor.getIterator(TryExcept.class);
        ASTEntry entry = it.next();
        TryExcept t = (TryExcept) entry.node;
        assertTrue(t.handlers[0].isExceptionGroup);
    }

}
