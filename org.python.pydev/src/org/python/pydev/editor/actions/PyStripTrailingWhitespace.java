/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Strips trailing whitespace on the selected lines.  If no lines are selected, it performs
 * the action on the entire document.
 * 
 * @author Parhaum Toofanian
 */
public class PyStripTrailingWhitespace extends PyAction {

	/**
	 * Strip whitespace.
	 */
	public void run(IAction action) {
		try {
			// Grab the editor
			ITextEditor textEditor = getTextEditor();

			// Grab the document
			IDocument doc =
				textEditor.getDocumentProvider().getDocument(
					textEditor.getEditorInput());
								
			// Grab the selection
			ITextSelection selection =
				(ITextSelection) textEditor
					.getSelectionProvider()
					.getSelection();
			
			// What we'll be modifying
			String str = new String ( );
			// End line delimiter character
			String endLineDelim = new String ( );
			
			// Get some line information
			int initialPos = 0;
			int length = 0;
			int startLineIndex = selection.getStartLine();
			int endLineIndex = selection.getEndLine();
			//special cases...first char of the editor, last char...
			if (endLineIndex < startLineIndex) {
				endLineIndex = startLineIndex;
			}
			IRegion startLine = doc.getLineInformation(startLineIndex);
			IRegion endLine = doc.getLineInformation(endLineIndex);
			
			// If anything is actually selected, we'll be modifying the selection only
			if ( selection.getLength ( ) > 0 )
			{
	
				// Get offsets and lengths
				initialPos = startLine.getOffset();
				length =
					(endLine.getOffset() - startLine.getOffset())
						+ endLine.getLength();
	
				endLineDelim = getDelimiter(doc, startLineIndex);
	
				// Grab the selected text into our string
				str = doc.get(initialPos, length);
			}
			// Otherwise we'll modify the whole document
			else
			{
				// Grab the whole document
				str = doc.get ( );
				endLineDelim = getDelimiter ( doc, 0 );
				length = doc.getLength();
			}

			// We can't use trim() since we only want to get rid of trailing whitespace,
			// so we need to go line by line.
			String [] lines = str.split ( endLineDelim );
			// What we'll be replacing the selected text with
			StringBuffer strbuf = new StringBuffer ( );
			
			// For all but the last line, trim whitespace normally
			for ( int i = 0; i < lines.length - 1; i++ )
			{
				strbuf.append ( trimTrailingWhitespace ( lines[i] ) + endLineDelim );
			}
			
			// Handle the last line differently, because it might not be a full line with
			// and ending delimiter, so we don't just want to trim whitespace (it could be
			// ending in the middle of a line, and this would screw up the code, which we
			// imagine the developer doesn't want to do
			if ( lines.length > 0 )
			{
				// Check if full last line is selected or not
				if ( lines[lines.length - 1].length ( ) == endLine.getLength ( ) )
					strbuf.append ( trimTrailingWhitespace ( lines[lines.length - 1] ) );
				else
					strbuf.append ( lines[lines.length - 1] );
			}
			
			// Replace selection with our new stripped text
			doc.replace(initialPos, length, strbuf.toString ( ) );

			// Put cursor at the first area of the selection
			textEditor.selectAndReveal(endLine.getOffset(),0);

		} catch (Exception e) {
			beep(e);
		}		
	}

	/**
	 * This method is called to check for trailing whitespace and get rid of it
	 * 
	 * @param str the string to be checked.
	 * @return String the string, stripped of whitespace, or an empty string.
	 */	private String trimTrailingWhitespace ( String str )
	{
		// If nothing at all, just return 
		if ( str.length ( ) == 0 )
			return "";
				
		// Find the last non-whitespace character
		int j;
		for ( j = str.length ( ) - 1; j >= 0; j-- )
		{
			if ( ! Character.isWhitespace ( str.charAt ( j ) ) ) 
			{
				break;
			}
		}
		
		// If the whole line is whitespace, return empty string
		if ( j < 0 )
			return "";

		// If the last character isn't whitespace, nothing to trim, so return the string
		if ( j == str.length ( ) - 1 )
		{
			return ( str );
		}
		// Otherwise, return a substring
		else
		{
			return ( str.substring ( 0, j + 1 ) );
		}		
	}

}
