/*
 * Created on 21/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.util.Collection;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;

/**
 * This interface defines the basic behavior for a class that wants to participate in the code-completion process. 
 * 
 * @author Fabio
 */
public interface IPyDevCompletionParticipant {

    /**
     * PyDev can have code completion participants, that may return a list of:
     * ICompletionProposal
     * IToken (will be automatically converted to completion proposals)
     * 
     * @param request the request that was done for the completion
     * @param state the state for the completion
     * 
     * @return a list of proposals or tokens
     * @throws MisconfigurationException 
     * 
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal
     * @see org.python.pydev.core.IToken
     */
    Collection<Object> getGlobalCompletions(CompletionRequest request, ICompletionState state) throws MisconfigurationException;
    
    /**
     * Called when a completion is requested within a string.
     * @throws MisconfigurationException 
     */
    Collection<Object> getStringGlobalCompletions(CompletionRequest request, ICompletionState state) throws MisconfigurationException;

    /**
     * Called when a completion is requested for some argument.
     */
    Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope, Collection<IToken> interfaceForLocal);

}
