package org.python.pydev.editor.correctionassist;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.FormatStd;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_ui.proposals.CompletionProposalFactory;

import junit.framework.TestCase;

public class IgnorePyLintCompletionProposalInSameLineTest extends TestCase {

    public void testApplyIgnorePyLint() throws Exception {
        Document doc = new Document("mydoc");
        PySelection ps = new PySelection(doc);
        ICompletionProposalHandle prop = CompletionProposalFactory.get()
                .createIgnorePyLintCompletionProposalInSameLine("something", ps.getEndLineOffset(), 0, 0, null,
                        "something", null,
                        null, 0, null, doc.get(), ps, new FormatStd(), null);
        prop.apply(doc);
        assertEquals("mydoc #pylint: disable=something", doc.get());

        ps = new PySelection(doc);
        prop = CompletionProposalFactory.get().createIgnorePyLintCompletionProposalInSameLine("else",
                ps.getEndLineOffset(), 0, 0, null, "else", null,
                null, 0, null, doc.get(), ps, new FormatStd(), null);
        prop.apply(doc);
        assertEquals("mydoc #pylint: disable=something, else", doc.get());
    }
}
