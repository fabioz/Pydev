package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

public abstract class AbstractOffsetStrategy implements IOffsetStrategy {

	protected IDocument doc;

	protected IASTNodeAdapter adapter;

	protected NodeHelper nodeHelper;

	public AbstractOffsetStrategy(IASTNodeAdapter adapter, IDocument doc) {
		this.adapter = adapter;
		this.doc = doc;
		this.nodeHelper = new NodeHelper(TextUtilities.getDefaultLineDelimiter(doc));
	}

	protected IRegion getRegion() throws BadLocationException {
		return doc.getLineInformation(getLine());
	}

	protected int getLineOffset() throws BadLocationException {
		return getRegion().getOffset();
	}

	protected int getLineIndendation() throws BadLocationException {
		return doc.getLineLength(getLine());
	}

	public int getOffset() throws BadLocationException {
		return getLineOffset() + getLineIndendation();
	}

	protected abstract int getLine();
}
