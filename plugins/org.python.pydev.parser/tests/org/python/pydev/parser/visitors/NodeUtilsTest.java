/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class NodeUtilsTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            NodeUtilsTest test = new NodeUtilsTest();
            test.setUp();
            test.testGetContextName2();
            test.tearDown();

            junit.textui.TestRunner.run(NodeUtilsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public void testFullRep() throws Exception {
        Module node = (Module) parseLegalDocStr("a.b.c.d");
        exprType attr = ((Expr) node.body[0]).value;
        assertEquals("a.b.c.d", NodeUtils.getFullRepresentationString(attr));
        exprType attr1 = NodeUtils.makeAttribute("a.b.c.d");
        assertEquals("a.b.c.d", NodeUtils.getFullRepresentationString(attr1));
        assertEquals(attr1.toString(), attr.toString());

        node = (Module) parseLegalDocStr("a.b.c.d()");
        exprType callAttr = NodeUtils.makeAttribute("a.b.c.d()");
        assertEquals(((Expr) node.body[0]).value.toString(), callAttr.toString());

        SimpleNode ast = parseLegalDocStr("print(a.b.c().d.__class__)");
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor
                .create(ast, false);

        Iterator<ASTEntry> iterator = visitor.getIterator();
        ASTEntry entry = null;
        int i = 0;
        while (entry == null || !(entry.node instanceof Attribute)) {
            i += 1;
            entry = iterator.next();
            if (i > 30) {
                throw new AssertionError("Didn't find Attribute iterating in: " + ast);
            }
        }
        assertEquals("a.b.c", NodeUtils.getFullRepresentationString(entry.node));

        visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr("'r.a.s.b'.join('a')"), false);
        iterator = visitor.getIterator();
        iterator.next(); //Module
        iterator.next(); //Expr
        entry = iterator.next(); //Attribute
        assertEquals("str.join", NodeUtils.getFullRepresentationString(entry.node));

        visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr("print(aa.bbb.cccc[comp.id].hasSimulate)"),
                false);
        iterator = visitor.getIterator();
        entry = null;
        while (entry == null || !(entry.node instanceof Attribute)) {
            entry = iterator.next();
        }
        assertEquals("aa.bbb.cccc", NodeUtils.getFullRepresentationString(entry.node));
    }

    public void testClassEndLine() {
        SimpleNode ast1 = parseLegalDocStr("" +
                "class env:\n" +
                "    pass\n" +
                "\n" +
                "#comment\n");

        checkEndLine(ast1, 4);

        ast1 = parseLegalDocStr("" +
                "class env:\n" +
                "    pass\n" +
                "\n" +
                "if True:\n" +
                "    pass\n" +
                "#comment\n");

        checkEndLine(ast1, 2);
    }

    public void testGetContextName() {
        SimpleNode ast1 = parseLegalDocStr("" +
                "class env:\n" +
                "    pass\n" +
                "\n" +
                "if __name__ == '__main__':\n"
                +
                "    print('step 1')\n" +
                "\n");

        SimpleNode ast2 = parseLegalDocStr("" +
                "class env:\n" +
                "    pass\n" +
                "\n" +
                "if __name__ == '__main__':\n"
                +
                "    print('step 1')\n" +
                "\n" +
                "#comment");

        checkEndLine(ast1, 2);
        checkEndLine(ast2, 2);

        assertEquals(null, NodeUtils.getContextName(4, ast1));
        assertEquals(null, NodeUtils.getContextName(4, ast2));
    }

    public void testGetContextName2() {
        SimpleNode ast = parseLegalDocStr("" +
                "class Simple(object):\n" +
                "  def m1(self):\n" +
                "    a = 10\n"
                +
                "    i = 20\n" +
                "    print('here')\n" +
                "  \n" +
                "if __name__ == '__main__':\n" +
                "  Simple().m1()\n"
                +
                "\n");

        assertEquals("Simple.m1", NodeUtils.getContextName(4, ast));
    }

    public void testIsValidContextForSetNext() {
        SimpleNode ast = parseLegalDocStr("" +
                "class Simple(object): \n" +
                "	def m1(self): \n" +
                "		a = 10 \n"
                +
                "		i = 20 \n" +
                "		for i in range(3):  \n" +
                "			print('here in for')  \n"
                +
                "			for j in range(3):  \n" +
                "				print('here in nested for')  \n" +
                "		x = 1  \n"
                +
                "		print('m1 Ends Here') \n" +
                " \n" +
                "	def m2(self): \n" +
                "	 	print('method m2 started') \n"
                +
                "		i = 0  \n" +
                "		try:  \n" +
                "			print('inside try')  \n" +
                "			while i < 5:  \n" +
                "				i += 1  \n"
                +
                "		except:  \n" +
                "			print('inside exception')  \n" +
                "		a = 30 \n" +
                "		i = 40 \n"
                +
                "		print('here') \n" +
                " \n" +
                "firstName = 'Hussain'  \n" +
                "lastName = 'Bohra'  \n"
                +
                "print('%s, %s in Global Context'%(lastName, firstName))  \n" +
                "if __name__ == '__main__': \n"
                +
                "	Simple().m1() \n");

        // Source And Target are in Same Method
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 3, 4));
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 10, 8));
        // Source And Target are in Different Method
        assertFalse(NodeUtils.isValidContextForSetNext(ast, 4, 16));
        // Source And Target are in Same Method. Target is inside For/While/Try..Except/Try..Finally
        assertFalse(NodeUtils.isValidContextForSetNext(ast, 4, 7));
        assertFalse(NodeUtils.isValidContextForSetNext(ast, 13, 18));
        assertFalse(NodeUtils.isValidContextForSetNext(ast, 15, 17));
        // Source And Target are in Same Method. Source is inside For/While/Try..Except/Try..Finally
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 7, 4));
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 18, 13));
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 17, 15));
        // Source And Target are in Global Context
        assertTrue(NodeUtils.isValidContextForSetNext(ast, 25, 26));
    }

    private void checkEndLine(SimpleNode ast1, int endLine) {
        EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast1);

        List<ASTEntry> classes = visitor.getClassesAndMethodsList();
        assertEquals(1, classes.size());
        assertEquals(endLine, classes.get(0).endLine);
    }

    public void testFindStmtForNode() throws Exception {
        Module ast = (Module) parseLegalDocStr("a=10;b=20;c=30");
        Assign assign = (Assign) ast.body[1];
        Name b = (Name) assign.targets[0];
        assertSame(assign, NodeUtils.findStmtForNode(ast, b));

        ast = (Module) parseLegalDocStr("a=10\nb=20\nc=30");
        assign = (Assign) ast.body[1];
        b = (Name) assign.targets[0];
        assertSame(assign, NodeUtils.findStmtForNode(ast, b));
    }

    public void testIsAfterDeclarationStart() throws Exception {
        checkWithAllGrammars((grammarVersion) -> {
            Module ast = (Module) parseLegalDocStr(""
                    + "def method():\n"
                    + "    #comment\n"
                    + "    a = 1");
            FunctionDef funcDef = (FunctionDef) ast.body[0];
            assertEquals(false, NodeUtils.isAfterDeclarationStart(funcDef, 0, 1));
            assertEquals(false, NodeUtils.isAfterDeclarationStart(funcDef, 1, 1));
            assertEquals(true, NodeUtils.isAfterDeclarationStart(funcDef, 3, 1));
            assertEquals(true, NodeUtils.isAfterDeclarationStart(funcDef, 2, 1)); // in comment line
            return true;
        });
    }
}
