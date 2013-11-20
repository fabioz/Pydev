/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_core.string;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.cache.Cache;
import org.python.pydev.shared_core.cache.LRUCache;

public class StringUtils {

    /**
     * @author fabioz
     *
     */
    private static final class IterLines implements Iterator<String> {
        private final String string;
        private final int len;
        private int i;
        private boolean calculatedNext;
        private boolean hasNext;
        private String next;

        private IterLines(String string) {
            this.string = string;
            this.len = string.length();
        }

        public boolean hasNext() {
            if (!calculatedNext) {
                calculatedNext = true;
                hasNext = calculateNext();
            }
            return hasNext;
        }

        private boolean calculateNext() {
            next = null;
            char c;
            int start = i;

            for (; i < len; i++) {
                c = string.charAt(i);

                if (c == '\r') {
                    if (i < len - 1 && string.charAt(i + 1) == '\n') {
                        i++;
                    }
                    i++;
                    next = string.substring(start, i);
                    return true;

                } else if (c == '\n') {
                    i++;
                    next = string.substring(start, i);
                    return true;
                }
            }
            if (start != i) {
                next = string.substring(start, i);
                i++;
                return true;
            }
            return false;
        }

        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String n = next;
            calculatedNext = false;
            next = null;
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Splits the given string in a list where each element is a line.
     *
     * @param string string to be split.
     * @return list of strings where each string is a line.
     *
     * @note the new line characters are also added to the returned string.
     *
     * IMPORTANT: The line returned will be a substring of the initial line, so, it's recommended that a copy
     * is created if it should be kept in memory (otherwise the full initial string will also be kept in memory).
     */
    public static Iterable<String> iterLines(final String string) {
        return new Iterable<String>() {

            public Iterator<String> iterator() {
                return new IterLines(string);
            }
        };

    }

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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String join(String delimiter, Collection splitted) {
        int size = splitted.size();
        if (size == 0) {
            return "";
        }
        Object[] arr = new Object[size];
        return join(delimiter, splitted.toArray(arr));
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
        int zeroAsInt = '0';

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
        int zeroAsInt = '0';

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

    /**
     * Splits the passed string based on the toSplit string.
     */
    public static List<String> split(final String string, final char toSplit, int maxPartsToSplit) {
        Assert.isTrue(maxPartsToSplit > 0);
        int len = string.length();
        if (len == 0) {
            return new ArrayList<String>(0);
        }

        ArrayList<String> ret = new ArrayList<String>();

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == toSplit) {
                if (last != i) {
                    if (ret.size() == maxPartsToSplit - 1) {
                        ret.add(string.substring(last, len));
                        return ret;
                    } else {
                        ret.add(string.substring(last, i));
                    }
                }
                while (c == toSplit && i < len - 1) {
                    i++;
                    c = string.charAt(i);
                }
                last = i;
            }
        }
        if (c != toSplit) {
            if (last == 0 && len > 0) {
                ret.add(string); //it is equal to the original (no char to split)

            } else if (last < len) {
                ret.add(string.substring(last, len));

            }
        }
        return ret;
    }

