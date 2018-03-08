package org.python.pydev.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

public class TokensOrProposalsList implements Iterable<Object> {

    private final Object[] proposals;
    private int size;

    private final List<TokensOrProposalsList> appended = new LowMemoryArrayList<>();
    private final List<TokensList> appendedTokens = new LowMemoryArrayList<>();

    public TokensOrProposalsList(Collection<ICompletionProposalHandle> proposals) {
        this(proposals.toArray(new ICompletionProposalHandle[0]));
    }

    public TokensOrProposalsList() {
        this.proposals = null;
    }

    public TokensOrProposalsList(ICompletionProposalHandle[] proposals) {
        this.proposals = proposals;
        if (proposals != null) {
            this.size = proposals.length;
        }
    }

    public TokensOrProposalsList(Object[] array) {
        this.proposals = array;
        if (proposals != null) {
            this.size = array.length;
        }
    }

    public int size() {
        return size;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Iterator<Object> iterator() {
        ArrayList<Iterator<Object>> lst = new ArrayList<>();
        if (proposals != null) {
            lst.add(Arrays.asList(proposals).iterator());
        }
        for (TokensOrProposalsList t : appended) {
            lst.add(t.iterator());
        }
        for (TokensList t : appendedTokens) {
            lst.add((Iterator) t.iterator());
        }
        return new ChainIterator<Object>(lst);
    }

    public void addAll(TokensOrProposalsList lst) {
        Assert.isTrue(lst != this);
        if (lst == null || lst.size == 0) {
            return;
        }
        this.appended.add(lst);
    }

    public void addAll(TokensList lst) {
        if (lst == null || lst.size() == 0) {
            return;
        }
        this.appendedTokens.add(lst);
    }

    @Override
    public String toString() {
        Iterator<Object> it = iterator();
        if (!it.hasNext()) {
            return "TokensOrProposalsList[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TokensOrProposalsList[");
        while (true) {
            sb.append(it.next());
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }
}
