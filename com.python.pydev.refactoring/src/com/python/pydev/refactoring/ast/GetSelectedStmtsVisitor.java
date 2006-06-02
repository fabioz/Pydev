package com.python.pydev.refactoring.ast;

import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.If;

public class GetSelectedStmtsVisitor extends FindScopeVisitor{

	private Tuple<Integer, Integer> start;
	private Tuple<Integer, Integer> end;

	public GetSelectedStmtsVisitor(PySelection ps) {
		int initialOffset = ps.getAbsoluteCursorOffset();
		int finalOffset = initialOffset + ps.getSelLength();
		
		this.start = ps.getLineAndCol(initialOffset);
		this.end = ps.getLineAndCol(finalOffset);
		toASTCoords(start);
		toASTCoords(end);
	}

	@Override
	protected Object unhandled_node(SimpleNode node) throws Exception {
		
		return null;
	}
	
	/**
	 * Transforms from doc coords to ast coords
	 */
	private void toASTCoords(Tuple<Integer, Integer> region) {
		region.o1 = region.o1 + 1;
		region.o2 = region.o2 + 1;
	}

	@Override
	protected void checkIfMainNode(If node) {
		//do nothing
	}

	public SimpleNode[] getSelectedStmts() {
		return null;
	}
}
