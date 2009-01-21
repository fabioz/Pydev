package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.editor.PyEdit;

public class PythonStringCompletionProcessor extends PythonCompletionProcessor{

    public PythonStringCompletionProcessor(PyEdit edit, PyContentAssistant pyContentAssistant) {
        super(edit, pyContentAssistant);
    }
    
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        //no auto-activation within strings.
        return new char[]{'@'};
    }

    protected IPyCodeCompletion getCodeCompletionEngine() {
        return new PyStringCodeCompletion();
    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return new IContextInformationValidator(){

            public void install(IContextInformation info, ITextViewer viewer, int offset) {
            }

            public boolean isContextInformationValid(int offset) {
                return true;
            }
            
        };
    }
    
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
        return new IContextInformation[]{};
    }

}
