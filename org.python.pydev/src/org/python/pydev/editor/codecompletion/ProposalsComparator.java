/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * @author Fabio Zadrozny
 */
public class ProposalsComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        ICompletionProposal p1 = (ICompletionProposal) o1;
        ICompletionProposal p2 = (ICompletionProposal) o2;
        
        return p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
    }

}
