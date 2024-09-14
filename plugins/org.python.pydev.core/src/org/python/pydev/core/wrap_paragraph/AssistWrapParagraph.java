package org.python.pydev.core.wrap_paragraph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.IAssistProps;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.partitioner.FastPartitioner;

public class AssistWrapParagraph implements IAssistProps {

    private Paragrapher paragrapher;

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        ReplaceEdit replaceEdit = paragrapher.getReplaceEdit();

        String replacementString = replaceEdit.getText();
        int replacementOffset = replaceEdit.getOffset();
        int replacementLength = replaceEdit.getLength();
        int cursorPosition = replaceEdit.getText().length();
        String displayString = "Wrap text";
        Object contextInformation = null;
        String additionalProposalInfo = null;
        int priority = IPyCompletionProposal.PRIORITY_DEFAULT;

        IImageHandle image = imageCache != null ? imageCache.get(UIConstants.COMPLETION_TEMPLATE) : null;

        ICompletionProposalHandle proposal = CompletionProposalFactory.get().createPyCompletionProposal(
                replacementString, replacementOffset,
                replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority);
        List<ICompletionProposalHandle> ret = new ArrayList<>();
        ret.add(proposal);

        return ret;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        IDocument doc = ps.getDoc();
        ITypedRegion partition = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc)).getPartition(offset);

        // Only valid for strings or comments.
        if (!ParsingUtils.isStringContentType(partition.getType())
                && !ParsingUtils.isCommentContentType(partition.getType())) {
            return false;
        }

        int noCols = edit.getPrintMarginColums();
        paragrapher = new Paragrapher(ps, noCols);
        String errorMsg = paragrapher.getValidErrorInPos();
        if (errorMsg == null) {
            return true;
        }

        return false;
    }

}
