package org.python.pydev.core;

import java.util.Collection;
import java.util.LinkedHashSet;

public class OrderedSet<E> extends LinkedHashSet<E> {

    private static final long serialVersionUID = -9140695560309322962L;

    public OrderedSet() {

    }

    public OrderedSet(Collection<? extends E> c) {
        super(c);
    }

    public OrderedSet(int initialCapacity) {
        super(initialCapacity);
    }

}
