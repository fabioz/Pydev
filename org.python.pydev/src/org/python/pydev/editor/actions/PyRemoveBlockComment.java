/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.docutils.PySelection;


/**
 * Removes a comment block.  Comment blocks are slightly different than regular comments 
 * created in that they provide a distinguishing element at the beginning and end as a 
 * separator.  In this case, it is a string of <code>=======</code> symbols to strongly
 * differentiate this comment block.  
 * 
 * This will handle regular comment blocks as well by removing the # token at the head of
 * each line, but will also remove the block separators if they are present
 * 
 * @author Parhaum Toofanian
 */
public class PyRemoveBlockComment extends PyAddBlockComment 
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
			ps = new PySelection ( getTextEditor ( ));
			// Perform the action
			perform ( );

			// Put cursor at the first area of the selection
			getTextEditor ( ).selectAndReveal ( ps.getEndLine().getOffset ( ), 0 );
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
		
		// If they selected a partial line, count it as a full one
		ps.selectCompleteLine ( );

		int i;
		try
		{
			// Start of block, if not block, don't bother
			if ( ! ps.getLine ( ps.getStartLineIndex() ).equals ( "#" + getFullCommentLine ( ) ) )
				return false;
			// End of block, if not block, don't bother
			if ( ! ps.getLine ( ps.getEndLineIndex() ).equals ( "#" + getFullCommentLine ( ) ) )
				return false;
			
			// For each line, comment them out
			for ( i = ps.getStartLineIndex() + 1; i < ps.getEndLineIndex(); i++ )
			{
				if ( ps.getLine ( i ).startsWith ( "#" ) && ! ps.getLine ( i ).substring ( 1 ).equals ( getFullCommentLine ( ) ) )
					strbuf.append ( ps.getLine ( i ).substring ( 1 ) + ( i < ps.getEndLineIndex() - 1 ? ps.getEndLineDelim() : "" ) );
			}

			// Replace the text with the modified information
			ps.getDoc().replace ( ps.getStartLine().getOffset ( ), ps.getSelLength(), strbuf.toString ( ) );
			return true;
		}
		catch ( Exception e )
		{
			beep ( e );
		}

		// In event of problems, return false
		return false;
	}

}
