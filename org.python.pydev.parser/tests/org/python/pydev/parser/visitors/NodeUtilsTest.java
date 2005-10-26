package org.python.pydev.parser.visitors;

import java.util.Iterator;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class NodeUtilsTest extends PyParserTestBase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(NodeUtilsTest.class);
	}

	public void testFullRep() throws Exception {
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
}
