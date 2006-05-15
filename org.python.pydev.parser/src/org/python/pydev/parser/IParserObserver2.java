package org.python.pydev.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;

public interface IParserObserver2 {

	/**
     * Has the argsToReparse additional Parameter
	 */
	void parserChanged(SimpleNode root, IAdaptable file, IDocument doc, Object ... argsToReparse);
	
	/**
	 * Has the argsToReparse additional Parameter
	 */
	void parserError(Throwable error, IAdaptable file, IDocument doc, Object ... argsToReparse);

}
