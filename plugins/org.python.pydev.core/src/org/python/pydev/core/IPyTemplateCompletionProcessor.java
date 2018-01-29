package org.python.pydev.core;

import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

public interface IPyTemplateCompletionProcessor {

    void addTemplateProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposalHandle> propList);

}
