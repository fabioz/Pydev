/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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

    public static boolean contains(Object[] array, Object o) {
        if (array == null) {
            return false;
        }
        return Arrays.asList(array).contains(o);
    }

    public static <T> Set<T> asSet(T... objects) {
        return new HashSet<>(Arrays.asList(objects));
    }

    public static int[] reversedCopy(int[] regionsForSave) {
        int[] copyOf = Arrays.copyOf(regionsForSave, regionsForSave.length);
        reverse(copyOf);
        return copyOf;
    }

    private static void reverse(int[] copyOf) {
        int length = copyOf.length;
        int middle = length / 2;
        for (int i = 0; i < middle; i++) {
            int temp = copyOf[i];
            copyOf[i] = copyOf[length - i - 1];
            copyOf[length - i - 1] = temp;
        }
    }

    /**
     * This class receives through `addArray` multiple arrays.
     * Afterwards, it can be iterated using hasNext/next.
     */
    public static final class ArraysIterator<T> implements Iterator<T> {

        private final List<T[]> iterThroughArrays = new ArrayList<>();
        private int currentArrayIndex = 0;
        private int currentElementIndex = 0;
        private boolean frozen = false;

        public void addArray(T[] array) {
            if (frozen) {
                throw new RuntimeException("ArraysIterator already frozen");
            }
            if (array != null) {
                iterThroughArrays.add(array);
            }
        }

        @Override
        public boolean hasNext() {
            frozen = true;
            if (iterThroughArrays.isEmpty()) {
                return false;
            }

            if (currentArrayIndex >= iterThroughArrays.size()) {
                return false;
            }

            T[] currentArray = iterThroughArrays.get(currentArrayIndex);
            if (currentElementIndex >= currentArray.length) {
                // Try next array
                currentArrayIndex++;
                currentElementIndex = 0;
                return hasNext(); // Recurse to check next array
            }

            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            T[] currentArray = iterThroughArrays.get(currentArrayIndex);
            T element = currentArray[currentElementIndex];
            currentElementIndex++;
            return element;
        }
    }
}
