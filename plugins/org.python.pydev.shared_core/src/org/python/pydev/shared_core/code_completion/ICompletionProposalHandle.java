package org.python.pydev.shared_core.code_completion;

import org.eclipse.jface.text.IDocument;

public interface ICompletionProposalHandle {

    String getAdditionalProposalInfo();

    String getDisplayString();

    void apply(IDocument doc);

    Object getContextInformation();

    Object getElement();
}
