/*
 * Created on Mar 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.python.pydev.editor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;
import org.osgi.framework.Bundle;
import org.python.parser.ast.If;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Dmoore
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PythonCompletionProcessor implements IContentAssistProcessor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
	 *      int)
	 */
	int docBoundary = -1; // the document prior to the activation token

	int docBoundary2 = 0;

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int documentOffset) {
		List propList = new ArrayList();
		IDocument doc = viewer.getDocument();
		//		System.out.println("The document:"+doc.get());
		Point selectedRange = viewer.getSelectedRange();
		// there may not be a selected range
		java.lang.String theDoc = doc.get();
		calcDocBoundary(theDoc, documentOffset);
		String activationToken = this
				.getActivationToken(theDoc, documentOffset);
		System.out.println("DBG:theActivationToken: " + activationToken);
		theDoc = partialDocument(theDoc, documentOffset);
		java.lang.String qualifier = getQualifier(doc, documentOffset);
		int qlen = qualifier.length();
		theDoc += "\n"+activationToken;
		System.out.println("Interpreted doc: " + theDoc);
//		System.out.println("activationToken: " + activationToken);
		Vector theList = autoComplete(theDoc, activationToken);
		System.out.println("DBG:vector:" + theList);

		for (Iterator iter = theList.iterator(); iter.hasNext();) {
			String element = (String) iter.next();

			CompletionProposal proposal = new CompletionProposal(element,
					documentOffset - qlen, qlen, element.length());
			propList.add(proposal);
		}

		ICompletionProposal[] proposals = new ICompletionProposal[propList
				.size()];
		// and fill with list elements
		propList.toArray(proposals);
		// Return the proposals
		return proposals;

	}

	private Vector autoComplete(java.lang.String theCode,
			java.lang.String theActivationToken) {
		Vector theList = new Vector();
		String s = new String();
		File tmp = null;
		System.out.println("DBG:autoComplete");
		try {
			// get the inspect.py file from the package:
			s = getAutoCompleteScript();
			System.out.println("DBG:getAutoCompleteScript() returns" + s);
		} catch (CoreException e) {
			System.out.println("DBG:getAutoCompleteScript() fails " + e);
			e.printStackTrace();
		}

		//
		try {
			tmp = bufferContent(theCode);
			if (tmp == null) {
				System.out
						.println("DBG:bufferContent() null. No tip for you!!");
				return theList;
			}
			String ss = new String("python " + s + " "
					+ tmp.getAbsolutePath());
			System.out.println("DBG:exec string " + ss);
			Process p = Runtime.getRuntime().exec(ss);
			BufferedReader in = new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			String str;
			while ((str = in.readLine()) != null) {
				if(!str.startsWith("tip: ")) continue;
				str = str.substring(5);
				System.out.println("DBG:autoComplete:output: " + str);
				theList.add(str);
			}
			in.close();
			InputStream eInput = p.getErrorStream();
			BufferedReader eIn = new BufferedReader(new InputStreamReader(
					eInput));

			while ((str = eIn.readLine()) != null) {
				System.out.println("error output: " + str);
			}
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Interrupted call: error output: ");
			e.printStackTrace();
		}
		return theList;
	}

	/**
	 * @param theCode
	 */
	private File bufferContent(java.lang.String theCode) {
		//
		try {
			// Create temp file.
			File temp = File.createTempFile("PyDev", ".tmp");

			// Delete temp file when program exits.
			temp.deleteOnExit();

			// Write to temp file
			BufferedWriter out = new BufferedWriter(new FileWriter(temp));
			out.write(theCode);
			out.close();
			return temp;
		} catch (IOException e) {
			return null;
		}
	}

	public static String getAutoCompleteScript() throws CoreException {
		String targetExec = "tipper.py";
		System.out.println("DBG:getAutoCompleteScript();");
		IPath relative = new Path("PySrc").addTrailingSeparator().append(
				targetExec);
		System.out.println("DBG:getAutoCompleteScript(); relative " + relative);
		Bundle bundle = PydevPlugin.getDefault().getBundle();
		System.out.println("DBG:getAutoCompleteScript(); bundle " + bundle);
		URL bundleURL = Platform.find(bundle, relative);
		URL fileURL;
		try {
			fileURL = Platform.asLocalURL(bundleURL);
			String filePath = new File(fileURL.getPath()).getAbsolutePath();
			System.out.println("DBG:getAutoCompleteScript();filePath "
					+ filePath);
			return filePath;
		} catch (IOException e) {
			throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR,
					"Can't find python debug script", null));
		}
	}

	/**
	 * @param qualifier
	 * @param documentOffset
	 * @param proposals
	 */
	public void calcDocBoundary(String theDoc, int documentOffset) {
		this.docBoundary = theDoc.substring(0, documentOffset)
				.lastIndexOf('\n');
	}

	public String getActivationToken(String theDoc, int documentOffset) {
		if (this.docBoundary < 0) {
			calcDocBoundary(theDoc, documentOffset);
		}
		return theDoc.substring(this.docBoundary + 1, documentOffset);
	}

	/**
	 * @param theDoc
	 */
	private String partialDocument(String theDoc, int documentOffset) {
		if (this.docBoundary < 0) {
			calcDocBoundary(theDoc, documentOffset);
		}

		String before = theDoc.substring(0, this.docBoundary);
		return before;

	}

	/**
	 * @param doc
	 * @param documentOffset
	 */
	private java.lang.String getQualifier(IDocument doc, int documentOffset) {
		// this routine should return any partial entry after the activation
		// character
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
	 *      int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int documentOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '(', '[' };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		// is this _really_ what we want to use??
		return new char[] { '.' };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public java.lang.String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}