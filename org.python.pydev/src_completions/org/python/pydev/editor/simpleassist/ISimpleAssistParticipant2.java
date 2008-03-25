/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.simpleassist;

import java.util.Collection;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface ISimpleAssistParticipant2 {
    
    /**
     * This method should be overridden to compute the completions for the console.
     * 
     * @param activationToken this is the activation token
     * @param qualifier this is the qualifier
     * @param offset the offset
     * 
     * @return a list of completions
     */
    Collection<ICompletionProposal> computeConsoleProposals(String activationToken, String qualifier, int offset);

}
