/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;

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
