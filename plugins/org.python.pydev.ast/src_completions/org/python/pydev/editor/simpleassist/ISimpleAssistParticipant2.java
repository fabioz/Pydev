/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
