package org.python.pydev.editor.correctionassist;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

import junit.framework.TestCase;

public class IgnoreFlake8CompletionProposalInSameLineTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompletionProposalFactory.set(null);
    }

    public void testApplyIgnorePyLint() throws Exception {
        Document doc = new Document("mydoc");
        PySelection ps = new PySelection(doc);
        ICompletionProposalHandle prop = CompletionProposalFactory.get()
                .createIgnoreFlake8CompletionProposalInSameLine("something", ps.getEndLineOffset(), 0, 0, null,
                        "something", null,
                        null, 0, null, doc.get(), ps, new FormatStd(), null);
        prop.apply(doc);
        assertEquals("mydoc #noqa:something", doc.get());

        ps = new PySelection(doc);
        prop = CompletionProposalFactory.get().createIgnoreFlake8CompletionProposalInSameLine("else",
                ps.getEndLineOffset(), 0, 0, null, "else", null,
                null, 0, null, doc.get(), ps, new FormatStd(), null);
        prop.apply(doc);
        assertEquals("mydoc #noqa:something, else", doc.get());
    }
}
