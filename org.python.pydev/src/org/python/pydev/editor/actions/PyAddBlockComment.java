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

			//strange things happen when we try to get the line information on the last line of the
			//document (it doesn't return the end line delimiter correctly), so we always get on position
			//0 and check to see if we are not in the last line. 
			if (doc.getNumberOfLines() <= 1)
				return;
				
			IRegion startLine = null;

			int startLineIndex = selection.getStartLine();

			String endLineDelim = getDelimiter(doc, 0);
			startLine = doc.getLineInformation(startLineIndex);

			IRegion line = doc.getLineInformation(startLineIndex);
			int lineLenght = line.getLength();
			int pos = line.getOffset();
			
			String content = doc.get(pos, lineLenght);
			String comment = getFullCommentLine();
			
			StringBuffer strBuf = new StringBuffer();
			
			strBuf.append(endLineDelim);
			strBuf.append(comment); //#=========...
			strBuf.append(endLineDelim);
			
			strBuf.append("# ");
			strBuf.append(content);
			
			strBuf.append(endLineDelim);
			strBuf.append(comment); //#=========...
			strBuf.append(endLineDelim);
			
			doc.replace(pos, lineLenght, strBuf.toString());
			textEditor.selectAndReveal(pos+strBuf.length()-comment.length()-4,0);

		} catch (Exception e) {
			beep(e);
		}
	}
	
	private String getFullCommentLine(){
		return "#===============================================================================";
	}
}
