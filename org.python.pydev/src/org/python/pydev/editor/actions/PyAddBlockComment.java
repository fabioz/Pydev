/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.python.pydev.plugin.PydevPrefs;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.plugin.PydevPlugin;


/**
 * Creates a comment block.  Comment blocks are slightly different than regular comments 
 * created in that they provide a distinguishing element at the beginning and end as a 
 * separator.  In this case, it is a string of <code>=======</code> symbols to strongly
 * differentiate this comment block.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyAddBlockComment extends PyComment {

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
		return	"#" + getFullCommentLine ( ) + endLineDelim +
				"#" + str.replaceAll(endLineDelim, endLineDelim + "#") +
				endLineDelim + "#" + getFullCommentLine ( );
	}
	
	/**
	 * Currently returns a string with the comment block.  
	 * @return Comment line string
	 */
	protected String getFullCommentLine(){
		Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
		return prefs.getString(PydevPrefs.BLOCK_COMMENT);
	}

}
