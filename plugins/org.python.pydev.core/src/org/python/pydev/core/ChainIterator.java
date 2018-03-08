package org.python.pydev.core;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ChainIterator<T> implements Iterator<T> {

    private final Iterator<Iterator<T>> chain;
    private Iterator<T> currentIterator;

    public ChainIterator(List<Iterator<T>> lst) {
        this.chain = lst.iterator();
    }

    @Override
    public boolean hasNext() {
        while (currentIterator == null || !currentIterator.hasNext()) {
            if (!chain.hasNext()) {
                return false;
            }
            currentIterator = chain.next();
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
}