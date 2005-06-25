/*
 * @author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import org.python.parser.SimpleNode;

/**
 * PyParser broadcasts events to IParserListeners 
 * 
 * parserChanged is generated every time document is parsed successfully
 * parserError is generated when parsing fails
 */
public interface IParserListener {
	
	/**
	 * every time document gets parsed, it generates a new parse tree
	 * @param root - the root of the new AST (abstract syntax tree)
	 */
	void parserChanged(SimpleNode root);
	
	/**
	 * if parse generates an error, you'll get this event
	 * the exception class will be ParseException, or TokenMgrError
	 */
	void parserError(Throwable error);
}
