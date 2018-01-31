/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion.simpleassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant2;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_ui.proposals.CompletionProposalFactory;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

/**
 * Auto completion for keywords:
 *
 * import keyword
 * >>> for k in keyword.kwlist: print k
and
assert
break
class
continue
def
del
elif
else
except
exec
finally
for
from
global
if
import
in
is
lambda
not
or
pass
print
raise
return
try
while
yield
 * @author Fabio
 */
public class KeywordsSimpleAssist implements ISimpleAssistParticipant, ISimpleAssistParticipant2 {

    /**
     * @see ISimpleAssistParticipant
     */
    @Override
    public Collection<ICompletionProposalHandle> computeCompletionProposals(String activationToken, String qualifier,
            PySelection ps, IPySyntaxHighlightingAndCodeCompletionEditor edit, int offset) {
        boolean isPy3Syntax = false;
        if (PyCodeCompletionPreferences.forcePy3kPrintOnPy2()) {
            isPy3Syntax = true;

        } else {
            try {
                IPythonNature nature = edit.getPythonNature();
                if (nature != null) {
                    isPy3Syntax = nature.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
                }
            } catch (MisconfigurationException e) {
            }
        }
        return innerComputeProposals(activationToken, qualifier, offset, false, isPy3Syntax);
    }

    /**
     * @see ISimpleAssistParticipant2
     */
    @Override
    public Collection<ICompletionProposalHandle> computeConsoleProposals(String activationToken, String qualifier,
            int offset) {
        return innerComputeProposals(activationToken, qualifier, offset, true, false);
    }

    /**
     * Collects simple completions (keywords)
     *
     * @param activationToken activation token used
     * @param qualifier qualifier used
     * @param offset offset at which the completion was requested
     * @param buildForConsole whether the completions should be built for the console or not
     * @param isPy3Syntax if py 3 syntax we'll treat print differently.
     * @return a list with the completions available.
     */
    private Collection<ICompletionProposalHandle> innerComputeProposals(String activationToken, String qualifier,
            int offset,
            boolean buildForConsole, boolean isPy3Syntax) {

        List<ICompletionProposalHandle> results = new ArrayList<>();
        //check if we have to use it
        if (!PyCodeCompletionPreferences.useKeywordsCodeCompletion()) {
            return results;
        }

        //get them
        int qlen = qualifier.length();
        if (activationToken.isEmpty() && !qualifier.isEmpty()) {
            for (String keyw : PyCodeCompletionPreferences.getKeywords()) {
                if (keyw.startsWith(qualifier) && !keyw.equals(qualifier)) {
                    if (buildForConsole) {
                        //In the console, only show the simple completions without any special treatment
                        results.add(
                                CompletionProposalFactory.get().createPyCompletionProposal(keyw, offset - qlen, qlen,
                                        keyw.length(), IPyCompletionProposal.PRIORITY_DEFAULT, null));

                    } else {
                        //in the editor, we'll create a special proposal with more features
                        if (isPy3Syntax) {
                            if ("print".equals(keyw)) {
                                keyw = "print()";//Handling print in py3k.
                            }
                        }
                        results.add(
                                CompletionProposalFactory.get().createSimpleAssistProposal(keyw, offset - qlen, qlen,
                                        keyw.length(), IPyCompletionProposal.PRIORITY_DEFAULT, null));
                    }
                }
            }
        }

        return results;
    }

}
