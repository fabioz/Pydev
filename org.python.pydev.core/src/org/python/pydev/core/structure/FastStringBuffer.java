package org.python.pydev.core.structure;

/**
 * This is a custom string that works around char[] objects to provide minimum allocation/garbage collection overhead.
 * To be used mostly when several small concatenations of strings are used and in local contexts while reusing the 
 * same object to create multiple strings.
 *
 * @author Fabio
 */
public final class FastStringBuffer {

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
     * Appends a string to the buffer 
     */
    public FastStringBuffer append(String string) {
        int strLen = string.length();

        if (this.count + strLen > this.value.length) {
            resizeForMinimum(this.count + strLen);
        }
        string.getChars(0, strLen, value, this.count);
        this.count += strLen;

        return this;
    }

    private void resizeForMinimum(int minimumCapacity) {
        int newCapacity = (value.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }
        char newValue[] = new char[newCapacity];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
    }

    public final FastStringBuffer append(int n) {
        append(String.valueOf(n));
        return this;
    }
    
    public final FastStringBuffer append(char n) {
        if (count + 1 > value.length) {
            resizeForMinimum(count + 1);
        }
        value[count] = n;
        count += 1;
        return this;
    }

    public final FastStringBuffer append(long n) {
        append(String.valueOf(n));
        return this;
    }

    public final FastStringBuffer append(boolean b) {
        append(String.valueOf(b));
        return this;
    }

    public FastStringBuffer append(char[] chars) {
        if (count + chars.length > value.length) {
            resizeForMinimum(count + chars.length);
        }
        System.arraycopy(chars, 0, value, count, chars.length);
        count += chars.length;
        return this;
    }

    public FastStringBuffer append(FastStringBuffer other) {
        append(other.value, 0, other.count);
        return this;
    }

    public FastStringBuffer append(char[] chars, int offset, int len) {
        if (count + len > value.length) {
            resizeForMinimum(count + len);
        }
        System.arraycopy(chars, offset, value, count, len);
        count += len;
        return this;
    }

    public FastStringBuffer reverse() {
        final int limit = count / 2;
        for (int i = 0; i < limit; ++i) {
            char c = value[i];
            value[i] = value[count - i - 1];
            value[count - i - 1] = c;
        }
        return this;
    }

    public void clear() {
        this.count = 0;
    }

    public int length() {
        return this.count;
    }

    @Override
    public String toString() {
        return new String(value, 0, count);
    }

    public void deleteLast() {
        if (this.count > 0) {
            this.count -= 1;
        }
    }

    public char charAt(int i) {
        return this.value[i];
    }

    public FastStringBuffer insert(int offset, String str) {
        int len = str.length();
        int newCount = count + len;
        if (newCount > value.length){
            resizeForMinimum(newCount);
        }
        System.arraycopy(value, offset, value, offset + len, count - offset);
        str.getChars(0, str.length(), value, offset);
        count = newCount;
        return this;
    }

    public FastStringBuffer appendObject(Object attribute) {
        return append(attribute != null?attribute.toString():"null");
    }

    public void setCount(int newLen) {
        this.count = newLen;
    }

}
