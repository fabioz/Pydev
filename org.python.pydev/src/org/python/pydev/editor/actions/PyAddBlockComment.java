/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;


/**
 * Creates a comment block.  Comment blocks are slightly different than regular comments 
 * created in that they provide a distinguishing element at the beginning and end as a 
 * separator.  In this case, it is a string of <code>=======</code> symbols to strongly
 * differentiate this comment block.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyAddBlockComment extends PyAction 
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
		
		// If they selected a partial line, count it as a full one
		ps.selectCompleteLines ( );

		int i;
		try
		{
			// Start of block
			strbuf.append ( "#" + getFullCommentLine ( ) + ps.endLineDelim );

			// For each line, comment them out
			for ( i = ps.startLineIndex; i <= ps.endLineIndex; i++ )
			{
				strbuf.append ( "#" + ps.getLine ( i ) + ps.endLineDelim );
			}
		
			// End of block
			strbuf.append ( "#" + getFullCommentLine ( ) );

			// Replace the text with the modified information
			ps.doc.replace ( ps.startLine.getOffset ( ), ps.selLength, strbuf.toString ( ) );
			return true;
		}
		catch ( Exception e )
		{
			beep ( e );
		}

		// In event of problems, return false
		return false;
	}
	
	
	/**
	 * Currently returns a string with the comment block.  
	 * 
	 * @return Comment line string, or a default one if Preferences are null
	 */
	protected static String getFullCommentLine ( )
	{
		try
		{
			Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
			return prefs.getString(PydevPrefs.BLOCK_COMMENT);
		}
		catch ( Exception e )
		{
		    //return the default print margin...
			return "================================================================================";
		}
	}
}
