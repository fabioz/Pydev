/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Fabio Zadrozny
 */
public class PyComment extends PyAction {

	/**
	 * Comment the first lines.
	 */
	public void run(IAction action) {
		try {

			ITextEditor textEditor = getTextEditor();

			IDocument doc =
				textEditor.getDocumentProvider().getDocument(
					textEditor.getEditorInput());
			ITextSelection selection =
				(ITextSelection) textEditor
					.getSelectionProvider()
					.getSelection();

			int startLineIndex = selection.getStartLine();
			int endLineIndex = selection.getEndLine();
			
			//special cases...first char of the editor, last char...
			if (endLineIndex < startLineIndex) {
				endLineIndex = startLineIndex;
			}

			IRegion startLine = doc.getLineInformation(startLineIndex);
			IRegion endLine = doc.getLineInformation(endLineIndex);

			int initialPos = startLine.getOffset();
			int length =
				(endLine.getOffset() - startLine.getOffset())
					+ endLine.getLength();

			String endLineDelim = getDelimiter(doc, startLineIndex);
			startLine = doc.getLineInformation(startLineIndex);
			endLine = doc.getLineInformation(endLineIndex);

			String str = new String(doc.get(initialPos, length));

			str = replaceStr(str, endLineDelim);
			doc.replace(initialPos, length, str);

		} catch (Exception e) {
			beep(e);
		}		
	}

	/**
	 * This method is called to return the text that should be replaced
	 * by the text passed as a parameter.
	 * 
	 * The text passed as a parameter represents the text from the whole
	 * lines of the selection.
	 * 
	 * @param str the string to be replaced.
	 * @param endLineDelim delimiter used.
	 * @return the new string.
	 */
	protected String replaceStr(String str, String endLineDelim) {
		return "#" + str.replaceAll(endLineDelim, endLineDelim + "#");
	}
}
