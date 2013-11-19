/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

public class PyCodeCompletionUtils {

    /**
     * Filters the python completions so that only the completions we care about are shown (given the qualifier) 
     * @param pythonAndTemplateProposals the completions to sort / filter
     * @param qualifier the qualifier we care about
     * @param onlyForCalltips if we should filter having in mind that we're going to show it for a calltip
     * @return the completions to show to the user
     */
    public static ICompletionProposal[] onlyValidSorted(List pythonAndTemplateProposals, String qualifier,
            boolean onlyForCalltips) {
        //FOURTH: Now, we have all the proposals, only thing is deciding which ones are valid (depending on
        //qualifier) and sorting them correctly.
        final Map<String, List<ICompletionProposal>> returnProposals = new HashMap<String, List<ICompletionProposal>>();
        final String lowerCaseQualifier = qualifier.toLowerCase();

        int len = pythonAndTemplateProposals.size();
        for (int i = 0; i < len; i++) {
            Object o = pythonAndTemplateProposals.get(i);
            if (o instanceof ICompletionProposal) {
                ICompletionProposal proposal = (ICompletionProposal) o;

                String displayString;
                if (proposal instanceof IPyCompletionProposal2) {
                    IPyCompletionProposal2 pyCompletionProposal = (IPyCompletionProposal2) proposal;
                    displayString = pyCompletionProposal.getInternalDisplayStringRepresentation();

                } else {
                    displayString = proposal.getDisplayString();
                }

                if (onlyForCalltips) {
                    if (displayString.equals(qualifier)) {
                        addProposal(returnProposals, proposal, displayString);

                    } else if (displayString.length() > qualifier.length() && displayString.startsWith(qualifier)) {
                        if (displayString.charAt(qualifier.length()) == '(') {
                            addProposal(returnProposals, proposal, displayString);
                        }
                    }
                } else if (displayString.toLowerCase().startsWith(lowerCaseQualifier)) {
                    List<ICompletionProposal> existing = returnProposals.get(displayString);
                    if (existing != null) {
                        //a proposal with the same string is already there...
                        boolean addIt = true;
                        if (proposal instanceof PyCompletionProposal) {
                            PyCompletionProposal propP = (PyCompletionProposal) proposal;

                            OUT: for (Iterator<ICompletionProposal> it = existing.iterator(); it.hasNext();) {
                                ICompletionProposal curr = it.next();
                                int overrideBehavior = propP.getOverrideBehavior(curr);

                                switch (overrideBehavior) {
                                    case PyCompletionProposal.BEHAVIOR_COEXISTS:
                                        //just go on (it will be added later)
                                        break;
                                    case PyCompletionProposal.BEHAVIOR_OVERRIDES:
                                        it.remove();
                                        break;

                                    case PyCompletionProposal.BEHAVIOR_IS_OVERRIDEN:
                                        addIt = false;
                                        break OUT;

                                }
                            }
                        }
                        if (addIt) {
                            existing.add(proposal);
                        }
                    } else {
                        //it's null, so, 1st insertion...
                        List<ICompletionProposal> lst = new ArrayList<ICompletionProposal>();
                        lst.add(proposal);
                        returnProposals.put(displayString, lst);
                    }
                }
            } else {
                throw new RuntimeException("Error: expected instanceof ICompletionProposal and received: "
                        + o.getClass().getName());
            }
        }

        // and fill with list elements
        Collection<List<ICompletionProposal>> values = returnProposals.values();
        ArrayList<ICompletionProposal> tproposals = new ArrayList<ICompletionProposal>();
        for (List<ICompletionProposal> value : values) {
            tproposals.addAll(value);
        }
        ICompletionProposal[] proposals = tproposals.toArray(new ICompletionProposal[returnProposals.size()]);

        Arrays.sort(proposals, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        return proposals;
    }

    private static void addProposal(Map<String, List<ICompletionProposal>> returnProposals,
            ICompletionProposal proposal, String displayString) {
        List<ICompletionProposal> lst = returnProposals.get(displayString);
        if (lst == null) {
            lst = new ArrayList<ICompletionProposal>();
            returnProposals.put(displayString, lst);
        }
        lst.add(proposal);
    }

}
