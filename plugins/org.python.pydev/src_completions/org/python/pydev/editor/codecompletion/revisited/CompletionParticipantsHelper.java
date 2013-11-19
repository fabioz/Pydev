/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;

public class CompletionParticipantsHelper {

    /**
     * Get the completions based on the arguments received
     * 
     * @param state this is the state used for the completion
     * @param localScope this is the scope we're currently on (may be null)
     */
    public static Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state,
            ILocalScope localScope) {
        IToken[] localTokens = localScope.getLocalTokens(-1, -1, false); //only to get the args
        String activationToken = state.getActivationToken();
        String firstPart = FullRepIterable.getFirstPart(activationToken);
        for (IToken token : localTokens) {
            if (token.getRepresentation().equals(firstPart)) {
                Collection<IToken> interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
                Collection<IToken> argsCompletionFromParticipants = getCompletionsForTokenWithUndefinedTypeFromParticipants(
                        state, localScope, interfaceForLocal);
                return argsCompletionFromParticipants;
            }
        }
        return getCompletionsForTokenWithUndefinedTypeFromParticipants(state, localScope, null);
    }

    /**
     * If we were unable to find its type, pass that over to other completion participants.
     */
    public static Collection<IToken> getCompletionsForTokenWithUndefinedTypeFromParticipants(ICompletionState state,
            ILocalScope localScope, Collection<IToken> interfaceForLocal) {
        ArrayList<IToken> ret = new ArrayList<IToken>();

        List<?> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getCompletionsForTokenWithUndefinedType(state, localScope, interfaceForLocal));
        }
        return ret;
    }

    /**
     * Get the completions based on the arguments received
     * 
     * @param state this is the state used for the completion
     * @param localScope this is the scope we're currently on (may be null)
     */
    public static Collection<IToken> getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope) {
        IToken[] args = localScope.getLocalTokens(-1, -1, true); //only to get the args
        String activationToken = state.getActivationToken();
        String firstPart = FullRepIterable.getFirstPart(activationToken);
        for (IToken token : args) {
            if (token.getRepresentation().equals(firstPart)) {
                Collection<IToken> interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
                Collection<IToken> argsCompletionFromParticipants = getCompletionsForMethodParameterFromParticipants(
                        state,
                        localScope, interfaceForLocal);
                for (IToken t : interfaceForLocal) {
                    if (!t.getRepresentation().equals(state.getQualifier())) {
                        argsCompletionFromParticipants.add(t);
                    }
                }
                return argsCompletionFromParticipants;
            }
        }
        return new ArrayList<IToken>();
    }

    /**
     * If we were able to find it as a method parameter, this method is called so that clients can extend those completions.
     */
    public static Collection<IToken> getCompletionsForMethodParameterFromParticipants(ICompletionState state,
            ILocalScope localScope, Collection<IToken> interfaceForLocal) {
        ArrayList<IToken> ret = new ArrayList<IToken>();

        List<?> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getCompletionsForMethodParameter(state, localScope, interfaceForLocal));
        }
        return ret;
    }

}
