package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class EndOffset extends AbstractOffsetStrategy {

	public EndOffset(IASTNodeAdapter adapter, IDocument doc) {
		super(adapter, doc);
	}

	protected int getLine() {
		int endLine = adapter.getNodeLastLine() - 1;
		if (endLine < 0)
			endLine = 0;
		return endLine;
	}

}
