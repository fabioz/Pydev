package org.python.pydev.shared_core.threaded_objects_pool;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is a pool where we can have at most maxSize objects in it.
 *
 * Note that we may have several objects with the same configuration in the
 * pool (and we ask for any which matches that configuration and later
 * put objects given a configuration).
 *
 * Clients are responsible for actually creating the objects.
 */
public class ThreadedObjectsPool<X> {

    private final int maxSize;
    private final List<Tuple<Object, X>> lst;
    private final Object lock = new Object();

    public ThreadedObjectsPool(int maxSize) {
        Assert.isTrue(maxSize > 0);
        this.maxSize = maxSize;
        lst = new LinkedListWarningOnSlowOperations<>();
    }

    /**
     * Returns null if there's no object for the given configuration.
     */
    public X getObject(Object configuration) {
        synchronized (lock) {
            Iterator<Tuple<Object, X>> iterator = lst.iterator();
            while (iterator.hasNext()) {
                Tuple<Object, X> tup = iterator.next();
                if (tup.o1.equals(configuration)) {
                    iterator.remove();
                    return tup.o2;
                }
            }
        }
        return null;
    }

    /**
     * Puts some object in the store.
     */
    public void putObject(Object configuration, X obj) {
        Assert.isNotNull(obj);
        synchronized (lock) {
            while (lst.size() + 1 > this.maxSize) {
                lst.remove(0);
            }
            lst.add(new Tuple<Object, X>(configuration, obj));
        }
    }

}
