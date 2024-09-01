package org.python.pydev.editor.correctionassist.docstrings;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docstrings.AssistDocstringProposalCore;
import org.python.pydev.core.docutils.PySelection.DocstringInfo;
import org.python.pydev.editor.codecompletion.proposals.PyCompletionProposal;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class AssistDocstringCompletionProposal extends PyCompletionProposal {

    private final AssistDocstringProposalCore core;

    public AssistDocstringCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext, String initial, String delimiter, String docStringMarker,
            String delimiterAndIndent, String preferredDocstringStyle2, boolean inFunctionLine,
            DocstringInfo finalDocstringFromFunction, String indentation, FastStringBuffer buf,
            List<String> params) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, compareContext);
        core = new AssistDocstringProposalCore(initial, delimiter, docStringMarker, delimiterAndIndent,
                preferredDocstringStyle2, inFunctionLine, finalDocstringFromFunction, indentation, buf, params,
                replacementString, replacementOffset, replacementLength, cursorPosition);
    }

    @Override
    public void apply(IDocument document) {
        ReplaceEdit replaceEdit = core.createTextEdit(document);

        // We need to update some of our fields for the proper selection afterwards.
        this.fReplacementString = core.fReplacementString;
        this.fReplacementOffset = core.fReplacementOffset;
        this.fReplacementLength = core.fReplacementLength;
        this.fCursorPosition = core.fCursorPosition;

        try {
            replaceEdit.apply(document);
        } catch (MalformedTreeException | BadLocationException e) {
            Log.log(e);
        }
    }
}