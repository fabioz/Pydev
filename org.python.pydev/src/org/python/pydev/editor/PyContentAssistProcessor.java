/*
 * Author: atotic
 * Created on Mar 25, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 *
 * TODO Implement this class
 */
public class PyContentAssistProcessor implements IContentAssistProcessor {

	static char[] completionProposalActivators = new char['.'];
	/**
	 * override
	 * TODO comment this method
	 */
	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer,
		int documentOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public IContextInformation[] computeContextInformation(
		ITextViewer viewer,
		int documentOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return completionProposalActivators;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
