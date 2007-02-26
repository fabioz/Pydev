package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class BeginOffset extends AbstractOffsetStrategy {

	public BeginOffset(IASTNodeAdapter adapter, IDocument doc) {
		super(adapter, doc);
	}

	protected int getLine() {
		int startLine = adapter.getNodeFirstLine() - 1;
		if (startLine < 0)
			startLine = 0;
		return startLine;
	}

	@Override
	protected int getLineIndendation() throws BadLocationException {
		if (adapter.getNodeBodyIndent() == 0)
			return 0;
		else {
			return doc.getLineLength(getLine());
		}
	}
}
