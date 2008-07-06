package org.python.pydev.core.structure;

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
 * @author Fabio
 */
public final class FastStringBuffer{

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

    /**
     * initializes from a string and the additional size for the buffer
     * 
     * @param s string with the initial contents
     * @param additionalSize the additional size for the buffer
     */
    public FastStringBuffer(String s, int additionalSize) {
        this.count = s.length();
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
            resizeForMinimum(newCount);
        }
        string.getChars(0, strLen, value, this.count);
        this.count = newCount;

        return this;
    }

    /**
     * Resizes the internal buffer to have at least the minimum capacity passed (but may be more)
     */
    private void resizeForMinimum(int minimumCapacity) {
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
    public final FastStringBuffer append(int n) {
        append(String.valueOf(n));
        return this;
    }

    /**
     * Appends a char to the buffer.
     */
    public final FastStringBuffer append(char n) {
        if (count + 1 > value.length) {
            resizeForMinimum(count + 1);
        }
        value[count] = n;
        count++;
        return this;
    }

    /**
     * Appends a long to the buffer.
     */
    public final FastStringBuffer append(long n) {
        append(String.valueOf(n));
        return this;
    }

    /**
     * Appends a boolean to the buffer.
     */
    public final FastStringBuffer append(boolean b) {
        append(String.valueOf(b));
        return this;
    }

    /**
     * Appends an array of chars to the buffer.
     */
    public FastStringBuffer append(char[] chars) {
        int newCount = count + chars.length;
        if (newCount > value.length) {
            resizeForMinimum(newCount);
        }
        System.arraycopy(chars, 0, value, count, chars.length);
        count = newCount;
        return this;
    }

    /**
     * Appends another buffer to this buffer.
     */
    public FastStringBuffer append(FastStringBuffer other) {
        append(other.value, 0, other.count);
        return this;
    }

    /**
     * Appends an array of chars to this buffer, starting at the offset passed with the length determined.
     */
    public FastStringBuffer append(char[] chars, int offset, int len) {
        int newCount = count + len;
        if (newCount > value.length) {
            resizeForMinimum(newCount);
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
            resizeForMinimum(newCount);
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(0, len, value, offset);
        count = newCount;
        return this;
    }

    /**
     * Inserts a char at a given position in the buffer.
     */
    public FastStringBuffer insert(int offset, char c) {
        int newCount = count + 1;
        if (newCount > value.length) {
            resizeForMinimum(newCount);
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
        return append(object != null ? object.toString() : "null");
    }

    /**
     * Sets the new size of this buffer (warning: use with care: no validation is done of the len passed)
     */
    public void setCount(int newLen) {
        this.count = newLen;
    }

    public FastStringBuffer delete(int start, int end) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            end = count;
        if (start > end)
            throw new StringIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            System.arraycopy(value, start + len, value, start, count - end);
            count -= len;
        }
        return this;
    }

    public FastStringBuffer replace(int start, int end, String str) {
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (start > count)
            throw new StringIndexOutOfBoundsException("start > length()");
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");
        if (end > count)
            end = count;

        if (end > count)
            end = count;
        int len = str.length();
        int newCount = count + len - (end - start);
        if (newCount > value.length) {
            resizeForMinimum(newCount);
        }

        System.arraycopy(value, end, value, start + len, count - end);
        str.getChars(0, len, value, start);
        count = newCount;
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

    public int indexOf(char c) {
        for(int i=0;i<this.count;i++){
            if(c == this.value[i]){
                return i;
            }
        }
        return -1;
    }

    public char firstChar() {
        return this.value[0];
    }
    
    public char lastChar() {
        return this.value[this.count-1];
    }


    
}
