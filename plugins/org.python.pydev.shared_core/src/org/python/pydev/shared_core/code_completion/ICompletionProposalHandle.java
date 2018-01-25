package org.python.pydev.shared_core.code_completion;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;

public interface ICompletionProposalHandle {

    /**
     * Returns optional additional information about the proposal. The additional information will
     * be presented to assist the user in deciding if the selected proposal is the desired choice.
     * <p>
     * If {@link ICompletionProposalExtension5} is implemented, this method should not be called any
     * longer. This method may be deprecated in a future release.
     * </p>
     *
     * @return the additional information or <code>null</code>
     */
    String getAdditionalProposalInfo();

    /**
     * Returns the string to be displayed in the list of completion proposals.
     *
     * @return the string to be displayed
     *
     * @see ICompletionProposalExtension6#getStyledDisplayString()
     * @see ICompletionProposalExtension7#getStyledDisplayString(IDocument, int, BoldStylerProvider)
     */
    String getDisplayString();

    void apply(IDocument doc);

    Object getContextInformation();

}
