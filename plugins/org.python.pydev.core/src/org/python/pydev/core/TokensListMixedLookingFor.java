package org.python.pydev.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class TokensListMixedLookingFor implements IObjectsList {

    private IterTokenEntry[] values;

    public TokensListMixedLookingFor(Collection<IterTokenEntry> values) {
        this.values = values.toArray(new IterTokenEntry[0]);
    }

    protected void addToIterator(@SuppressWarnings("rawtypes") ChainIterator chainIterator) {
        if (this.values != null && this.values.length > 0) {
            chainIterator.add(this);
        }
    }

    @Override
    public Iterator<IterTokenEntry> buildIterator() {
        return Arrays.asList(values).iterator();
    }

    public int size() {
        return values.length;
    }

    public void freeze() {
        // Already frozen by default
    }

}
