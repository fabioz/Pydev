package org.python.pydev.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.ICompletionState.LookingFor;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

public class TokensList implements IObjectsList, Iterable<IterTokenEntry> {

    protected final IToken[] tokens;
    private int size;

    private final List<TokensList> appended = new LowMemoryArrayList<>();
    private LookingFor lookingFor;
    private boolean freeze;
    private boolean mapsToTypeVar;

    public TokensList(IToken[] tokens) {
        this.tokens = tokens;
        if (tokens != null && tokens.length > 0) {
            this.size = tokens.length;
        }
    }

    public TokensList copy() {
        TokensList ret = new TokensList(tokens);
        ret.size = size;
        ret.freeze = true;
        ret.lookingFor = lookingFor;
        ret.mapsToTypeVar = mapsToTypeVar;
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
        for (IterTokenEntry t : this) {
            t.getToken().setGeneratorType(type);
        }
    }

    public void addAll(TokensList lst) {
        if (freeze) {
            throw new AssertionError("Cannot add items after TokensList is frozen.");
        }
        if (lst == this) {
            throw new AssertionError("Cannot add a list to itself.");
        }
        if (lst == null || lst.size == 0) {
            return;
        }
        this.appended.add(lst);
        this.size += lst.size;
    }

    @Override
    public Iterator<IterTokenEntry> iterator() {
        ChainIterator<IterTokenEntry> chainIterator = new ChainIterator<IterTokenEntry>();
        this.addToIterator(chainIterator);
        chainIterator.build();
        return chainIterator;
    }

    protected void addToIterator(@SuppressWarnings("rawtypes") ChainIterator chainIterator) {
        if (this.tokens != null && this.tokens.length > 0) {
            chainIterator.add(this);
        }
        for (TokensList t : appended) {
            t.addToIterator(chainIterator);
        }
    }

    public IToken getFirst() {
        return this.iterator().next().getToken();
    }

    @Override
    public String toString() {
        Iterator<IterTokenEntry> it = iterator();
        if (!it.hasNext()) {
            return "TokensList[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TokensList[");
        while (true) {
            sb.append(it.next().object);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    public void setLookingFor(LookingFor lookingFor) {
        this.freeze(); // When we set what we're looking for we freeze our contents (because we set on our contents too).
        this.lookingFor = lookingFor;
        for (TokensList t : appended) {
            t.setLookingFor(lookingFor);
        }
    }

    public LookingFor getLookingFor() {
        if (lookingFor != null) {
            return lookingFor;
        }
        for (TokensList t : appended) {
            LookingFor lookingFor2 = t.getLookingFor();
            if (lookingFor2 != null) {
                return lookingFor2;
            }
        }
        return null;
    }

    @Override
    public Iterator<IterTokenEntry> buildIterator() {
        return new Iterator<IterTokenEntry>() {
            IterTokenEntry iterTokenEntry = new IterTokenEntry();
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < tokens.length;
            }

            @Override
            public IterTokenEntry next() {
                IToken object = tokens[i];
                i++;
                iterTokenEntry.object = object;
                iterTokenEntry.lookingFor = lookingFor;
                return iterTokenEntry;
            }
        };
    }

    public void freeze() {
        this.freeze = true;
    }

    public void setMapsToTypeVar(boolean b) {
        this.mapsToTypeVar = b;
    }

    public boolean getMapsToTypeVar() {
        return this.mapsToTypeVar;
    }

    public IToken find(String qualifier) {
        for (IterTokenEntry entry : this) {
            Object obj = entry.object;
            if (obj instanceof IToken) {
                IToken tok = (IToken) obj;
                String rep = tok.getRepresentation();
                if (qualifier.equals(rep)) {
                    return tok;
                }
            }
        }
        return null;
    }

}
