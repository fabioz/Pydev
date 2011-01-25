/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Module;
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
        exprType attr = ((Expr)node.body[0]).value;
        assertEquals("a.b.c.d", NodeUtils.getFullRepresentationString(attr));
        exprType attr1 = NodeUtils.makeAttribute("a.b.c.d");
        assertEquals("a.b.c.d", NodeUtils.getFullRepresentationString(attr1));
        assertEquals(attr1.toString(), attr.toString());
        
        node = (Module) parseLegalDocStr("a.b.c.d()");
        exprType callAttr = NodeUtils.makeAttribute("a.b.c.d()");
        assertEquals(((Expr)node.body[0]).value.toString(), callAttr.toString());
        
        
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr(
                "print a.b.c().d.__class__"));
        
        Iterator<ASTEntry> iterator = visitor.getIterator();
        iterator.next(); //Module
        iterator.next(); //Print
        ASTEntry entry = iterator.next(); //Attribute
        assertEquals("a.b.c", NodeUtils.getFullRepresentationString(entry.node));


        visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr(
            "'r.a.s.b'.join('a')"));
        iterator = visitor.getIterator();
        iterator.next(); //Module
        iterator.next(); //Expr
        entry = iterator.next(); //Attribute
        assertEquals("str.join", NodeUtils.getFullRepresentationString(entry.node));
        
        visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr(
            "print aa.bbb.cccc[comp.id].hasSimulate"));
        iterator = visitor.getIterator();
        iterator.next(); //Module
        iterator.next(); //Expr
        entry = iterator.next(); //Attribute
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
                "if __name__ == '__main__':\n" +
                "    print 'step 1'\n" +
                "\n");
        
        SimpleNode ast2 = parseLegalDocStr("" +
                "class env:\n" +
                "    pass\n" +
                "\n" +
                "if __name__ == '__main__':\n" +
                "    print 'step 1'\n" +
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
                "    a = 10\n" +
                "    i = 20\n" +
                "    print 'here'\n" +
                "  \n" +
                "if __name__ == '__main__':\n" +
                "  Simple().m1()\n" +
                "\n");
        
        assertEquals("Simple.m1", NodeUtils.getContextName(4, ast));
    }

    private void checkEndLine(SimpleNode ast1, int endLine) {
        EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast1);
        
        List<ASTEntry> classes = visitor.getClassesAndMethodsList();
        assertEquals(1, classes.size());
        assertEquals(endLine, classes.get(0).endLine);
    }
}
