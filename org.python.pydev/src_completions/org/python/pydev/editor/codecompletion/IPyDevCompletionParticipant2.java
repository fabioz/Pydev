package org.python.pydev.editor.codecompletion;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;

/**
 * This is an extension to the IPyDevCompletionParticipant for gathering completions
 * from the console.
 *
 * @author Fabio
 */
public interface IPyDevCompletionParticipant2 {
    
    /**
     * Used for getting the completions to be applied when a completion
     * is requested in the console.
     * 
     * @param tokenAndQual the activation token and the qualifier used
     * @param naturesUsed the natures that the console is using
     * @param viewer the viewer for the console
     * @param requestOffset the offset where the request for completions was issued
     * @return a list of completion proposals to be applied in the console
     */
    Collection<ICompletionProposal> computeConsoleCompletions(ActivationTokenAndQual tokenAndQual, 
            List<IPythonNature> naturesUsed, IScriptConsoleViewer viewer, int requestOffset);

}
