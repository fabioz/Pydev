/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.LabelProvider;


/*
 * In RawPartition, elements are Position markers inside the document
 * we need reference to the document to get its text.
 * I whish I had access to DocumentAdapter in SourceViewer, but I do not
 */
class RawPartitionLabelProvider extends LabelProvider {

	IDocument document;

	RawPartitionLabelProvider(IDocument document) {
		this.document = document;
	}
	
	/** 
	 * @param element is a org.eclipse.jface.text.Position
	 */
	public String getText(Object element) {
		Position p = (Position)element;
		try {
			// get the text from the document, truncate if needed
			boolean trim = p.length > 50;
			String text = document.get(p.offset, trim ? 30 : p.length);
			if (trim) {
				text += "...";
			}
			return text;
		} catch (BadLocationException e) {
			System.err.println(
				"PyOutlinePage::OutlineLabelProvider unexpected error");
			e.printStackTrace();
			return "";
		}
	}
};