package org.python.pydev.shared_core.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class FIFOCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxSize;

    public FIFOCache(int maxSize) {
        super(maxSize + 1, 1.0f, false); // The difference from lru to fifo is the `false` in the constructor.
        if (maxSize <= 0) {
            throw new AssertionError("Max size must be > 0.");
        }
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}