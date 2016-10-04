package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import junit.framework.TestCase;

public class ProposalsComparatorTest extends TestCase {

    private static class Dummy1 implements ICompletionProposal {

        private String string;

        public Dummy1(String string) {
            this.string = string;
        }

        @Override
        public void apply(IDocument document) {
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            return null;
        }

        @Override
        public String getDisplayString() {
            return this.string;
        }

        @Override
        public Image getImage() {
            return null;
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }
    }

    public void testProposalsComparator() {
        ProposalsComparator proposalsComparator = new ProposalsComparator("foo", null);

        // alphabetical (ignore case)
        assertEquals(1, proposalsComparator.compare(new Dummy1("bar"), new Dummy1("aBar")));
        assertEquals(-1, proposalsComparator.compare(new Dummy1("abar"), new Dummy1("Bar")));

        // _ is always the last
        assertEquals(1, proposalsComparator.compare(new Dummy1("_bar"), new Dummy1("abar")));
        assertEquals(-1, proposalsComparator.compare(new Dummy1("Zbar"), new Dummy1("_bar")));
    }

}
