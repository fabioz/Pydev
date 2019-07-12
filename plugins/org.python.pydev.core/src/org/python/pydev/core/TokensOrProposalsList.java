package org.python.pydev.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

public class TokensOrProposalsList implements IObjectsList, Iterable<IterEntry> {

    private final Object[] proposals;
    private int size;

    private final List<TokensOrProposalsList> appended = new LowMemoryArrayList<>();
    private final List<TokensList> appendedTokens = new LowMemoryArrayList<>();
    private final List<TokensListMixedLookingFor> appendedMixed = new LowMemoryArrayList<>();

    private boolean freeze;

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

    @Override
    public Iterator<IterEntry> iterator() {
        ChainIterator<IterEntry> chainIterator = new ChainIterator<IterEntry>();
        this.addToIterator(chainIterator);
        chainIterator.build();
        return chainIterator;
    }

    private void addToIterator(ChainIterator<IterEntry> chainIterator) {
        if (this.proposals != null && this.proposals.length > 0) {
            chainIterator.add(this);
        }
        for (TokensOrProposalsList t : appended) {
            t.addToIterator(chainIterator);
        }
        for (TokensList t : appendedTokens) {
            t.addToIterator(chainIterator);
        }
        for (TokensListMixedLookingFor t : appendedMixed) {
            t.addToIterator(chainIterator);
        }
    }

    public void addAll(TokensOrProposalsList lst) {
        Assert.isTrue(!freeze);
        Assert.isTrue(lst != this);
        if (lst == null || lst.size == 0) {
            return;
        }
        lst.freeze(); // size cannot change
        this.appended.add(lst);
        this.size += lst.size;
    }

    private void freeze() {
        this.freeze = true;
    }

    public void addAll(TokensList lst) {
        Assert.isTrue(!freeze);
        if (lst == null || lst.size() == 0) {
            return;
        }
        lst.freeze(); // size cannot change
        this.appendedTokens.add(lst);
        this.size += lst.size();
    }

    public void addAll(TokensListMixedLookingFor tokensListMixedLookingFor) {
        Assert.isTrue(!freeze);
        if (tokensListMixedLookingFor == null || tokensListMixedLookingFor.size() == 0) {
            return;
        }
        tokensListMixedLookingFor.freeze();
        this.appendedMixed.add(tokensListMixedLookingFor);
        this.size += tokensListMixedLookingFor.size();
    }

    @Override
    public String toString() {
        Iterator<IterEntry> it = iterator();
        if (!it.hasNext()) {
            return "TokensOrProposalsList[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TokensOrProposalsList[");
        while (true) {
            sb.append(it.next().object);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public Iterator<IterEntry> buildIterator() {
        return new Iterator<IterEntry>() {
            IterEntry iterEntry = new IterEntry();
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < proposals.length;
            }

            @Override
            public IterEntry next() {
                Object object = proposals[i];
                i++;
                iterEntry.object = object;
                return iterEntry;
            }
        };
    }

}
