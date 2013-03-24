package org.python.pydev.shared_core.structure;

import java.util.LinkedHashMap;

public class OrderedMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 3463361572350039096L;

    public OrderedMap() {
        super();
    }

    public OrderedMap(int initialCapacity) {
        super(initialCapacity);
    }

}
