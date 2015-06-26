/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.string;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

/**
 * This is a custom string buffer optimized for append(), clear() and deleteLast().
 *
 * Basically it aims at being created once, being used for something, having clear() called and then reused
 * (ultimately providing minimum allocation/garbage collection overhead for that use-case).
 *
 * append() is optimizing by doing less checks (so, exceptions thrown may be uglier on invalid operations
 * and null is not checked for in the common case -- use appendObject if it may be null).
 *
 * clear() and deleteLast() only change the internal count and have almost zero overhead.
 *
 * Note that it's also not synchronized.
 *
 * @author Fabio
 */
public final class FastStringBuffer implements CharSequence {

    /**
     * Holds the actual chars
     */
    private char[] value;

    /**
     * Count for which chars are actually used
     */
    private int count;

    /**
     * Initializes with a default initial size (128 chars)
     */
    public FastStringBuffer() {
        this(128);
    }

    /**
     * An initial size can be specified (if available and given for no allocations it can be more efficient)
     */
    public FastStringBuffer(int initialSize) {
        this.value = new char[initialSize];
        this.count = 0;
    }

    public FastStringBuffer(char[] internalBuffer) {
        this.value = internalBuffer;
        this.count = internalBuffer.length;
    }

    /**
     * Will de-allocate the internal char[] (if > 128 chars)
     */
    public void clearMemory() {
        if (this.value.length > 128) {
            this.value = null; //make it available for gc before allocating the new memory.
            this.value = new char[128];
        }
        this.count = 0;
    }

    /**
     * initializes from a string and the additional size for the buffer
     *
     * @param s string with the initial contents
     * @param additionalSize the additional size for the buffer
     */
    public FastStringBuffer(String s, int additionalSize) {
        this.count = s.length();
        if (additionalSize < 0) {
            additionalSize = 0;
        }
        value = new char[this.count + additionalSize];
        s.getChars(0, this.count, value, 0);
    }

    /**
     * Appends a string to the buffer. Passing a null string will throw an exception.
     */
    public FastStringBuffer append(String string) {
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;

        return this;
    }

    /**
     * Appends a string to the buffer. The buffer must have enough pre-allocated space for it to succeed.
     *
     * Passing a null string will throw an exception.
     * Not having a pre-allocated internal array big enough will throw an exception.
     */
    public FastStringBuffer appendNoResize(String string) {
        int strLen = string.length();
        string.getChars(0, strLen, value, this.count);
        this.count = count + strLen;
        return this;
    }

