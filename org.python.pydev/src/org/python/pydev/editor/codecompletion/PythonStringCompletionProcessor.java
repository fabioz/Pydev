package org.python.pydev.editor.codecompletion;

import org.python.pydev.editor.PyEdit;

public class PythonStringCompletionProcessor extends PythonCompletionProcessor{

	public PythonStringCompletionProcessor(PyEdit edit, PyContentAssistant pyContentAssistant) {
		super(edit, pyContentAssistant);
	}
	
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		//no auto-activation within strings.
		return new char[]{};
	}

}
