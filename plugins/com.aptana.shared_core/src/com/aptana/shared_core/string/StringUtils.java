package com.aptana.shared_core.string;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

import com.aptana.shared_core.cache.LRUCache;

public class StringUtils {

    /**
     * Same as Python join: Go through all the paths in the string and join them with the passed delimiter.
     * 
     * Note: optimized to have less allocations/method calls 
     * (i.e.: not using FastStringBuffer, pre-allocating proper size and doing string.getChars directly).
     * 
     * Having a return type != from String (i.e.: char[].class or FastStringBuffer.class) is a bit faster
     * as it won't do an additional array/copy for the final result.
     */
    public static Object join(String delimiter, String[] splitted, Class<? extends Object> returnType) {
        //A bit faster than if..elif?
        final int len = splitted.length;
        switch (len) {
            case 0:
                return "";
            case 1:
                return splitted[0];
        }

        final int delimiterLen = delimiter.length();
        int totalSize = delimiterLen * (len - 1);
        for (int i = 0; i < len; i++) {
            totalSize += splitted[i].length();
        }

        final char[] buf = new char[totalSize];
        int count = 0;

        //Copy the first item
        String string = splitted[0];
        int strLen = string.length();
        string.getChars(0, strLen, buf, count);
        count += strLen;

        switch (delimiterLen) {
            case 0:
                //Special case when the delimiter is empty (i.e.: doesn't need to be copied).
                for (int i = 1; i < len; i++) {
                    string = splitted[i];
                    strLen = string.length();
                    string.getChars(0, strLen, buf, count);
                    count += strLen;
                }
                break;

            case 1:
                //Special case with single-char delimiter (as it's pretty common)
                final char delimiterChar = delimiter.charAt(0);
                for (int i = 1; i < len; i++) {
                    buf[count] = delimiterChar;
                    count++;

                    string = splitted[i];
                    strLen = string.length();
                    string.getChars(0, strLen, buf, count);
                    count += strLen;
                }
                break;

            case 2:
                //Special case with double-char delimiter (usually: \r\n)
                final char delimiterChar0 = delimiter.charAt(0);
                final char delimiterChar1 = delimiter.charAt(1);
                for (int i = 1; i < len; i++) {
                    buf[count] = delimiterChar0;
                    buf[count + 1] = delimiterChar1;
                    count += 2;

                    string = splitted[i];
                    strLen = string.length();
                    string.getChars(0, strLen, buf, count);
                    count += strLen;
                }
                break;

            default:
                //Copy the remaining ones with the delimiter in place.
                for (int i = 1; i < len; i++) {
                    strLen = delimiterLen;
                    delimiter.getChars(0, strLen, buf, count);
                    count += strLen;

                    string = splitted[i];
                    strLen = string.length();
                    string.getChars(0, strLen, buf, count);
                    count += strLen;
                }
                break;

        }

        if (returnType == null || returnType == String.class) {
            return new String(buf);

        } else if (returnType == FastStringBuffer.class) {
            return new FastStringBuffer(buf);

        } else if (returnType == char[].class) {
            return buf;

        } else {
            throw new RuntimeException("Don't know how to handle return type: " + returnType);
        }

    }

    /**
     * Same as Python join: Go through all the paths in the string and join them with the passed delimiter,
     * but start at the passed initial location in the splitted array.
     */
    public static String join(String delimiter, String[] splitted, int startAtSegment, int endAtSegment) {
        String[] s = new String[endAtSegment - startAtSegment];
        for (int i = startAtSegment, j = 0; i < splitted.length && i < endAtSegment; i++, j++) {
            s[j] = splitted[i];
        }
        return StringUtils.join(delimiter, s);
    }

    /**
     * Same as Python join: Go through all the paths in the string and join them with the passed delimiter.
     */
    public static String join(String delimiter, List<String> splitted) {
        return (String) join(delimiter, splitted.toArray(new String[splitted.size()]), null);
    }

    public static String join(String delimiter, String[] splitted) {
        return (String) join(delimiter, splitted, null);
    }

    public static String join(String delimiter, Object... splitted) {
        String[] newSplitted = new String[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            Object s = splitted[i];
            if (s == null) {
                newSplitted[i] = "null";
            } else {
                newSplitted[i] = s.toString();
            }
        }
        return join(delimiter, newSplitted);
    }

