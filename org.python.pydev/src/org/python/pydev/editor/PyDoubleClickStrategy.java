/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

/**
 * Our double-click implementation. 
 * 
 * Copied org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickStrategy.
 */
public class PyDoubleClickStrategy implements ITextDoubleClickStrategy {

	protected static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

	protected JavaPairMatcher fPairMatcher = new JavaPairMatcher(BRACKETS);

	/**
	 * @see ITextDoubleClickStrategy#doubleClicked
	 */
	public void doubleClicked(ITextViewer textViewer) {

		int offset = textViewer.getSelectedRange().x;

		if (offset < 0)
			return;

		IDocument document = textViewer.getDocument();

		IRegion region = fPairMatcher.match(document, offset);
		if (region != null && region.getLength() >= 2)
			textViewer.setSelectedRange(
				region.getOffset() + 1,
				region.getLength() - 2);
		else
			selectWord(textViewer, document, offset);
	}

	protected void selectWord(
		ITextViewer textViewer,
		IDocument document,
		int anchor) {

		try {

			int offset = anchor;
			char c;

			while (offset >= 0) {
				c = document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--offset;
			}

			int start = offset;

			offset = anchor;
			int length = document.getLength();

			while (offset < length) {
				c = document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++offset;
			}

			int end = offset;

			if (start == end)
				textViewer.setSelectedRange(start, 0);
			else
				textViewer.setSelectedRange(start + 1, end - start - 1);

		} catch (BadLocationException x) {
		}
	}
}
