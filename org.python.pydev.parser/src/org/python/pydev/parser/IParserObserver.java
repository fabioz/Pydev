/*
 * @author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;

/**
 * PyParser broadcasts events to IParserListeners 
 * 
 * parserChanged is generated every time document is parsed successfully
 * parserError is generated when parsing fails
 */
public interface IParserObserver {
	
	/**
	 * every time document gets parsed, it generates a new parse tree
	 * @param root the root of the new AST (abstract syntax tree)
	 * @param file the file that has just been analyzed (it may be null)
     * 
     * It is meant to be an org.eclipse.core.resources.IFile or an 
     * org.eclipse.ui.internal.editors.text.JavaFileEditorInput
     * 
	 */
	void parserChanged(SimpleNode root, IAdaptable file, IDocument doc);
	
	/**
	 * if parse generates an error, you'll get this event
	 * the exception class will be ParseException, or TokenMgrError
	 * @param file the file that has just been analyzed (it may be null)
	 */
	void parserError(Throwable error, IAdaptable file, IDocument doc);
}