    /**
     * Splits the passed string based on the toSplit string.
     *
     * Corner-cases:
     * if the delimiter to do the split is empty an error is raised.
     * if the entry is an empty string, the return should be an empty array.
     */
    public static List<String> split(final String string, final String toSplit) {
        int len = string.length();
        if (len == 0) {
            return new ArrayList<String>(0);
        }

        int length = toSplit.length();

        if (length == 1) {
            return split(string, toSplit.charAt(0));
        }
        ArrayList<String> ret = new ArrayList<String>();
        if (length == 0) {
            ret.add(string);
            return ret;
        }

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == toSplit.charAt(0) && matches(string, toSplit, i)) {
                if (last != i) {
                    ret.add(string.substring(last, i));
                }
                last = i + toSplit.length();
                i += toSplit.length() - 1;
            }
        }

        if (last < len) {
            ret.add(string.substring(last, len));
        }

        return ret;
    }

    private static boolean matches(final String string, final String toSplit, int i) {
        int length = string.length();
        int toSplitLen = toSplit.length();
        if (length - i >= toSplitLen) {
            for (int j = 0; j < toSplitLen; j++) {
                if (string.charAt(i + j) != toSplit.charAt(j)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Splits some string given some char (that char will not appear in the returned strings)
     * Empty strings are also never added.
     */
    public static List<String> split(String string, char toSplit) {
        int len = string.length();
        if (len == 0) {
            return new ArrayList<String>(0);
        }
        ArrayList<String> ret = new ArrayList<String>();

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == toSplit) {
                if (last != i) {
                    ret.add(string.substring(last, i));
                }
                while (c == toSplit && i < len - 1) {
                    i++;
                    c = string.charAt(i);
                }
                last = i;
            }
        }
        if (c != toSplit) {
            if (last == 0 && len > 0) {
                ret.add(string); //it is equal to the original (no char to split)

            } else if (last < len) {
                ret.add(string.substring(last, len));

            }
        }
        return ret;
    }

    /**
     * Splits the given string in a list where each element is a line.
     *
     * @param string string to be split.
     * @return list of strings where each string is a line.
     *
     * @note the new line characters are also added to the returned string.
     */
    public static List<String> splitInWhiteSpaces(String string) {
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (Character.isWhitespace(c)) {
                if (last != i) {
                    ret.add(string.substring(last, i));
                }
                while (Character.isWhitespace(c) && i < len - 1) {
                    i++;
                    c = string.charAt(i);
                }
                last = i;
            }
        }
        if (!Character.isWhitespace(c)) {
            if (last == 0 && len > 0) {
                ret.add(string); //it is equal to the original (no char to split)

            } else if (last < len) {
                ret.add(string.substring(last, len));

            }
        }
        return ret;
    }

    /**
     * Splits the given string in a list where each element is a line.
     *
     * @param string string to be split.
     * @return list of strings where each string is a line.
     *
     * @note the new line characters are also added to the returned string.
     */
    public static List<String> splitInLines(String string) {
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

        char c;
        FastStringBuffer buf = new FastStringBuffer();

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            buf.append(c);

            if (c == '\r') {
                if (i < len - 1 && string.charAt(i + 1) == '\n') {
                    i++;
                    buf.append('\n');
                }
                ret.add(buf.toString());
                buf.clear();
            }
            if (c == '\n') {
                ret.add(buf.toString());
                buf.clear();

            }
        }
        if (buf.length() != 0) {
            ret.add(buf.toString());
        }
        return ret;
    }

    /**
     * Splits the given string in a list where each element is a line.
     *
     * @param string string to be split.
     * @param addNewLines defines if new lines should be added to the returned strings.
     * @return list of strings where each string is a line.
     *
     */
    public static List<String> splitInLines(String string, boolean addNewLines) {
        if (addNewLines) {
            return splitInLines(string);
        }
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

        char c;
        FastStringBuffer buf = new FastStringBuffer();

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            buf.append(c);

            if (c == '\r') {
                buf.deleteLast();
                if (i < len - 1 && string.charAt(i + 1) == '\n') {
                    i++;
                }
                ret.add(buf.toString());
                buf.clear();
            }
            if (c == '\n') {
                buf.deleteLast();
                ret.add(buf.toString());
                buf.clear();

            }
        }
        if (buf.length() != 0) {
            ret.add(buf.toString());
        }
        return ret;
    }

    /**
     * This is usually what's on disk
     */
    public static String BOM_UTF8 = new String(new char[] { 0xEF, 0xBB, 0xBF });
    /**
     * When we convert a string from the disk to a java string, if it had an UTF-8 BOM, it'll have that BOM converted
     * to this BOM. See: org.python.pydev.parser.PyParser27Test.testBom()
     */
    public static String BOM_UNICODE = new String(new char[] { 0xFEFF });

    public static String removeBom(String contents) {
        if (contents.startsWith(BOM_UTF8)) {
            contents = contents.substring(BOM_UTF8.length());
        }
        return contents;
    }

    /**
     * Small cache to hold strings only with spaces (so that each width has a created string).
     */
    private static Cache<Integer, String> widthToSpaceString = new LRUCache<Integer, String>(8);

    /**
     * Creates a string of spaces of the designated length.
     * @param width number of spaces you want to create a string of
     * @return the created string
     */
    public static String createSpaceString(int width) {
        String existing = StringUtils.widthToSpaceString.getObj(width);
        if (existing != null) {
            return existing;
        }
        FastStringBuffer buf = new FastStringBuffer(width);
        buf.appendN(' ', width);
        String newStr = buf.toString();
        StringUtils.widthToSpaceString.add(width, newStr);
        return newStr;
    }

    public static String getWithClosedPeer(char c) {
        switch (c) {
            case '{':
                return "{}";
            case '(':
                return "()";
            case '[':
                return "[]";
            case '<':
                return "<>";
            case '\'':
                return "''";
            case '"':
                return "\"\"";
        }

        throw new NoPeerAvailableException("Unable to find peer for :" + c);
    }

    public static boolean isOpeningPeer(char lastChar) {
        return lastChar == '(' || lastChar == '[' || lastChar == '{' || lastChar == '<';
    }

    public static boolean isClosingPeer(char lastChar) {
        return lastChar == ')' || lastChar == ']' || lastChar == '}' || lastChar == '>';
    }

    public static char getPeer(char c) {
        switch (c) {
            case '{':
                return '}';
            case '}':
                return '{';
            case '(':
                return ')';
            case ')':
                return '(';
            case '[':
                return ']';
            case ']':
                return '[';
            case '>':
                return '<';
            case '<':
                return '>';
            case '\'':
                return '\'';
            case '\"':
                return '\"';
            case '/':
                return '/';
            case '`':
                return '`';
        }

        throw new NoPeerAvailableException("Unable to find peer for :" + c);
    }

    /**
     * Counts the number of occurences of a certain character in a string.
     *
     * @param line the string to search in
     * @param c the character to search for
     * @return an integer (int) representing the number of occurences of this character
     */
    public static int countChars(char c, StringBuffer line) {
        int ret = 0;
        int len = line.length();
        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == c) {
                ret += 1;
            }
        }
        return ret;
    }

    /**
     * Counts the number of occurences of a certain character in a string.
     *
     * @param line the string to search in
     * @param c the character to search for
     * @return an integer (int) representing the number of occurences of this character
     */
    public static int countChars(char c, FastStringBuffer line) {
        int ret = 0;
        int len = line.length();
        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == c) {
                ret += 1;
            }
        }
        return ret;
    }

    /**
     * Counts the number of occurences of a certain character in a string.
     *
     * @param line the string to search in
     * @param c the character to search for
     * @return an integer (int) representing the number of occurences of this character
     */
    public static int countChars(char c, String line) {
        int ret = 0;
        int len = line.length();
        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == c) {
                ret += 1;
            }
        }
        return ret;
    }

    private static final Pattern compiled = Pattern.compile("\\r?\\n|\\r");

    public static String replaceNewLines(String text, String repl) {
        return compiled.matcher(text).replaceAll(repl);
    }

    public static String replaceAll(String string, String replace, String with) {
        FastStringBuffer ret = new FastStringBuffer(string, 16);
        return ret.replaceAll(replace, with).toString();
    }

    public static String shorten(String nameForUI, int maxLen) {
        if (nameForUI.length() >= maxLen) {
            maxLen -= 5;
            int first = maxLen / 2;
            int last = maxLen / 2 + (maxLen % 2);

            return nameForUI.substring(0, first) + " ... "
                    + nameForUI.substring(nameForUI.length() - last, nameForUI.length());
        }
        return nameForUI;
    }

    /**
     * Removes whitespaces and tabs at the end of the string.
     */
    public static String rightTrim(final String input) {
        int len = input.length();
        int st = 0;
        int off = 0;

        while ((st < len) && (input.charAt(off + len - 1) <= ' ')) {
            len--;
        }
        return input.substring(0, len);
    }

    /**
     * Removes whitespaces and tabs at the beginning of the string.
     */
    public static String leftTrim(String input) {
        int len = input.length();
        int off = 0;

        while ((off < len) && (input.charAt(off) <= ' ')) {
            off++;
        }
        return input.substring(off, len);
    }

    /**
     * Find the nth index of character in string
     * @param string to search
     * @param character to search for
     * @param nth count
     * @return count <= 0 returns -1. count > number of occurances of character returns -1.
     * Otherwise return index of nth occurence of character
     */
    public static int nthIndexOf(final String string, final char character, int nth) {
        if (nth <= 0) {
            return -1;
        }
        int pos = string.indexOf(character);
        while (--nth > 0 && pos != -1) {
            pos = string.indexOf(character, pos + 1);
        }
        return pos;
    }

    public static int count(String name, char c) {
        int count = 0;
        final int len = name.length();
        for (int i = 0; i < len; i++) {
            if (name.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private static Charset latin1Charset;

    private static Charset getLatin1Charset() {
        if (latin1Charset == null) {
            latin1Charset = Charset.forName("iso8859-1");
        }
        return latin1Charset;
    }

    /**
     * Returns whether the given input (to the number of bytes passed in len) is to be considered a valid text string
     * (otherwise, it's considered a binary string).
     *
     * If no bytes are available, it's considered valid.
     */
    public static boolean isValidTextString(byte[] buffer, int len) {
        if (len <= 0) {
            return true;
        }
        if (len > buffer.length) {
            len = buffer.length;
        }
        String s = new String(buffer, 0, len, getLatin1Charset()); //Decode as latin1
        int maxLen = s.length();
        for (int i = 0; i < maxLen; i++) {
            char c = s.charAt(i);

            //based on http://casa.colorado.edu/~ajsh/iso8859-1.html
            //and http://www.ic.unicamp.br/~stolfi/EXPORT/www/ISO-8859-1-Encoding.html
            //9 - 15: \t\r\n and other feeds
            //32 - 127: standard
            //128 - 159: ok (but windows only)
            //160 - 255: ok

            if (c >= 32 && c <= 255 || c >= 9 && c <= 15) {
                //Ok, in valid range.
            } else {
                return false;
            }
        }

        return true;
    }
}
