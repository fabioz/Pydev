package org.python.pydev.core;

import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface IPyTemplateCompletionProcessor {

    void addTemplateProposals(ITextViewer viewer, int documentOffset, List<ICompletionProposal> propList);

}
