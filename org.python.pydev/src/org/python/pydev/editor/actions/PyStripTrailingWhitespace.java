/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;


/**
 * Strips trailing whitespace on the selected lines.  If no lines are selected, it performs
 * the action on the entire document.
 * 
 * @author Parhaum Toofanian
 */
public class PyStripTrailingWhitespace extends PyAction 
{
	/* Selection element */
	private static PySelection ps;


	/**
	 * Grabs the selection information and performs the action.
	 */
	public void run ( IAction action ) 
	{
		try 
		{
			// Select from text editor
			ps = new PySelection ( getTextEditor ( ), false );
			// Perform the action
			perform ( );

			// Put cursor at the first area of the selection
			getTextEditor ( ).selectAndReveal ( ps.endLine.getOffset ( ), 0 );
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}		
	}


	/**
	 * Performs the action with the class' PySelection.
	 * 
	 * @return boolean The success or failure of the action
	 */
	public static boolean perform ( )
	{
		return perform ( ps );
	}


	/**
	 * Performs the action with a given PySelection
	 * 
	 * @param ps Given PySelection
	 * @return boolean The success or failure of the action
	 */
	public static boolean perform ( PySelection ps ) 
	{
		// What we'll be replacing the selected text with
		StringBuffer strbuf = new StringBuffer ( );
			
		int i;
	
		try 
		{
			// For each line, strip their whitespace
			for ( i = ps.startLineIndex; i < ps.endLineIndex; i++ )
			{
				strbuf.append ( trimTrailingWhitespace ( ps.doc.get ( ps.doc.getLineInformation ( i ).getOffset ( ), ps.doc.getLineInformation ( i ).getLength ( ) ) ) + ps.endLineDelim );
			}
		
			// Handle the last line differently, because it might not be a full line with
			// and ending delimiter, so we don't just want to trim whitespace (it could be
			// ending in the middle of a line, and this would screw up the code, which we
			// imagine the developer doesn't want to do
			if ( ps.endLineIndex - ps.startLineIndex > 0 )
			{
				String lastline = ps.doc.get ( ps.endLine.getOffset ( ), ps.startLine.getOffset ( ) + ps.selLength - ps.endLine.getOffset ( ) );
					
				// Check if full last line is selected or not
				if ( lastline.length ( ) == ps.endLine.getLength ( ) )
				{
					strbuf.append ( trimTrailingWhitespace ( lastline ) );
				}
				else
				{
					strbuf.append ( lastline );
				}
			}

			// If all goes well, replace the text with the modified information	
			if ( strbuf.toString ( ) != null )
			{
				ps.doc.replace ( ps.startLine.getOffset ( ), ps.selLength, strbuf.toString ( ) );
				return true;
			}
		}
		catch ( Exception e ) 
		{
			beep( e );
		}	
			
		// In event of problems, return false
		return false;		
	}

	
	/**
	 * This method is called to check for trailing whitespace and get rid of it
	 * 
	 * @param str the string to be checked.
	 * @return String the string, stripped of whitespace, or an empty string.
	 */	public static String trimTrailingWhitespace ( String str )
	{
		// If nothing at all, just return 
		if ( str == null )
			return "";
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
