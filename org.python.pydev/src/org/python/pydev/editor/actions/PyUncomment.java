/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;


/**
 * @author fabioz
 */
public class PyUncomment extends PyComment {

	/**
	 * Same as comment, but remove the first char.
	 */
	protected String replaceStr(String str,String endLineDelim){
		str = str.replaceAll(endLineDelim+"#", endLineDelim );
		if (str.startsWith("#")){
			str = str.substring(1);
		}
		return str;
	}

}
