/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.simpleassist;

import java.util.Collection;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;

public interface ISimpleAssistParticipant {
    
    /**
     * This method should be overridden to compute the completions
     * 
     * @param activationToken this is the activation token
     * @param qualifier this is the qualifier
     * @param ps the selection in the editor (may be null if it's requested without using a PyEdit)
     * @param edit the edit (may be null if there's no PyEdit available. E.g. console)
     * @param offset the offset
     * 
     * @return a list of completions
     */
    Collection<ICompletionProposal> computeCompletionProposals(String activationToken, String qualifier, PySelection ps, 
            PyEdit edit, int offset);

}