    /**
     * Formats a string, replacing %s with the arguments passed.
     * 
     * %% is also changed to %.
     * 
     * If % is followed by any other char, the % and the next char are ignored. 
     * 
     * @param str string to be formatted
     * @param args arguments passed
     * @return a string with the %s replaced by the arguments passed
     */
    public static String format(final String str, Object... args) {
        final int length = str.length();
        FastStringBuffer buffer = new FastStringBuffer(length + (16 * args.length));
        int j = 0;
        int i = 0;

        int start = 0;

        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c == '%') {
                if (i + 1 < length) {
                    if (i > start) {
                        buffer.append(str.substring(start, i));
                    }
                    char nextC = str.charAt(i + 1);

                    switch (nextC) {
                        case 's':
                            buffer.appendObject(args[j]);
                            j++;
                            break;
                        case '%':
                            buffer.append('%');
                            j++;
                            break;
                    }
                    i++;
                    start = i + 1;
                }
            }
        }

        if (i > start) {
            buffer.append(str.substring(start, i));
        }
        return buffer.toString();
    }

    /**
     * A faster alternative for parsing positive longs (without exponential notation and only on decimal notation).
     * Attempting to parse an longs that's negative or has exponential notation will throw a NumberFormatException.
     * 
     * Note that it doesn't check for longs overflow (so, values higher than MAX_LONG will overflow silently).
     */
    public static long parsePositiveLong(FastStringBuffer buf) {
        char[] array = buf.getInternalCharsArray();
        int len = buf.length();
        if (len == 0) {
            throw new NumberFormatException("Empty string received");
        }

        long result = 0;
        int zeroAsInt = (int) '0';

        for (int i = 0; i < len; i++) {
            result *= 10;
            int c = array[i] - zeroAsInt;
            if (c < 0 || c > 9) {
                throw new NumberFormatException("Error getting positive int from: " + buf);
            }
            result += c;

        }
        return result;
    }

    /**
     * A faster alternative for parsing positive ints (without exponential notation and only on decimal notation).
     * Attempting to parse an ints that's negative or has exponential notation will throw a NumberFormatException.
     * 
     * Note that it doesn't check for ints overflow (so, values higher than MAX_INT will overflow silently).
     */
    public static int parsePositiveInt(FastStringBuffer buf) {
        char[] array = buf.getInternalCharsArray();
        int len = buf.length();
        if (len == 0) {
            throw new NumberFormatException("Empty string received");
        }

        int result = 0;
        int zeroAsInt = (int) '0';

        for (int i = 0; i < len; i++) {
            result *= 10;
            int c = array[i] - zeroAsInt;
            if (c < 0 || c > 9) {
                throw new NumberFormatException("Error getting positive int from: " + buf);
            }
            result += c;

        }
        return result;
    }

    /**
     * @return the number of line breaks in the passed string.
     */
    public static int countLineBreaks(final String replacementString) {
        int lineBreaks = 0;
        int ignoreNextNAt = -1;

        //we may have line breaks with \r\n, or only \n or \r
        final int len = replacementString.length();
        for (int i = 0; i < len; i++) {
            char c = replacementString.charAt(i);
            if (c == '\r') {
                lineBreaks++;
                ignoreNextNAt = i + 1;

            } else if (c == '\n') {
                if (ignoreNextNAt != i) {
                    lineBreaks++;
                }
            }
        }
        return lineBreaks;
    }

    private static final Object md5CacheLock = new Object();
    private static final LRUCache<String, String> md5Cache = new LRUCache<String, String>(1000);

    public static String md5(String str) {
        synchronized (StringUtils.md5CacheLock) {
            String obj = md5Cache.getObj(str);
            if (obj != null) {
                return obj;
            }
            try {
                byte[] bytes = str.getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                //MAX_RADIX because we'll generate the shorted string possible... (while still
                //using only numbers 0-9 and letters a-z)
                String ret = new BigInteger(1, md.digest(bytes)).toString(Character.MAX_RADIX).toLowerCase();
                md5Cache.add(str, ret);
                return ret;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
