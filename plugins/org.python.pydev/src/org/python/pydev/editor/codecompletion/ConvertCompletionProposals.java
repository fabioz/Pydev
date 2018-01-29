package org.python.pydev.editor.codecompletion;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

public class ConvertCompletionProposals {

    public static ICompletionProposal[] convertHandlesToProposals(ICompletionProposalHandle[] proposals) {
        List<ICompletionProposalHandle> asList = Arrays.asList(proposals);
        ICompletionProposal[] ret = new ICompletionProposal[asList.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (ICompletionProposal) asList.get(i);
        }
        return ret;
    }

}
