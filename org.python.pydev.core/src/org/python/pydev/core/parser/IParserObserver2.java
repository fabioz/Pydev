package org.python.pydev.core.parser;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;

public interface IParserObserver2 {

	/**
     * Has the argsToReparse additional Parameter
	 */
	void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, Object ... argsToReparse);
	
	/**
	 * Has the argsToReparse additional Parameter
	 */
	void parserError(Throwable error, IAdaptable file, IDocument doc, Object ... argsToReparse);

}
