/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.callbacks.ICallback;

public class ArrayUtils {

    /**
     * @param listToFilter the list that should be filtered.
     * @param callbackThatFilters if true is returned in the callback, that means that the object should be added to the
     * resulting list.
     */
    public static <T> List<T> filter(T[] listToFilter, ICallback<Boolean, T> callbackThatFilters) {
        ArrayList<T> lst = new ArrayList<T>();
        for (T marker : listToFilter) {
            if (callbackThatFilters.call(marker)) {
                lst.add(marker);
            }
        }
        return lst;
    }

    public static <T> T[] concatArrays(T[]... arrays) {

        int count = 0;
        for (T[] array : arrays) {
            count += array.length;
        }

        final T[] mergedArray = (T[]) java.lang.reflect.Array.newInstance(arrays[0].getClass().getComponentType(),
                count);

        int start = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return (T[]) mergedArray;
    }

}
