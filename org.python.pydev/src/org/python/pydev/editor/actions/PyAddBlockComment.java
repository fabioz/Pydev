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
 * Creates a comment block
 * 
 * @author Fabio Zadrozny
 */
public class PyAddBlockComment extends PyAction {

	/**
	 * Hi.
	 * Insert a comment block.
	 *                #===...
	 * 				  #
	 * 				  #===...
	 * (TODO:This could be customized somewhere...)
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

			IRegion startLine = null;

			int startLineIndex = selection.getStartLine();

			String endLineDelim = getDelimiter(doc, startLineIndex);
			startLine = doc.getLineInformation(startLineIndex);

			IRegion line = doc.getLineInformation(startLineIndex);
			int pos = line.getOffset();
			StringBuffer strBuf = new StringBuffer();
			strBuf.append(endLineDelim);
			strBuf.append(
				"#===============================================================================");
			strBuf.append(endLineDelim);
			strBuf.append("# ");
			strBuf.append(endLineDelim);
			strBuf.append(
				"#===============================================================================");
			strBuf.append(endLineDelim);
			strBuf.append(endLineDelim);

			doc.replace(pos, 0, strBuf.toString());

		} catch (Exception e) {
			beep(e);
		}
	}
}
