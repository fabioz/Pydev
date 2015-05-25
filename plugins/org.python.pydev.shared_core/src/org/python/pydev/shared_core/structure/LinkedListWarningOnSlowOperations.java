package org.python.pydev.shared_core.structure;

import java.util.Collection;
import java.util.LinkedList;

import org.python.pydev.shared_core.log.Log;

public class LinkedListWarningOnSlowOperations<T> extends LinkedList<T> {

    private static final long serialVersionUID = -3818091184735547024L;

    public LinkedListWarningOnSlowOperations(Collection<T> subList) {
        super(subList);
    }

    public LinkedListWarningOnSlowOperations() {
    }

    @Override
    public T get(int index) {
        Log.log("Performance warning: LinkedList.get() being called. Consider using another List implementation!");
        return super.get(index);
    }

}
