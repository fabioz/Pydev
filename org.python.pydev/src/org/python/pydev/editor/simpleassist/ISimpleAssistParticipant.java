/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.simpleassist;

import java.util.Collection;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;

public interface ISimpleAssistParticipant {
    
    /**
     * This method should be overriden to compute the completions
     * 
     * @param activationToken this is the activation token
     * @param qualifier this is the qualifier
     * @param ps the selectio
     * @param edit the edit
     * @param offset the offset
     * 
     * @return a list of completions
     */
    Collection<ICompletionProposal> computeCompletionProposals(String activationToken, String qualifier, PySelection ps, PyEdit edit, int offset);

}
