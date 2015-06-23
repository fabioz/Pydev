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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.cache.Cache;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;

public final class StringUtils {

    public static final String EMPTY = "";

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
                //MAX_RADIX because we'll generate the shortest string possible... (while still
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
            return new ArrayList<>(0);
        }

        ArrayList<String> ret = new ArrayList<String>(maxPartsToSplit);

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
            return new ArrayList<>(0);
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
            return new ArrayList<>(0);
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

    /**
     * <p>Find the last position of a character which matches a given regex.</p>
     *
     * <p>This method is similar to {@link java.lang.String#lastIndexOf(String)}
     * except it allows for comparing characters akin to <i>wildcard</i> searches, i.e.
     * find the position of the last character classified as alphanumeric, without
     * the need to implement dozens of method variations where each method takes the
     * same parameters but does a slightly different search.</p>
     *
     * @param string - the string to search through, e.g. the <i>haystack</i>
     * @param regex -  a string containing a compilable {@link java.util.regex.Pattern}.
     * @return the last position of the character that matches the pattern<br>
     *         or <tt>-1</tt> if no match or some of the parameters are invalid.
     * @note the string is iterated over one char at a time, so the pattern will be
     * compared at most to one character strings.
     */
    public static int lastIndexOf(final String string, final String regex) {

        int index = -1;

        if (null == string || null == regex || string.length() == 0 || regex.length() == 0) {
            return index;
        }

        Pattern pat;
        try {
            pat = Pattern.compile(regex);
        } catch (PatternSyntaxException pse) {
            return index;
        }

        int len = string.length();
        int i = len - 1;
        char c = '\0';
        Matcher mat = null;

        while (i >= 0) {
            c = string.charAt(i);
            mat = pat.matcher(String.valueOf(c));
            if (mat.matches()) {
                index = i;
                break;
            }
            i--;
        }
        return index;
    }

    /**
     * <p>Join the elements of an <tt>Iterable</tt> by using <tt>delimiter</tt>
     * as separator.</p>
     *
     * @see http://snippets.dzone.com/posts/show/91
     *
     * @param objs - a collection which implements {@link java.lang.Iterable}
     * @param <T> - type in collection
     * @param delimiter - string used as separator
     *
     * @throws IllegalArgumentException if <tt>objs</tt> or <tt>delimiter</tt>
     *         is <tt>null</tt>.
     *
     * @return joined string
     */
    public static <T> String joinIterable(final String delimiter, final Iterable<T> objs)
            throws IllegalArgumentException {
        if (null == objs) {
            throw new IllegalArgumentException("objs can't be null!");
        }
        if (null == delimiter) {
            throw new IllegalArgumentException("delimiter can't be null");
        }

        Iterator<T> iter = objs.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        String nxt = String.valueOf(iter.next());
        FastStringBuffer buffer = new FastStringBuffer(String.valueOf(nxt), nxt.length());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(String.valueOf(iter.next()));
        }

        return buffer.toString();
    }

    /**
     * <p>Repeat a substring (a.k.a. <i>substring multiplication</i>).</p>
     *
     * <p>Invalid Argument Values</p>
     *
     * <ul>return an empty string if <tt>str</tt> is empty, or if
     * <tt>times &lt;= 0</tt></ul>
     * <ul>if <tt>str</tt> is <tt>null</tt>, the string <tt>"null"</tt>
     * will be repeated.</ul>
     *
     * @param str - the substring to repeat<br>
     * @param times - how many copies
     * @return the repeated string
     */
    public static String repeatString(final String str, int times) {

        String s = String.valueOf(str);
        if (s.length() == 0 || times <= 0) {
            return "";
        }

        FastStringBuffer buffer = new FastStringBuffer();
        buffer.appendN(s, times);
        return buffer.toString();
    }

    /**
     * Counts the number of %s in the string
     *
     * @param str the string to be analyzed
     * @return the number of %s in the string
     */
    public static int countPercS(final String str) {
        int j = 0;

        final int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '%' && i + 1 < len) {
                char nextC = str.charAt(i + 1);
                if (nextC == 's') {
                    j++;
                    i++;
                }
            }
        }
        return j;
    }

    /**
     * Given a string remove all from the rightmost '.' onwards.
     *
     * E.g.: bbb.t would return bbb
     *
     * If it has no '.', returns the original string unchanged.
     */
    public static String stripExtension(String input) {
        return stripFromRigthCharOnwards(input, '.');
    }

    public static String getFileExtension(String name) {
        int i = name.lastIndexOf('.');
        if (i == -1) {
            return null;
        }
        if (name.length() - 1 == i) {
            return "";
        }
        return name.substring(i + 1);
    }

    public static int rFind(String input, char ch) {
        int len = input.length();
        int st = 0;
        int off = 0;

        while ((st < len) && (input.charAt(off + len - 1) != ch)) {
            len--;
        }
        len--;
        return len;
    }

    private static String stripFromRigthCharOnwards(String input, char ch) {
        int len = rFind(input, ch);
        if (len == -1) {
            return input;
        }
        return input.substring(0, len);
    }

    public static String stripFromLastSlash(String input) {
        return stripFromRigthCharOnwards(input, '/');
    }

    /**
     * Removes the occurrences of the passed char in the end of the string.
     */
    public static String rightTrim(String input, char charToTrim) {
        int len = input.length();
        int st = 0;
        int off = 0;

        while ((st < len) && (input.charAt(off + len - 1) == charToTrim)) {
            len--;
        }
        return input.substring(0, len);
    }

    /**
     * Removes the occurrences of the passed char in the end of the string.
     */
    public static String rightTrimNewLineChars(String input) {
        int len = input.length();
        int st = 0;
        int off = 0;
        char c;
        while ((st < len) && ((c = input.charAt(off + len - 1)) == '\r' || c == '\n')) {
            len--;
        }
        return input.substring(0, len);
    }

    /**
     * Removes the occurrences of the passed char in the start and end of the string.
     */
    public static String leftAndRightTrim(String input, char charToTrim) {
        return rightTrim(leftTrim(input, charToTrim), charToTrim);
    }

    /**
     * Removes the occurrences of the passed char in the end of the string.
     */
    public static String leftTrim(String input, char charToTrim) {
        int len = input.length();
        int off = 0;

        while ((off < len) && (input.charAt(off) == charToTrim)) {
            off++;
        }
        return input.substring(off, len);
    }

    /**
     * Changes all backward slashes (\) for forward slashes (/)
     *
     * @return the replaced string
     */
    public static String replaceAllSlashes(String string) {
        int len = string.length();
        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            if (c == '\\') { // only do some processing if there is a
                             // backward slash
                char[] ds = string.toCharArray();
                ds[i] = '/';
                for (int j = i; j < len; j++) {
                    if (ds[j] == '\\') {
                        ds[j] = '/';
                    }
                }
                return new String(ds);
            }

        }
        return string;
    }

    /**
     * Given some html, extracts its text.
     */
    public static String extractTextFromHTML(String html) {
        try {
            EditorKit kit = new HTMLEditorKit();
            Document doc = kit.createDefaultDocument();

            // The Document class does not yet handle charset's properly.
            doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);

            // Create a reader on the HTML content.
            Reader rd = new StringReader(html);

            // Parse the HTML.
            kit.read(rd, doc, 0);

            //  The HTML text is now stored in the document
            return doc.getText(0, doc.getLength());
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * Helper to process parts of a string.
     */
    public static interface ICallbackOnSplit {

        /**
         * @param substring the part found
         * @return false to stop processing the string (and true to check the next part).
         */
        boolean call(String substring);

    }

    /**
     * Splits some string given some char (that char will not appear in the returned strings)
     * Empty strings are also never added.
     *
     * @return true if the onSplit callback only returned true (and false if it stopped before).
     * @note: empty strings may be yielded.
     */
    public static boolean split(String string, char toSplit, ICallbackOnSplit onSplit) {
        int len = string.length();
        int last = 0;
        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == toSplit) {
                if (last != i) {
                    if (!onSplit.call(string.substring(last, i))) {
                        return false;
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
                if (!onSplit.call(string)) { //it is equal to the original (no char to split)
                    return false;
                }

            } else if (last < len) {
                if (!onSplit.call(string.substring(last, len))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Splits some string given many chars
     */
    public static List<String> split(String string, char... toSplit) {
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            if (contains(c, toSplit)) {
                if (last != i) {
                    ret.add(string.substring(last, i));
                }
                while (contains(c, toSplit) && i < len - 1) {
                    i++;
                    c = string.charAt(i);
                }
                last = i;
            }
        }
        if (!contains(c, toSplit)) {
            if (last == 0 && len > 0) {
                ret.add(string); //it is equal to the original (no dots)

            } else if (last < len) {
                ret.add(string.substring(last, len));

            }
        }
        return ret;
    }

    private static boolean contains(char c, char[] toSplit) {
        for (char ch : toSplit) {
            if (c == ch) {
                return true;
            }
        }
        return false;
    }

    public static List<String> splitAndRemoveEmptyNotTrimmed(String string, char c) {
        List<String> split = split(string, c);
        for (int i = split.size() - 1; i >= 0; i--) {
            if (split.get(i).length() == 0) {
                split.remove(i);
            }
        }
        return split;
    }

    public static List<String> splitAndRemoveEmptyTrimmed(String string, char c) {
        List<String> split = split(string, c);
        for (int i = split.size() - 1; i >= 0; i--) {
            if (split.get(i).trim().length() == 0) {
                split.remove(i);
            }
        }
        return split;
    }

    /**
     * Splits some string given some char in 2 parts. If the separator is not found,
     * everything is put in the 1st part.
     */
    public static Tuple<String, String> splitOnFirst(String fullRep, char toSplit) {
        int i = fullRep.indexOf(toSplit);
        if (i != -1) {
            return new Tuple<String, String>(fullRep.substring(0, i), fullRep.substring(i + 1));
        } else {
            return new Tuple<String, String>(fullRep, "");
        }
    }

    /**
     * Splits some string given some char in 2 parts. If the separator is not found,
     * everything is put in the 1st part.
     */
    public static Tuple<String, String> splitOnFirst(String fullRep, String toSplit) {
        int i = fullRep.indexOf(toSplit);
        if (i != -1) {
            return new Tuple<String, String>(fullRep.substring(0, i), fullRep.substring(i + toSplit.length()));
        } else {
            return new Tuple<String, String>(fullRep, "");
        }
    }

    /**
     * Splits the string as would string.split("\\."), but without yielding empty strings
     */
    public static List<String> dotSplit(String string) {
        return splitAndRemoveEmptyTrimmed(string, '.');
    }

    /**
     * Adds a char to an array of chars and returns the new array.
     *
     * @param c The chars to where the new char should be appended
     * @param toAdd the char to be added
     * @return a new array with the passed char appended.
     */
    public static char[] addChar(char[] c, char toAdd) {
        char[] c1 = new char[c.length + 1];

        System.arraycopy(c, 0, c1, 0, c.length);
        c1[c.length] = toAdd;
        return c1;

    }

    public static String[] addString(String[] c, String toAdd) {
        String[] c1 = new String[c.length + 1];

        System.arraycopy(c, 0, c1, 0, c.length);
        c1[c.length] = toAdd;
        return c1;
    }

    public static String removeNewLineChars(String message) {
        return message.replaceAll("\r", "").replaceAll("\n", "");
    }

    private static final int STATE_LOWER = 0;
    private static final int STATE_UPPER = 1;
    private static final int STATE_NUMBER = 2;

    public static String asStyleLowercaseUnderscores(String string) {
        int len = string.length();
        FastStringBuffer buf = new FastStringBuffer(len * 2);

        int lastState = 0;
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                if (lastState != STATE_UPPER) {
                    if (buf.length() > 0 && buf.lastChar() != '_') {
                        buf.append('_');
                    }
                }
                buf.append(Character.toLowerCase(c));
                lastState = STATE_UPPER;

            } else if (Character.isDigit(c)) {
                if (lastState != STATE_NUMBER) {
                    if (buf.length() > 0 && buf.lastChar() != '_') {
                        buf.append('_');
                    }
                }

                buf.append(c);
                lastState = STATE_NUMBER;
            } else {
                buf.append(c);
                lastState = STATE_LOWER;
            }
        }
        return buf.toString();
    }

    public static boolean isAllUpper(String string) {
        int len = string.length();
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (Character.isLetter(c) && !Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * How come that the Character class doesn't have this?
     */
    public static boolean isAsciiLetter(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /**
     * How come that the Character class doesn't have this?
     */
    public static boolean isAsciiLetterOrUnderline(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    public static boolean isAsciiLetterOrUnderlineOrNumber(int c) {
        return isAsciiLetterOrUnderline(c) || Character.isDigit(c);
    }

    public static String asStyleCamelCaseFirstLower(String string) {
        if (isAllUpper(string)) {
            string = string.toLowerCase();
        }

        int len = string.length();
        FastStringBuffer buf = new FastStringBuffer(len);
        boolean first = true;
        int nextUpper = 0;

        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (first) {
                if (c == '_') {
                    //underscores at the start
                    buf.append(c);
                    continue;
                }
                buf.append(Character.toLowerCase(c));
                first = false;
            } else {

                if (c == '_') {
                    nextUpper += 1;
                    continue;
                }
                if (nextUpper > 0) {
                    c = Character.toUpperCase(c);
                    nextUpper = 0;
                }

                buf.append(c);
            }
        }

        if (nextUpper > 0) {
            //underscores at the end
            buf.appendN('_', nextUpper);
        }
        return buf.toString();
    }

    public static String asStyleCamelCaseFirstUpper(String string) {
        string = asStyleCamelCaseFirstLower(string);
        if (string.length() > 0) {
            return Character.toUpperCase(string.charAt(0)) + string.substring(1);
        }
        return string;
    }

    public static boolean endsWith(FastStringBuffer str, char c) {
        if (str.length() == 0) {
            return false;
        }
        if (str.charAt(str.length() - 1) == c) {
            return true;
        }
        return false;
    }

    public static boolean endsWith(final String str, char c) {
        int len = str.length();
        if (len == 0) {
            return false;
        }
        if (str.charAt(len - 1) == c) {
            return true;
        }
        return false;
    }

    public static boolean endsWith(final StringBuffer str, char c) {
        int len = str.length();
        if (len == 0) {
            return false;
        }
        if (str.charAt(len - 1) == c) {
            return true;
        }
        return false;
    }

    public static String safeDecodeByteArray(byte[] b, String baseCharset) {
        try {
            if (baseCharset == null) {
                baseCharset = "ISO-8859-1";
            }
            return new String(b, baseCharset);
        } catch (Exception e) {
            try {
                //If it fails, go for something which shouldn't fail!
                CharsetDecoder decoder = Charset.forName(baseCharset).newDecoder();
                decoder.onMalformedInput(CodingErrorAction.IGNORE);
                decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
                CharBuffer parsed = decoder.decode(ByteBuffer.wrap(b, 0, b.length));
                return parsed.toString();
            } catch (Exception e2) {
                Log.log(e2);
                //Shouldn't ever happen!
                return new String("Unable to decode bytearray from Python.");
            }
        }
    }

    /**
     * Decodes some string that was encoded as base64
     */
    public static byte[] decodeBase64(String persisted) {
        return Base64Coder.decode(persisted.toCharArray());
    }

    /**
     * @param o the object we want as a string
     * @return the string representing the object as base64
     */
    public static String getObjAsStr(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(o);
            stream.close();
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }

        return new String(encodeBase64(out));
    }

    /**
     * @return the contents of the passed ByteArrayOutputStream as a byte[] encoded with base64.
     */
    public static char[] encodeBase64(ByteArrayOutputStream out) {
        byte[] byteArray = out.toByteArray();
        return encodeBase64(byteArray);
    }

    /**
     * @return the contents of the passed byteArray[] as a byte[] encoded with base64.
     */
    public static char[] encodeBase64(byte[] byteArray) {
        return Base64Coder.encode(byteArray);
    }

    public static boolean containsWhitespace(final String name) {
        final int len = name.length();
        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(name.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String getWithFirstUpper(final String creationStr) {
        final int len = creationStr.length();
        if (len == 0) {
            return creationStr;
        }
        char upperCase = Character.toUpperCase(creationStr.charAt(0));
        return upperCase + creationStr.substring(1);

    }

    public static String indentTo(String source, String indent) {
        return indentTo(source, indent, true);
    }

    public static String indentTo(final String source, final String indent, boolean indentFirstLine) {
        final int indentLen = indent.length();
        if (indent == null || indentLen == 0) {
            return source;
        }
        List<String> splitInLines = splitInLines(source);
        final int sourceLen = source.length();
        FastStringBuffer buf = new FastStringBuffer(sourceLen + (splitInLines.size() * indentLen) + 2);

        for (int i = 0; i < splitInLines.size(); i++) {
            String line = splitInLines.get(i);
            if (indentFirstLine || i > 0) {
                buf.append(indent);
            }
            buf.append(line);
        }
        return buf.toString();
    }

    public static String reverse(String lineContentsToCursor) {
        return new FastStringBuffer(lineContentsToCursor, 0).reverse().toString();
    }

    /**
     * Split so that we can create multiple WildcardQuery.
     *
     * Note that it accepts wildcards (such as * or ? but if an entry would contain
     * only wildcards it'd be ignored).
     *
     * Also, anything which Character.isJavaIdentifierPart does not match is considered
     * to be a separator and will be ignored.
     */
    public static List<String> splitForIndexMatching(String string) {
        int len = string.length();
        if (len == 0) {
            return new ArrayList<>(0);
        }
        ArrayList<String> ret = new ArrayList<String>();

        int last = 0;

        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '*' && c != '?') {
                if (last != i) {
                    String substring = string.substring(last, i);
                    if (!containsOnlyWildCards(substring)) {
                        ret.add(substring);
                    }
                }
                while (!Character.isJavaIdentifierPart(c) && c != '*' && c != '?' && i < len - 1) {
                    i++;
                    c = string.charAt(i);
                }
                last = i;
            }
        }
        if (Character.isJavaIdentifierPart(c) || c == '*' || c == '?') {
            if (last == 0 && len > 0) {
                if (!containsOnlyWildCards(string)) {
                    ret.add(string); //it is equal to the original (no char to split)
                }

            } else if (last < len) {
                String substring = string.substring(last, len);
                if (!containsOnlyWildCards(substring)) {
                    //Don't add if it has only wildcards in it.
                    ret.add(substring);
                }
            }
        }
        return ret;
    }

    public static void checkTokensValidForWildcardQuery(String token) {
        List<String> splitForIndexMatching = StringUtils.splitForIndexMatching(token);

        if (splitForIndexMatching == null || splitForIndexMatching.size() == 0) {
            throw new RuntimeException(StringUtils.format(
                    "Token: %s is not a valid token to search for.", token));
        }
    }

    public static boolean containsOnlyWildCards(String string) {
        boolean onlyWildCardsInPart = true;
        int length = string.length();
        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            if (c != '*' && c != '?') {
                onlyWildCardsInPart = false;
                break; //break inner for
            }
        }
        return onlyWildCardsInPart;
    }

}
