/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 21/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.util.Collection;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.structure.CompletionRecursionException;

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
    Collection<Object> getGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException;

    /**
     * Called when a completion is requested within a string.
     * @throws MisconfigurationException 
     * 
     * @return a list of proposals or tokens
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal
     * @see org.python.pydev.core.IToken
     */
    Collection<Object> getStringGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException;

    /**
     * Called when a completion is requested for a method parameter.
     * 
     * The completions for attributes already assigned in the local scope are already added in the default engine, so, at this
     * point, clients can add other completions (e.g.: getting other known tokens to appear there).
     * 
     * @param state The state for the completion
     * @param localScope The current local scope for the completion
     * @param interfaceForLocal a list of tokens that were called in the local scope for the passed activation token.
     * 
     * @return a list of proposals or tokens
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal
     * @see org.python.pydev.core.IToken
     */
    Collection<IToken> getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal);

    /**
     * Called when a completion is requested for some token whose type we don't know about
     * (excluding parameters -- that's handled at getCompletionsForMethodParameter)
     * 
     * E.g.: 
     *     for a in xrange(10):
     *         a.|<-- as variables created in the for are not resolved to any known type, this method is called on extensions.
     * 
     * 
     * @param state The state for the completion
     * @param localScope The current local scope for the completion
     * @param interfaceForLocal a list of tokens that were called in the local scope for the passed activation token.
     * 
     * @return a list of proposals or tokens
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal
     * @see org.python.pydev.core.IToken
     */
    Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal);

    /**
     * getCompletionsForMethodParameter is used instead (the name of the method was misleading)
     * 
     * This method is not called anymore.
     * 
     * @deprecated
     */
    @Deprecated
    Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal);

    /**
     * This is usually used to get completions when we only have a class name or path.
     * I.e.: unittest.test.TestCase or just TestCase.
     * 
     * Note that users should only ask for this if it was not found in the context already
     * (i.e.: it's preferred to find a token already imported in a scope if possible).
     * 
     * @param state: the activationToken in the state is the type for which we want completions. 
     * 
     * @return the completions given the state passed. May be null!
     * @throws CompletionRecursionException 
     */
    Collection<IToken> getCompletionsForType(ICompletionState state) throws CompletionRecursionException;

}
