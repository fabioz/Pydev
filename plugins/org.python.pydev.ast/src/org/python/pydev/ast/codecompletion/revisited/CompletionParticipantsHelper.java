/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.ast.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.ast.codecompletion.IPyDevCompletionParticipant3;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.string.FullRepIterable;

public class CompletionParticipantsHelper {

    /**
     * Get the completions based on the arguments received
     *
     * @param state this is the state used for the completion
     * @param localScope this is the scope we're currently on (may be null)
     * @throws CompletionRecursionException
     */
    public static TokensList getCompletionsForTokenWithUndefinedType(ICompletionState state,
            ILocalScope localScope) throws CompletionRecursionException {
        TokensList localTokens = localScope.getLocalTokens(-1, -1, false); //only to get the args
        String activationToken = state.getActivationToken();
        String firstPart = FullRepIterable.getFirstPart(activationToken);
        for (IterTokenEntry entry : localTokens) {
            IToken token = entry.getToken();
            if (token.getRepresentation().equals(firstPart)) {
                TokensList interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
                TokensList argsCompletionFromParticipants = getCompletionsForTokenWithUndefinedTypeFromParticipants(
                        state, localScope, interfaceForLocal);
                return argsCompletionFromParticipants;
            }
        }
        return getCompletionsForTokenWithUndefinedTypeFromParticipants(state, localScope, null);
    }

    /**
     * If we were unable to find its type, pass that over to other completion participants.
     * @throws CompletionRecursionException
     */
    private static TokensList getCompletionsForTokenWithUndefinedTypeFromParticipants(ICompletionState state,
            ILocalScope localScope, TokensList interfaceForLocal) throws CompletionRecursionException {
        TokensList ret = new TokensList();

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
     * @throws CompletionRecursionException
     */
    public static TokensList getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope)
            throws CompletionRecursionException {
        TokensList args = localScope.getLocalTokens(-1, -1, true); //only to get the args
        String activationToken = state.getActivationToken();
        String firstPart = FullRepIterable.getFirstPart(activationToken);
        for (IterTokenEntry entry : args) {
            IToken token = entry.getToken();
            if (token.getRepresentation().equals(firstPart)) {
                TokensList interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
                TokensList argsCompletionFromParticipants = getCompletionsForMethodParameterFromParticipants(
                        state, localScope, interfaceForLocal);
                final List<IToken> lst = new ArrayList<>();
                for (IterTokenEntry entry1 : interfaceForLocal) {
                    IToken t = entry1.getToken();
                    if (!t.getRepresentation().equals(state.getQualifier())) {
                        lst.add(t);
                    }
                }
                argsCompletionFromParticipants.addAll(new TokensList(lst));
                return argsCompletionFromParticipants;
            }
        }
        return new TokensList();
    }

    /**
     * If we were able to find it as a method parameter, this method is called so that clients can extend those completions.
     * @throws CompletionRecursionException
     */
    public static TokensList getCompletionsForMethodParameterFromParticipants(ICompletionState state,
            ILocalScope localScope, TokensList interfaceForLocal) throws CompletionRecursionException {
        TokensList ret = new TokensList();

        List<?> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getCompletionsForMethodParameter(state, localScope, interfaceForLocal));
        }
        return ret;
    }

    public static IDefinition findDefinitionForMethodParameterFromParticipants(Definition d, IPythonNature nature,
            ICompletionState completionCache) {
        List<?> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator<?> iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            if (participant instanceof IPyDevCompletionParticipant3) {
                IPyDevCompletionParticipant3 iPyDevCompletionParticipant3 = (IPyDevCompletionParticipant3) participant;
                IDefinition ret = iPyDevCompletionParticipant3.findDefinitionForMethodParameter(d, nature,
                        completionCache);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

}
