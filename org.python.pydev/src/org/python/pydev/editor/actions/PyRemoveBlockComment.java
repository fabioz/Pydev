/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;


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
public class PyRemoveBlockComment extends PyAddBlockComment {

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
		str = str.replaceAll(endLineDelim+"#", endLineDelim );
		if (str.startsWith("#")){
			str = str.substring(1);
		}
		str = str.replaceAll(getFullCommentLine()+endLineDelim, "");
		str = str.replaceAll(endLineDelim+getFullCommentLine(), "");

		return str;
	}

}
