/*
 * Author: atotic
 * Created on Mar 25, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;

/**
 *
 * TODO Implement this class
 * It should implement navigation/
 */
public class PyContentAssistant implements IContentAssistant {

	/**
	 */
	public void install(ITextViewer textViewer) {
		// TODO Auto-generated method stub

	}

	public void uninstall() {
		// TODO Auto-generated method stub

	}

	public String showPossibleCompletions() {
		// TODO Auto-generated method stub
		return null;
	}

	public String showContextInformation() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public IContentAssistProcessor getContentAssistProcessor(String contentType) {
		// TODO Auto-generated method stub
		return null;
	}

}
