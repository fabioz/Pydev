/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * More for my benefit in making the unit tests, this assembles information from a document, 
 * whether in Eclipse or faked from outside, and performs a common selection routine for many
 * of the actions in the editor.  Serves to refactor repetitive code in some Actions and tests.
 * 
 * @author Parhaum Toofanian
 */
public class PySelection
{
	/* The document this is a part of */
	public IDocument doc;
	/* The line number of the first line */
	public int startLineIndex;
	/* The line number of the last line */
	public int endLineIndex;
	/* Length of selected text */
	public int selLength;
	/* The selected text */
	public String selection;
	/* End line delimiter */
	public String endLineDelim;
	/* Start line region */
	public IRegion startLine;
	/* End line region */
	public IRegion endLine;
	/* Original cursor line */
	public int cursorLine;

	/**
	 * Default constructor for PySelection.  Simply defaults all the values.
	 */
	public PySelection ( )
	{
		// Initialize values
		setDefaults ( );		
	}
	
	
	/**
	 * Alt constructor for PySelection.  Takes in a text editor from Eclipse, and a boolean that
	 * is used to indicate how to handle an empty selection.
	 * 
	 * @param textEditor The text editor operating in Eclipse
	 * @param onEmptySelectAll If true, this indicates that on an empty selection, the whole 
	 * document should be selected
	 */
	public PySelection ( ITextEditor textEditor, boolean onEmptySelectAll )
	{
		// Initialize values
		setDefaults ( );

		try 
		{
			// Grab the document
			this.doc =
				textEditor.getDocumentProvider().getDocument(
					textEditor.getEditorInput());
								
			// Grab the selection
			ITextSelection selection =
				(ITextSelection) textEditor
					.getSelectionProvider()
					.getSelection();
			
			// Set data
			this.startLine 		= doc.getLineInformation ( selection.getStartLine ( ) );
			this.endLine 		= doc.getLineInformation ( selection.getEndLine ( ) );
			
			this.startLineIndex	= selection.getStartLine ( );
			this.endLineIndex	= selection.getEndLine ( );
			this.selLength		= selection.getLength ( );
			
			this.cursorLine		= selection.getEndLine ( );
	
			// Store selection information
			select ( onEmptySelectAll );
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}				
	}
		
		
	/**
	 * Alt constructor for PySelection.  Takes in a document, starting line, ending line, and
	 * length of selection, as well as a boolean indicating how to handle an empty selection.
	 * 
	 * @param doc Document to be affected
	 * @param startLineIndex Line number for first line
	 * @param endLineIndex Line number for last line
	 * @param selLength Length of selected text
	 * @param onEmptySelectAll If true, this indicates that on an empty selection, the whole 
	 * document should be selected
	 */
	public PySelection ( IDocument doc, int startLineIndex,	int endLineIndex, int selLength, boolean onEmptySelectAll )
	{
		// Initialize values
		setDefaults ( );

		// Set data
		this.doc 			= doc;
		this.startLineIndex = startLineIndex;
		this.endLineIndex 	= endLineIndex;
		this.selLength 		= selLength;
		
		this.cursorLine			= endLineIndex;

		// Store selection information
		select ( onEmptySelectAll );
	}
	
	
	/**
	 * Defaults all the values.
	 */
	private void setDefaults ( )
	{
		doc 			= null;
		startLineIndex	= 0;
		endLineIndex	= 0;
		selLength		= 0;
		selection		= new String ( );
		selection		= "";
		endLineDelim	= new String ( );
		endLineDelim	= "";
		startLine		= null;
		endLine			= null;	
		cursorLine		= 0;	
	}
	
	
	/**
	 * Make the full selection from the information in the class' data.
	 * 
	 * @param onEmptySelectAll If true, this indicates that on an empty selection, the whole 
	 * document should be selected
	 */
	private void select ( boolean onEmptySelectAll)
	{
		try 
		{
			// Get some line information
			int initialPos = 0;

			//special cases...first char of the editor, last char...
			if (endLineIndex < startLineIndex) {
				endLineIndex = startLineIndex;
			}
			
			// If anything is actually selected, we'll be modifying the selection only
			if ( selLength > 0 )
			{
				startLine = doc.getLineInformation ( startLineIndex );
				endLine = doc.getLineInformation ( endLineIndex );

				// Get offsets and lengths
				initialPos = startLine.getOffset ( );
				endLineDelim = PyAction.getDelimiter ( doc, startLineIndex );
	
				// Grab the selected text into our string
				selection = doc.get ( initialPos, selLength );
			}
			// Otherwise we'll modify the whole document, if asked to
			else if ( onEmptySelectAll )
			{
				startLineIndex = 0;
				endLineIndex = doc.getNumberOfLines ( ) - 1;

				startLine = doc.getLineInformation ( startLineIndex );
				endLine = doc.getLineInformation ( endLineIndex );

				endLineDelim = PyAction.getDelimiter ( doc, 0 );

				// Grab the whole document
				selection = doc.get ( );
				selLength = selection.length ( );
			}
			// Grab the current line only
			else
			{
				startLine = doc.getLineInformation ( startLineIndex );
				endLine = doc.getLineInformation ( endLineIndex );
				
				selLength = startLine.getLength ( );

				// Get offsets and lengths
				initialPos = startLine.getOffset ( );
				endLineDelim = PyAction.getDelimiter ( doc, startLineIndex );
	
				// Grab the selected text into our string
				selection = doc.get ( initialPos, selLength );				
			}
		} 
		catch ( Exception e ) 
		{
			beep( e );
		}	
	}
	
	
	/**
	 * In event of partial selection, used to select the full lines involved.
	 */
	public void selectCompleteLines ( )
	{
		selLength = ( endLine.getOffset ( ) + endLine.getLength ( ) ) - startLine.getOffset ( );		
	}


	/**
	 * Beep...humm... yeah....beep....ehehheheh
	 */
	protected static void beep(Exception e) {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
		e.printStackTrace();
	}
	
	
	/**
	 * Gets line from document.
	 * 
	 * @param i Line number
	 * @return String line in String form
	 */
	public String getLine ( int i )
	{
		try
		{
			return doc.get ( doc.getLineInformation ( i ).getOffset ( ), doc.getLineInformation ( i ).getLength ( ) );	
		}
		catch ( Exception e )
		{
			return "";
		}
	}
	
	
	/**
	 * Gets cursor offset to go to.
	 * 
	 * @return int Offset to put cursor at
	 */
	public int getCursorOffset ( )
	{
		try
		{
			return doc.getLineInformation ( cursorLine ).getOffset ( );
		}
		catch ( Exception e )
		{
			return 0;
		}
	}
	
}