    /**
     * Resizes the internal buffer to have at least the minimum capacity passed (but may be more)
     * This code was  inlined on all methods and it's kept here to use as a reference when needed
     * (and to be used from clients to pre-reserve space).
     */
    public void resizeForMinimum(int minimumCapacity) {
        int newCapacity = (value.length + 1) * 2;
        if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }
        char newValue[] = new char[newCapacity];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
    }

    /**
     * Appends an int to the buffer.
     */
    public FastStringBuffer append(int n) {
        //Inlined: append(String.valueOf(n));
        String string = String.valueOf(n);
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;
        return this;
    }

    /**
     * Appends a char to the buffer.
     */
    public FastStringBuffer append(char n) {
        if (count + 1 > value.length) {
            //was: resizeForMinimum(newCount);
            int minimumCapacity = count + 1;
            int newCapacity = (value.length + 1) * 2;
            if (minimumCapacity > newCapacity) {
                newCapacity = minimumCapacity;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        value[count] = n;
        count++;
        return this;
    }

    /**
     * Appends a char to the buffer. Use when the size allocated is usually already ok (will only resize on exception
     * instead of doing a size check all the time).
     */
    public void appendResizeOnExc(char n) {
        try {
            value[count] = n;
        } catch (Exception e) {
            //System.out.println("Had to resize for minimun: "+(count+1));
            //was: resizeForMinimum(newCount);
            int minimumCapacity = count + 1;
            int newCapacity = (value.length + 1) * 2;
            if (minimumCapacity > newCapacity) {
                newCapacity = minimumCapacity;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
            value[count] = n;
        }
        count++;
    }

    /**
     * Appends a long to the buffer.
     */
    public FastStringBuffer append(long n) {
        //Inlined: append(String.valueOf(b));
        String string = String.valueOf(n);
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;
        return this;
    }

    /**
     * Appends a boolean to the buffer.
     */
    public FastStringBuffer append(boolean b) {
        //Inlined: append(String.valueOf(b));
        String string = String.valueOf(b);
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;
        return this;
    }

    /**
     * Appends a double to the buffer.
     */
    public FastStringBuffer append(double b) {
        //Inlined: append(String.valueOf(b));
        String string = String.valueOf(b);
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;
        return this;
    }

    /**
     * Appends an array of chars to the buffer.
     */
    public FastStringBuffer append(char[] chars) {
        int newCount = count + chars.length;
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(chars, 0, value, count, chars.length);
        count = newCount;
        return this;
    }

    /**
     * Appends another buffer to this buffer.
     */
    public FastStringBuffer append(FastStringBuffer other) {
        //Inlined append(other.value, 0, other.count);
        int len = other.count;
        int newCount = count + len;
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(other.value, 0, value, count, len);
        count = newCount;
        return this;
    }

    /**
     * Appends an array of chars to this buffer, starting at the offset passed with the length determined.
     */
    public FastStringBuffer append(char[] chars, int offset, int len) {
        int newCount = count + len;
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(chars, offset, value, count, len);
        count = newCount;
        return this;
    }

    /**
     * Reverses the contents on this buffer
     */
    public FastStringBuffer reverse() {
        final int limit = count / 2;
        for (int i = 0; i < limit; ++i) {
            char c = value[i];
            value[i] = value[count - i - 1];
            value[count - i - 1] = c;
        }
        return this;
    }

    /**
     * Clears this buffer.
     */
    public FastStringBuffer clear() {
        this.count = 0;
        return this;
    }

    /**
     * @return the length of this buffer
     */
    public int length() {
        return this.count;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    /**
     * @return a new string with the contents of this buffer.
     */
    @Override
    public String toString() {
        return new String(value, 0, count);
    }

    /**
     * @return a new char array with the contents of this buffer.
     */
    public char[] toCharArray() {
        char[] v = new char[count];
        System.arraycopy(value, 0, v, 0, count);
        return v;
    }

    /**
     * Erases the last char in this buffer
     */
    public void deleteLast() {
        if (this.count > 0) {
            this.count--;
        }
    }

    /**
     * @param length
     * @return
     */
    public FastStringBuffer deleteLastChars(int charsToDelete) {
        this.count -= charsToDelete;
        if (this.count < 0) {
            this.count = 0;
        }
        return this;
    }

    public void deleteFirstChars(int charsToDelete) {
        System.arraycopy(value, 0 + charsToDelete, value, 0, count - 0 - charsToDelete);
        count -= charsToDelete;
    }

    /**
     * @return the char given at a specific position of the buffer (no bounds check)
     */
    public char charAt(int i) {
        return this.value[i];
    }

    /**
     * Inserts a string at a given position in the buffer.
     */
    public FastStringBuffer insert(int offset, String str) {
        int len = str.length();
        int newCount = count + len;
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(0, len, value, offset);
        count = newCount;
        return this;
    }

    /**
     * Inserts a string at a given position in the buffer.
     */
    public FastStringBuffer insert(int offset, FastStringBuffer str) {
        int len = str.length();
        int newCount = count + len;
        if (newCount > value.length) {
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        System.arraycopy(str.value, 0, value, offset, str.count);
        count = newCount;
        return this;
    }

    /**
     * Inserts a char at a given position in the buffer.
     */
    public FastStringBuffer insert(int offset, char c) {
        int newCount = count + 1;
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        System.arraycopy(value, offset, value, offset + 1, count - offset);
        value[offset] = c;
        count = newCount;
        return this;
    }

    /**
     * Appends object.toString(). If null, "null" is appended.
     */
    public FastStringBuffer appendObject(Object object) {
        //Inlined: return append(object != null ? object.toString() : "null");
        String string = object != null ? object.toString() : "null";
        int strLen = string.length();
        int newCount = count + strLen;

        if (newCount > this.value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;

        return this;
    }

    /**
     * Sets the new size of this buffer (warning: use with care: no validation is done of the len passed)
     * @return
     */
    public FastStringBuffer setCount(int newLen) {
        this.count = newLen;
        return this;
    }

    public FastStringBuffer delete(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count) {
            end = count;
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException();
        }
        int len = end - start;
        if (len > 0) {
            System.arraycopy(value, start + len, value, start, count - end);
            count -= len;
        }
        return this;
    }

    public FastStringBuffer replace(int start, int end, String str) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (start > count) {
            throw new StringIndexOutOfBoundsException("start > length()");
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException("start > end");
        }
        if (end > count) {
            end = count;
        }

        if (end > count) {
            end = count;
        }
        int len = str.length();
        int newCount = count + len - (end - start);
        if (newCount > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (newCount > newCapacity) {
                newCapacity = newCount;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }

        System.arraycopy(value, end, value, start + len, count - end);
        str.getChars(0, len, value, start);
        count = newCount;
        return this;
    }

    /**
     * Replaces all the occurrences of a string in this buffer for another string and returns the
     * altered version.
     */
    public FastStringBuffer replaceAll(String replace, String with) {
        int replaceLen = replace.length();
        int withLen = with.length();

        Assert.isTrue(replaceLen > 0);

        int matchPos = 0;
        for (int i = 0; i < this.count; i++) {
            if (this.value[i] == replace.charAt(matchPos)) {
                matchPos++;
                if (matchPos == replaceLen) {
                    this.replace(i - (replaceLen - 1), i + 1, with);
                    matchPos = 0;
                    i = i - (replaceLen - withLen);
                }
                continue;
            } else {
                matchPos = 0;
            }
        }

        return this;
    }

    public FastStringBuffer replaceFirst(String replace, String with) {
        int replaceLen = replace.length();

        Assert.isTrue(replaceLen > 0);

        int matchPos = 0;
        for (int i = 0; i < this.count; i++) {
            if (this.value[i] == replace.charAt(matchPos)) {
                matchPos++;
                if (matchPos == replaceLen) {
                    this.replace(i - (replaceLen - 1), i + 1, with);
                    return this;
                }
                continue;
            } else {
                matchPos = 0;
            }
        }

        return this;
    }

    public FastStringBuffer deleteCharAt(int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        System.arraycopy(value, index + 1, value, index, count - index - 1);
        count--;
        return this;
    }

    public int indexOf(String s) {
        int thisLen = this.length();
        int sLen = s.length();

        for (int i = 0; i <= thisLen - sLen; i++) {
            int j = 0;
            while (j < sLen && this.value[i + j] == s.charAt(j)) {
                j += 1;
            }
            if (j == sLen) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(String s, int fromIndex) {
        int thisLen = this.length();
        int sLen = s.length();

        for (int i = fromIndex > 0 ? fromIndex : 0; i <= thisLen - sLen; i++) {
            int j = 0;
            while (j < sLen && this.value[i + j] == s.charAt(j)) {
                j += 1;
            }
            if (j == sLen) {
                return i;
            }
        }
        return -1;

    }

    public int indexOf(char c) {
        for (int i = 0; i < this.count; i++) {
            if (c == this.value[i]) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(char c, int fromOffset) {
        for (int i = fromOffset; i < this.count; i++) {
            if (c == this.value[i]) {
                return i;
            }
        }
        return -1;
    }

    public char firstChar() {
        return this.value[0];
    }

    public char lastChar() {
        return this.value[this.count - 1];
    }

    public final static class BackwardCharIterator implements Iterable<Character> {

        private int i;
        private FastStringBuffer fastStringBuffer;

        public BackwardCharIterator(FastStringBuffer fastStringBuffer) {
            this.fastStringBuffer = fastStringBuffer;
            i = fastStringBuffer.length();
        }

        public Iterator<Character> iterator() {
            return new Iterator<Character>() {

                public boolean hasNext() {
                    return i > 0;
                }

                public Character next() {
                    return fastStringBuffer.value[--i];
                }

                public void remove() {
                    throw new RuntimeException("Not implemented");
                }
            };
        }
    }

    public BackwardCharIterator reverseIterator() {
        return new BackwardCharIterator(this);
    }

    public void rightTrim() {
        char c;
        //while !isEmpty && lastChar == ' ' || \t.
        while (this.count > 0 && ((c = this.value[this.count - 1]) == ' ' || c == '\t')) {
            this.count--;
        }
    }

    public char deleteFirst() {
        char ret = this.value[0];
        this.deleteCharAt(0);
        return ret;
    }

    public FastStringBuffer appendN(final String val, int n) {
        final int strLen = val.length();
        int min = count + (n * strLen);
        if (min > value.length) {
            //was: resizeForMinimum(newCount);
            int newCapacity = (value.length + 1) * 2;
            if (min > newCapacity) {
                newCapacity = min;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }

        while (n-- > 0) {
            val.getChars(0, strLen, value, this.count);
            this.count += strLen;
        }
        return this;
    }

    public FastStringBuffer appendN(char val, int n) {
        if (count + n > value.length) {
            //was: resizeForMinimum(newCount);
            int minimumCapacity = count + n;
            int newCapacity = (value.length + 1) * 2;
            if (minimumCapacity > newCapacity) {
                newCapacity = minimumCapacity;
            }
            char newValue[] = new char[newCapacity];
            System.arraycopy(value, 0, newValue, 0, count);
            value = newValue;
        }

        while (n-- > 0) {
            value[count] = val;
            count++;
        }
        return this;
    }

    public boolean endsWith(String string) {
        return startsWith(string, count - string.length());
    }

    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    public boolean startsWith(char c) {
        if (this.count < 1) {
            return false;
        }
        return this.value[0] == c;
    }

    public boolean endsWith(char c) {
        if (this.count < 1) {
            return false;
        }
        return this.value[this.count - 1] == c;
    }

    public boolean startsWith(String prefix, int offset) {
        char ta[] = value;
        int to = offset;
        char pa[] = prefix.toCharArray();
        int po = 0;
        int pc = pa.length;
        // Note: toffset might be near -1>>>1.
        if ((offset < 0) || (offset > count - pc)) {
            return false;
        }
        while (--pc >= 0) {
            if (ta[to++] != pa[po++]) {
                return false;
            }
        }
        return true;
    }

    public void setCharAt(int i, char c) {
        this.value[i] = c;
    }

    /**
     * Careful: it doesn't check anything. Just sets the internal length.
     */
    public void setLength(int i) {
        this.count = i;
    }

    public byte[] getBytes() {
        return this.toString().getBytes();
    }

    public int countNewLines() {
        int lines = 0;

        for (int i = 0; i < count; i++) {
            char c = value[i];
            switch (c) {
                case '\n':
                    lines += 1;
                    break;

                case '\r':
                    lines += 1;
                    if (i < count - 1) {
                        if (value[i + 1] == '\n') {
                            i++; //skip the \n after the \r
                        }
                    }
                    break;
            }
        }
        return lines;
    }

    public FastStringBuffer insertN(int pos, char c, int repetitions) {
        FastStringBuffer other = new FastStringBuffer(repetitions);
        other.appendN(c, repetitions);
        insert(pos, other);
        return this;
    }

    public String getLastWord() {
        FastStringBuffer lastWordBuf = new FastStringBuffer(this.count);
        int i;
        //skip whitespaces in the end
        for (i = this.count - 1; i >= 0; i--) {
            if (!Character.isWhitespace(this.value[i])) {
                break;
            }
        }
        //actual word
        for (; i >= 0; i--) {
            if (Character.isWhitespace(this.value[i])) {
                break;
            }
            lastWordBuf.append(this.value[i]);
        }
        lastWordBuf.reverse();
        return lastWordBuf.toString();
    }

    public void removeWhitespaces() {
        int length = this.count;
        char[] newVal = new char[length];

        int j = 0;
        for (int i = 0; i < length; i++) {
            char ch = this.value[i];
            if (!Character.isWhitespace(ch)) {
                newVal[j] = ch;
                j++;
            }
        }
        this.count = j;
        this.value = newVal;
    }

    public FastStringBuffer removeChars(Set<Character> chars) {
        int length = this.count;
        char[] newVal = new char[length];

        int j = 0;
        for (int i = 0; i < length; i++) {
            char ch = this.value[i];
            if (!chars.contains(ch)) {
                newVal[j] = ch;
                j++;
            }
        }
        this.count = j;
        this.value = newVal;
        return this;
    }

    public char[] getInternalCharsArray() {
        return this.value;
    }

    /**
     * Provide a subsequence as a view of the buffer we're dealing with.
     *
     * @author Fabio
     */
    private static class BufCharSequence implements CharSequence {

        private char[] value;
        private int fStart;
        private int fEnd;

        public BufCharSequence(char[] value, int start, int end) {
            this.value = value;
            this.fStart = start;
            this.fEnd = end;
        }

        public int length() {
            return fEnd - fStart;
        }

        public char charAt(int index) {
            if (index < 0 || index >= fEnd - fStart) {
                throw new IndexOutOfBoundsException();
            }
            return value[fStart + index];
        }

        public CharSequence subSequence(int start, int end) {
            return new BufCharSequence(value, fStart + start, fStart + end);
        }

        @Override
        public String toString() {
            return new String(value, fStart, fEnd - fStart);
        }
    }

    public CharSequence subSequence(int start, int end) {
        return new BufCharSequence(this.value, start, end);
    }

}
