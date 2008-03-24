package org.python.pydev.debug.newconsole;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.dltk.console.IScriptConsoleShell;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.editor.codecompletion.PythonCompletionProcessor;
import org.python.pydev.editor.simpleassist.SimpleAssistProcessor;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Gathers completions for the pydev console.
 * 
 * @author fabioz
 */
public class PydevConsoleCompletionProcessor implements IContentAssistProcessor {

	/**
	 * Checks to see if the context information is valid.
	 */
    protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {

        protected int installOffset;

        public boolean isContextInformationValid(int offset) {
            return Math.abs(installOffset - offset) < 5;
        }

        public void install(IContextInformation info, ITextViewer viewer, int offset) {
            installOffset = offset;
        }

        public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
            return false;
        }
    }

    private IContextInformationValidator validator;
    private IScriptConsoleShell interpreterShell;
    private String errorMessage = null;

    @SuppressWarnings("unchecked")
    public PydevConsoleCompletionProcessor(IScriptConsoleShell interpreterShell) {
        this.interpreterShell = interpreterShell;

    }
    
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return SimpleAssistProcessor.getStaticAutoActivationCharacters(
        		PythonCompletionProcessor.getStaticCompletionProposalAutoActivationCharacters(), 0);
    }

    /**
     * Get the completions
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer v, int offset) {
        IScriptConsoleViewer viewer = (IScriptConsoleViewer) v;

        try {
            String commandLine = viewer.getCommandLine();
            int cursorPosition = offset - viewer.getCommandLineOffset();

            return interpreterShell.getCompletions(viewer, commandLine, cursorPosition, offset);
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
            PydevPlugin.log(e);
        }

        return new ICompletionProposal[] {};
    }

    public IContextInformation[] computeContextInformation(ITextViewer v, int offset) {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        if (validator == null) {
            validator = new Validator();
        }

        return validator;
    }
    
    /**
     * @return an error message that happened while getting the completions
     */
    public String getErrorMessage() {
        String msg = errorMessage;
        errorMessage = null;
        return msg;
    }
}
