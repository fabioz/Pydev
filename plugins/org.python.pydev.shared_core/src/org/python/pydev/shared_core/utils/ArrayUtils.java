/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.callbacks.ICallback;

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
        Assert.isTrue(arrays.length > 0, "Arrays len must be > 0.");

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
        return mergedArray;
    }

    public static void reverse(Object[] array) {
        Object temp;
        int size = array.length;

        for (int i = 0; i < size / 2; i++) {
            temp = array[i];
            array[i] = array[size - i - 1];
            array[size - 1 - i] = temp;
        }
    }

    public static <T> T[] remove(T[] original, int element, Class componentType) {
        final T[] n = (T[]) java.lang.reflect.Array.newInstance(componentType,
                original.length - 1);

        System.arraycopy(original, 0, n, 0, element);
        System.arraycopy(original, element + 1, n, element, original.length - element - 1);
        return n;
    }

    public static <T> int indexOf(T[] original, T element) {
        for (int i = 0; i < original.length; i++) {
            if (element.equals(original)) {
                return i;
            }
        }
        return -1;
    }
}
