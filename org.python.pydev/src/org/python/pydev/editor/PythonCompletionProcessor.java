/*
 * Created on Mar 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;


/**
 * @author Dmoore
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PythonCompletionProcessor implements IContentAssistProcessor {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer,
		int documentOffset) {
		// get the document to be inspected...
		IDocument doc = viewer.getDocument();
		// get the current selection range...
		Point selectedRange = viewer.getSelectedRange();
		List propList = new ArrayList();
		if (selectedRange.y > 0) {
			try {
				String text = doc.get(selectedRange.x, selectedRange.y);
				System.out.println("Document text:"+text);
				computeStyleProposals(text, selectedRange, propList);	
			} catch (BadLocationException e) {
				// ???
				return null;
			}
		}
		else{
			//try to retrv a 'qualifier' from the doc.
			// for example, a partial python statement. This might be used to restrict the set of possible proposals
			String qualifier = getQualifier(doc, documentOffset);
			System.out.println("Document qualifier:"+qualifier);
			computeStructureProposals(qualifier, documentOffset, propList);
		}
		// create an array of completion proposals
		ICompletionProposal []  proposals = new ICompletionProposal[propList.size()];
		propList.toArray(proposals);
		return proposals;
		 
	}

	/**
	 * @param qualifier
	 * @param documentOffset
	 * @param proposals
	 */
	private void computeStructureProposals(String qualifier, int documentOffset, List propList) {
		// Not sure how to adapt this one either.
		// The HTML example computes a part 'before the planned cursor' and a part 'after the planned cursor'
		// I think that here is where we want to somehow latch the pointer from the current parse tree.
		// we want to look at the text before the '.', and see what from the parse tree completes it...
		int qlen = qualifier.length();
		for (int i = 0; i < STRUCTTAGS1.length; i++) {
			String startTag = STRUCTTAGS1[i];
			// Check if proposal matches qualifier
			if (startTag.startsWith(qualifier)){
				// Yes.. compute entire proposal text
				String text = startTag + STRUCTTAGS2[i];
				int cursor = startTag.length();
				// construct proposal
				CompletionProposal proposal = new CompletionProposal(text, documentOffset - qlen, qlen, cursor);
				// add to results
				propList.add(proposal);
			}
			
		}	
	}

	/**
	 * @param doc
	 * @param documentOffset
	 * @return
	 */
	private String getQualifier(IDocument doc, int documentOffset) {
		// use a StringBuffer to collect the bunch of proposals
//		StringBuffer sB = new StringBuffer();
//		while (true) {
//			try {
//				char c = doc.getChar(--documentOffset);
				// do something here to test if this was the start of a python statement
				// TODO: Figure this out. For sonmething like HTML, the test might be:
				// if (c == '>' || Character.isWhitespace(c) or something like that
				// Not really sure what our equivaent is?
				// for now..
//				return "";
//			} catch (BadLocationException e) {
//				return "";
//			}
//		}
		return "";
	}
// Proposal part before cursor...
// Let's stub in some static ones for the moment...
private final static String[] STRUCTTAGS1 = 
{"Dana", "Bill", "Aleks", "Fabio"};
private final static String[] STRUCTTAGS2 = 
{"Moore", "Wright", "Totic", "Zadrozny"};


private final static String[] STYLETAGS = 
{"dana", "bill", "aleks", "fabio"};
private final static String[] STYLELABELS = 
{"DANA", "BILL", "ALEKS", "FABIO"};

	/**
	 * @param text
	 * @param selectedRange
	 * @param proposals
	 */
	private void computeStyleProposals(String selectedText, Point selectedRange, List propList) {
		// loop thru the styles.. what does this have to do with completion???
		for (int i = 0; i < STYLETAGS.length; i++) {
			String tag = STYLETAGS[i];
			// compute replacement text
			String replacement = "<"+tag+">"+ selectedText +"</"+tag+">";
			int cursor = tag.length()+2; // ?? why plus 2
			IContextInformation contextInfo = new ContextInformation(null, STYLELABELS[i]+" Style");
			CompletionProposal proposal = new CompletionProposal(replacement, selectedRange.x, selectedRange.y, cursor, null, STYLELABELS[i], contextInfo, replacement);
			// add that to the list of proposals
			propList.add(proposal);			
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(
		ITextViewer viewer,
		int documentOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'.'};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		// is this _really_ what we want to use??
		return new char[] {'.'};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
