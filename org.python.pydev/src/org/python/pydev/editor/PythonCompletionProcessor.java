/*
 * Created on Mar 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.python.pydev.editor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

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
	int docBoundary = -1; // the document prior to the activation token
	int docBoundary2 = 0;
	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer,
		int documentOffset) {
		List propList = new ArrayList();
		IDocument doc = viewer.getDocument();
		//		System.out.println("The document:"+doc.get());
		Point selectedRange = viewer.getSelectedRange();
		// there may not be a selected range
		java.lang.String theDoc = doc.get();
		calcDocBoundary(theDoc, documentOffset);
		String activationToken = this.getActivationToken(theDoc, documentOffset);
//		System.out.println("DBG:theActivationToken: "+activationToken);
		theDoc = partialDocument(theDoc, documentOffset);
//		System.out.println("DBG:theDoc: "+theDoc);
		PythonInterpreter interp = initInterpreter(null);
		java.lang.String qualifier = getQualifier(doc, documentOffset);
		int qlen = qualifier.length();
		try {
			System.out.println("Interpreted doc: "+theDoc);
			PyList theList = autoComplete(interp, theDoc, activationToken);
			PyObject o = new PyObject();
			for (int i = 0; i < theList.__len__(); i++) {
				String p = theList.__getitem__(i).toString();
//				System.out.println("Item:" + p);
				CompletionProposal proposal =
					new CompletionProposal(
						p,
						documentOffset - qlen,
						qlen,
						p.length());
				propList.add(proposal);
			}

		} catch (PyException e) {
			e.printStackTrace();
		}

		PyObject theCode = null;

		ICompletionProposal[] proposals =
			new ICompletionProposal[propList.size()];
		// and fill with list elements
		propList.toArray(proposals);
		// Return the proposals
		return proposals;

	}
	private PyList autoComplete(
		PythonInterpreter interp,
		java.lang.String theCode,
		java.lang.String theActivationToken)
		throws PyException {
		StringBuffer example = new StringBuffer();
		interp.exec("from PyDev import jintrospect");
//		System.out.println("DBG:from PyDev import jintrospect:done");
		interp.exec("class object:pass"); //TODO: REMOVE AFTER JYTHON ADDS SUPPORT TO NEW STYLE CLASSES.
		interp.exec(theCode);
		String xCommand = "theList = jintrospect.getAutoCompleteList(command='"+theActivationToken+"', locals=locals())";
//		System.out.println("DBG:xCommand:"+xCommand);
		interp.exec(xCommand);
		PyList theList = (PyList) interp.get("theList");
		return theList;
	}

	/**
	 * @param qualifier
	 * @param documentOffset
	 * @param proposals
	 */
	public void calcDocBoundary(String theDoc, int documentOffset){
		this.docBoundary = theDoc.substring(0, documentOffset).lastIndexOf('\n');
	}
	public String getActivationToken(String theDoc, int documentOffset) {
		if (this.docBoundary < 0){
			calcDocBoundary(theDoc,documentOffset);
		}
		return theDoc.substring(this.docBoundary+1, documentOffset);
	}

	/**
	 * @param theDoc
	 */
	private String partialDocument(String theDoc, int documentOffset) {
		if (this.docBoundary < 0){
			calcDocBoundary(theDoc,documentOffset);
		}
		
		String before = theDoc.substring(0, this.docBoundary);
		return before;

	}

	/**
	 * @param doc
	 * @param documentOffset
	 */
	private java.lang.String getQualifier(IDocument doc, int documentOffset) {
		// this routine should return any partial entry after the activation character
		return "";
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
		return new char[] { '.', '(' , '['};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		// is this _really_ what we want to use??
		return new char[] { '.' };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public java.lang.String getErrorMessage() {
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
// 1. create the PythonSystemState with the embedded $py.class files inserted into its path
// 2. 
	protected PythonInterpreter initInterpreter(java.lang.String[] argv) {
		Properties p = System.getProperties();
//		p.setProperty("python.path", "c:\\Jython\\Lib");
		PySystemState.initialize();
		PythonInterpreter.initialize(System.getProperties(), p, null);
		return new PythonInterpreter(null, createPySystemState());
	}
	/**
		 * Create a new Python interpreter system state object aware for standard 
		 * Jython library.
		 * 
		 * @return Python interpreter system state.
		 * 
		 * @throws JythonRuntimeException if it fails to locate the Jython 
		 * libraries. The exception message will contain an explanation of the 
		 * reason to fail.
		 */
	private String _jythonLib = null;
	public PySystemState createPySystemState() {
		if (_jythonLib == null) {
			// Locate org.jython plugin and grab the jar location
			Plugin jythonPlugin = Platform.getPlugin("org.pydev.jython");

			String jythonPath =
				jythonPlugin.getDescriptor().getInstallURL().toString() + "jythonlib.jar";
			try {
				_jythonLib = Platform.asLocalURL(new URL(jythonPath)).getFile();
//				System.out.println("_jythonLib:"+_jythonLib);
			} catch (MalformedURLException e) {
				System.out.println(
					"Failed to located Python System library because of invalid URL."+
					e);
			} catch (IOException e) {
				System.out.println(
					"Failed to located Python System library because of IO Error."+
					e);
			}
		}
		PySystemState result = new PySystemState();
		result.path.insert(0, new PyString(_jythonLib + "/Lib"));
//		System.out.println("result.path: "+result.path);
		// Location of the jython/python modules
		return result;
	}

}
