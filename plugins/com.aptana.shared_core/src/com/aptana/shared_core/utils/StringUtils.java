package com.aptana.shared_core.utils;

import java.util.List;

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

}
