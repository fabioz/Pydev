package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractReplaceEdit extends AbstractTextEdit {

	public AbstractReplaceEdit(IRefactoringRequest req) {
		super(req);
	}

	@Override
	public TextEdit getEdit() {
		return new ReplaceEdit(getOffset(), getReplaceLength(),
				getFormatedNode());
	}

	protected String getFormatedNode() {
		String source = VisitorFactory.createSourceFromAST(getEditNode());
		return source.trim();
	}

	protected abstract int getReplaceLength();

}