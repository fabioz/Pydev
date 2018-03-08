package org.python.pydev.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

public class TokensList implements Iterable<IToken> {

    private final IToken[] tokens;
    private int size;

    private final List<TokensList> appended = new LowMemoryArrayList<>();

    public TokensList(IToken[] tokens) {
        this.tokens = tokens;
        if (tokens != null) {
            this.size = tokens.length;
        }
    }

    public TokensList copy() {
        TokensList ret = new TokensList(tokens);
        ret.size = size;
        ret.appended.addAll(this.appended);
        return ret;
    }

    public TokensList() {
        // Create it empty
        this.tokens = null;
    }

    public TokensList(Collection<IToken> collection) {
        this(collection.toArray(new IToken[0]));
    }

    public TokensList(IToken token) {
        this(new IToken[] { token });
    }

    public int size() {
        return size;
    }

    public boolean notEmpty() {
        return this.size > 0;
    }

    public boolean empty() {
        return this.size == 0;
    }

    public void setGeneratorType(ITypeInfo type) {
        for (IToken t : this) {
            t.setGeneratorType(type);
        }
    }

    public void addAll(TokensList lst) {
        Assert.isTrue(lst != this);
        if (lst == null || lst.size == 0) {
            return;
        }
        this.appended.add(lst);
        this.size += lst.size;
    }

    @Override
    public Iterator<IToken> iterator() {
        ArrayList<Iterator<IToken>> lst = new ArrayList<>();
        if (tokens != null) {
            lst.add(Arrays.asList(tokens).iterator());
        }
        for (TokensList t : appended) {
            lst.add(t.iterator());
        }
        return new ChainIterator<IToken>(lst);
    }

    public IToken getFirst() {
        return this.iterator().next();
    }

    @Override
    public String toString() {
        Iterator<IToken> it = iterator();
        if (!it.hasNext()) {
            return "TokensList[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TokensList[");
        while (true) {
            sb.append(it.next());
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }
}
