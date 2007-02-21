package org.python.pydev.editor.codecompletion;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;

/**
 * The code-completion engine that should be used inside strings
 * 
 * @author fabioz
 */
public class PyStringCodeCompletion extends PyCodeCompletion{
	
	
	@Override
	public List getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException, BadLocationException {
		return super.getCodeCompletionProposals(viewer, request);
	}

}
