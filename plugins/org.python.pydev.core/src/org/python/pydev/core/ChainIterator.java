package org.python.pydev.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ChainIterator<T> implements Iterator<T> {

    private List<IObjectsList> lst = new ArrayList<>();
    private Iterator<T> currentIterator;
    private Iterator<IObjectsList> chain;

    public ChainIterator() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean hasNext() {
        while (currentIterator == null || !currentIterator.hasNext()) {
            if (!chain.hasNext()) {
                return false;
            }
            IObjectsList next = chain.next();
            currentIterator = next.buildIterator();
        }
        return true;
    }

    @Override
    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        return currentIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void add(IObjectsList tokensList) {
        lst.add(tokensList);
    }

    public void build() {
        this.chain = lst.iterator();
    }
}