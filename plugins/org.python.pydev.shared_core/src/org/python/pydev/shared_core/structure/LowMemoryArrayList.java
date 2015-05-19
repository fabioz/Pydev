/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.structure;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An array list that has a null array backing it when created and cleared.
 *
 * @author Fabio
 */
public class LowMemoryArrayList<E> extends AbstractCollection<E>implements List<E> {

    private transient E[] data;
    private int size;

    public LowMemoryArrayList() {

    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator<E> iterator() {
        return new Iterator() {
            private int curr;

            public boolean hasNext() {
                if (data == null) {
                    return false;
                }
                return curr < size;
            }

            public Object next() {
                E e = data[curr];
                curr++;
                return e;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        if (data != null) {
            System.arraycopy(data, 0, result, 0, size);
        }
        return result;
    }

    public E[] internalArray() {
        return this.data;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        if (data != null) {
            System.arraycopy(data, 0, a, 0, size);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    public void sortAndTrim(Comparator<? super E> comparator) {
        trim();
        if (size > 1) {
            //Only need to sort it if we have more than 1 element
            Arrays.sort(data, comparator);
        }
    }

    public void trim() {
        if (size == 0) {
            data = null;

        } else if (size > 0) {
            //Trim if needed
            int oldCapacity = data.length;
            if (size < oldCapacity) {
                Object oldData[] = data;
                data = (E[]) new Object[size];
                System.arraycopy(oldData, 0, data, 0, size);
            }
        }
    }

    public void ensureCapacity(int minCapacity) {
        if (data == null) {
            if (minCapacity < 3) {
                minCapacity = 3;
            }
            data = (E[]) new Object[minCapacity];
            return;
        }
        int oldCapacity = data.length;
        if (minCapacity > oldCapacity) {
            Object oldData[] = data;
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            data = (E[]) new Object[newCapacity];
            System.arraycopy(oldData, 0, data, 0, size);
        }
    }

    @Override
    public boolean add(E o) {
        ensureCapacity(size + 1); // Increments modCount!!
        data[size++] = o;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (data == null) {
            return false;
        }
        if (o == null) {
            for (int index = 0; index < size; index++) {
                if (data[index] == null) {
                    fastRemove(index);
                    return true;
                }
            }
        } else {
            for (int index = 0; index < size; index++) {
                if (o.equals(data[index])) {
                    fastRemove(index);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     */
    private void fastRemove(int index) {
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(data, index + 1, data, index, numMoved);
        }
        data[--size] = null; // Let gc do its work
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacity(size + numNew); // Increments modCount
        System.arraycopy(a, 0, data, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clear() {
        if (data == null) {
            return;
        }
        data = null; //Let the GC work!

        size = 0;
    }

    private void RangeCheck(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    public E get(int index) {
        RangeCheck(index);
        //No need to check for null here!
        return data[index];
    }

    public E set(int index, E element) {
        throw new RuntimeException("Not implemented");
    }

    public void add(int index, E element) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        ensureCapacity(size + 1); // Increments modCount!!
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = element;
        size++;
    }

    public E remove(int index) {
        RangeCheck(index);

        E oldValue = data[index];

        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(data, index + 1, data, index, numMoved);
        }
        data[--size] = null; // Let gc do its work

        return oldValue;
    }

    public int indexOf(Object elem) {
        if (data == null) {
            return -1;
        }
        if (elem == null) {
            for (int i = 0; i < size; i++) {
                if (data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (elem.equals(data[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object elem) {
        if (data == null) {
            return -1;
        }
        if (elem == null) {
            for (int i = size - 1; i >= 0; i--) {
                if (data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                if (elem.equals(data[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public ListIterator<E> listIterator() {
        throw new RuntimeException("Not implemented");
    }

    public ListIterator<E> listIterator(int index) {
        throw new RuntimeException("Not implemented");
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new RuntimeException("Not implemented");
    }

}
