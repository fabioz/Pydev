/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.cache.Cache;
import org.python.pydev.core.cache.LRUCache;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;

public final class StringUtils {

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

    private StringUtils() {

    }

    public static final String EMPTY = "";

    private static final boolean DEBUG = false;

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
     * <p>Find the index of <tt>character</tt> in a <tt>string</tt>.</p>
     * 
     * <p>This method is like {@link java.lang.String#indexOf(int)} 
     * but has the additional ability to ignore occurrences of 
     * <tt>character</tt> in Python string literals (e.g. enclosed
     * by single, double or triple quotes). This is done by employing
     * a very simple statemachine.</p>
     * 
     * @param string - the source string, e.g. the <i>haystack</i>
     * @param character - the character to retrieve the index for
     * @param ignoreInStringLiteral - if <tt>true</tt>, ignore occurrences 
     *        of <tt>character</tt> in Python string literals
     * @return the position of the character in string.<br>
     *         if <tt>string</tt> is <tt>null</tt> or empty, or
     *         if <tt>(int)character < 0</tt>, returns <tt>-1</tt>.
     * @note escaped (i.e. <tt>\"</tt>) characters are ignored and 
     *       control characters, e.g. line delimiters etc., are treated 
     *       normally like every other character.
     */
    public static int indexOf(final String string, final char character, final boolean ignoreInStringLiteral) {

        if (null == string || ((int) character < 0) || string.length() == 0) {
            return -1;
        }

        int index = string.indexOf(character);

        if (-1 == index) {
            return index;
        }

        if (ignoreInStringLiteral) {
            final int len = string.length();
            boolean inString = false;
            char nextc = '\0';
            char c = '\0';

            int i = -1;

            try {
                while (i < len) {
                    i++;
                    c = string.charAt(i);
                    if ((i + 1) < len) {
                        nextc = string.charAt(i + 1);
                    }
                    if ('\\' == c) { // ignore escapes
                        i++;
                        continue;
                    }
                    if (!inString && character == c) {
                        index = i;
                        break;
                    }
                    if ('"' == c || '\'' == c) {
                        if ('"' == nextc || '\'' == nextc) {
                            i++;
                            continue;
                        } else {
                            if (inString) {
                                inString = false;
                            } else {
                                inString = true;
                            }
                        }
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
                // malformed Python string literals may throw a SIOOBE
                if (DEBUG) {
                    System.err.print(e.getMessage());
                }
                index = -1;
            }
        }
        return index;
    }

    /**
     * <p>Find the substring in <tt>string</tt> that starts from the first 
     * occurrence of <tt>character</tt>.</p>
     * 
     * <p>This method is similar to {@link java.lang.String#substring} 
     * but has the additional ability to ignore occurrences of 
     * <tt>character</tt> in Python string literals (e.g. enclosed
     * by single, double or triple single/double quotes).</p>
     * 
     * @param string - the source string, e.g. the <i>haystack</i>
     * @param character - the character that is the starting boundary of the searched substring
     * @param ignoreInStringLiteral - if <tt>true</tt>, ignore occurrences 
     *        of <tt>character</tt> in Python string literals
     * @return a substring from <tt>string</tt><br>or <tt>null</tt> if 
     *         {@link StringUtils#indexOf} returns <tt>-1</tt>
     * @see {@link StringUtils#indexOf} 
     */
    public static String findSubstring(final String string, final char character, final boolean ignoreInStringLiteral) {

        String result = null;
        int index = StringUtils.indexOf(string, character, ignoreInStringLiteral);

        if (index >= 0) {
            result = string.substring(index + 1);
        }
        return result;
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
        if (!iter.hasNext())
            return "";
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
        for (int i = 0; i < times; i++) {
            buffer.append(s);
        }
        return buffer.toString();
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
     * Can be used instead of JDK 1.5's <tt>String.isEmpty()</tt> so that
     * code intentions can be made explicit while keeping support for 1.4 and below.
     * @param str
     * @return true if length of string is 0, otherwise false
     */
    public static Boolean isEmpty(String str) {
        return (0 == str.length());
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
     * Given a string remove all from the rightmost '.' onwards.
     * 
     * E.g.: bbb.t would return bbb
     * 
     * If it has no '.', returns the original string unchanged.
     */
    public static String stripExtension(String input) {
        return stripFromRigthCharOnwards(input, '.');
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
     * Removes the occurrences of the passed char in the beggining of the string.
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

    public static String replaceAll(String string, String replace, String with) {
        FastStringBuffer ret = new FastStringBuffer(string, 16);
        return ret.replaceAll(replace, with).toString();
    }

    /**
     * Formats a docstring to be shown and adds the indentation passed to all the docstring lines but the 1st one.
     */
    public static String fixWhitespaceColumnsToLeftFromDocstring(String docString, String indentationToAdd) {
        FastStringBuffer buf = new FastStringBuffer();
        List<String> splitted = splitInLines(docString);
        for (int i = 0; i < splitted.size(); i++) {
            String initialString = splitted.get(i);
            if (i == 0) {
                buf.append(initialString);//first is unchanged
            } else {
                String string = StringUtils.leftTrim(initialString);
                buf.append(indentationToAdd);

                if (string.length() > 0) {
                    buf.append(string);
                } else {
                    int length = initialString.length();
                    if (length > 0) {
                        char c;
                        if (length > 1) {
                            //check 2 chars
                            c = initialString.charAt(length - 2);
                            if (c == '\n' || c == '\r') {
                                buf.append(c);
                            }
                        }
                        c = initialString.charAt(length - 1);
                        if (c == '\n' || c == '\r') {
                            buf.append(c);
                        }
                    }
                }
            }
        }

        //last line
        if (buf.length() > 0) {
            char c = buf.lastChar();
            if (c == '\r' || c == '\n') {
                buf.append(indentationToAdd);
            }
        }

        return buf.toString();
    }

    public static String removeWhitespaceColumnsToLeft(String hoverInfo) {
        FastStringBuffer buf = new FastStringBuffer();
        int firstCharPosition = Integer.MAX_VALUE;

        List<String> splitted = splitInLines(hoverInfo);
        for (String line : splitted) {
            if (line.trim().length() > 0) {
                int found = PySelection.getFirstCharPosition(line);
                firstCharPosition = Math.min(found, firstCharPosition);
            }
        }

        if (firstCharPosition != Integer.MAX_VALUE) {
            for (String line : splitted) {
                if (line.length() > firstCharPosition) {
                    buf.append(line.substring(firstCharPosition));
                }
            }
            return buf.toString();
        } else {
            return hoverInfo;//return initial
        }
    }

    public static String removeWhitespaceColumnsToLeftAndApplyIndent(String code, String indent,
            boolean indentCommentLinesAt0Pos) {
        FastStringBuffer buf = new FastStringBuffer();
        int firstCharPosition = Integer.MAX_VALUE;

        List<String> splitted = splitInLines(code);
        for (String line : splitted) {
            if (indentCommentLinesAt0Pos || !line.startsWith("#")) {
                if (line.trim().length() > 0) {
                    int found = PySelection.getFirstCharPosition(line);
                    firstCharPosition = Math.min(found, firstCharPosition);
                }
            }
        }

        if (firstCharPosition != Integer.MAX_VALUE) {
            for (String line : splitted) {
                if (indentCommentLinesAt0Pos || !line.startsWith("#")) {
                    buf.append(indent);
                    if (line.length() > firstCharPosition) {
                        buf.append(line.substring(firstCharPosition));
                    } else {
                        buf.append(line);
                    }
                } else {
                    buf.append(line);
                }
            }
            return buf.toString();
        } else {
            return code;//return initial
        }
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
     * Splits the passed string based on the toSplit string.
     */
    public static List<String> split(final String string, final char toSplit, int maxPartsToSplit) {
        Assert.isTrue(maxPartsToSplit > 0);
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

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
     */
    public static List<String> split(final String string, final String toSplit) {
        if (toSplit.length() == 1) {
            return split(string, toSplit.charAt(0));
        }
        ArrayList<String> ret = new ArrayList<String>();
        if (toSplit.length() == 0) {
            ret.add(string);
            return ret;
        }

        int len = string.length();

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
    public static void splitWithIntern(String string, char toSplit, Collection<String> addTo) {
        synchronized (ObjectsPool.lock) {
            int len = string.length();

            int last = 0;

            char c = 0;

            for (int i = 0; i < len; i++) {
                c = string.charAt(i);
                if (c == toSplit) {
                    if (last != i) {
                        addTo.add(ObjectsPool.internUnsynched(string.substring(last, i)));
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
                    addTo.add(ObjectsPool.internUnsynched(string)); //it is equal to the original (no char to split)

                } else if (last < len) {
                    addTo.add(ObjectsPool.internUnsynched(string.substring(last, len)));
                }
            }
        }
    }

    /**
     * Splits some string given some char (that char will not appear in the returned strings)
     * Empty strings are also never added.
     */
    public static List<String> split(String string, char toSplit) {
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

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

    private static boolean contains(char c, char[] toSplit) {
        for (char ch : toSplit) {
            if (c == ch) {
                return true;
            }
        }
        return false;
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

    public static String join(String delimiter, String[] splitted) {
        return (String) join(delimiter, splitted, null);
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
                return EMPTY;
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
        return join(delimiter, s);
    }

    /**
     * Same as Python join: Go through all the paths in the string and join them with the passed delimiter.
     */
    public static String join(String delimiter, List<String> splitted) {
        return (String) join(delimiter, splitted.toArray(new String[splitted.size()]), null);
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

    public static String replaceNewLines(String message, String string) {
        message = message.replaceAll("\r\n", string);
        message = message.replaceAll("\r", string);
        message = message.replaceAll("\n", string);

        return message;
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

    /**
     * Tests whether each character in the given
     * string is a letter.
     *
     * @param str
     * @return <code>true</code> if the given string is a word
     */
    public static boolean isWord(final String str) {
        int len = str.length();
        if (str == null || len == 0)
            return false;

        for (int i = 0; i < len; i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i)))
                return false;
        }
        return true;
    }

    /**
     * An array of Python pairs of characters that you will find in any Python code.
     * 
     * Currently, the set contains:
     * <ul>
     * <ol>left and right brackets: [, ]</ol>
     * <ol>right and right parentheses: (, )
     * </ul>
     */
    public static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };
    public static final char[] CLOSING_BRACKETS = { '}', ')', ']' };

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
        }

        throw new NoPeerAvailableException("Unable to find peer for :" + c);
    }

    public static String getWithClosedPeer(char c) {
        switch (c) {
            case '{':
                return "{}";
            case '(':
                return "()";
            case '[':
                return "[]";
            case '\'':
                return "''";
            case '"':
                return "\"\"";
        }

        throw new NoPeerAvailableException("Unable to find peer for :" + c);
    }

    public static boolean isOpeningPeer(char lastChar) {
        return lastChar == '(' || lastChar == '[' || lastChar == '{';
    }

    public static boolean isClosingPeer(char lastChar) {
        return lastChar == ')' || lastChar == ']' || lastChar == '}';
    }

    public static boolean hasOpeningBracket(String trimmedLine) {
        return trimmedLine.indexOf('{') != -1 || trimmedLine.indexOf('(') != -1 || trimmedLine.indexOf('[') != -1;
    }

    public static boolean hasClosingBracket(String trimmedLine) {
        return trimmedLine.indexOf('}') != -1 || trimmedLine.indexOf(')') != -1 || trimmedLine.indexOf(']') != -1;
    }

    public static boolean hasUnbalancedClosingPeers(final String line) {
        Map<Character, Integer> stack = new HashMap<Character, Integer>();
        final int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            switch (c) {
                case '(':
                case '{':
                case '[':
                    Integer iStack = stack.get(c);
                    if (iStack == null) {
                        iStack = 0;
                    }
                    iStack++;
                    stack.put(c, iStack);
                    break;

                case ')':
                case '}':
                case ']':
                    char peer = StringUtils.getPeer((char) c);
                    iStack = stack.get(peer);
                    if (iStack == null) {
                        iStack = 0;
                    }
                    iStack--;
                    stack.put(peer, iStack);
                    break;
            }
        }
        for (int i : stack.values()) {
            if (i < 0) {
                return true;
            }
        }
        return false;
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

    public static String urlEncodeKeyValuePair(String key, String value) {
        String result = null;

        try {
            result = URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.log(e);
        }

        return result;
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

    /**
     * //Python 3.0 can use unicode identifiers. So, the letter construct deals with that...
     *  TOKEN : * Python identifiers *
     *  {
     *      < NAME: <LETTER> ( <LETTER> | <DIGIT>)* >
     *  |
     *      < #LETTER: 
     *      [
     *         "a"-"z",
     *         "A"-"Z",
     *         "_",
     *         "\u0080"-"\uffff" //Anything more than 128 is considered valid (unicode range)
     *      
     *      ] 
     *  >
     *  }
     * @param param
     * @return
     */
    public static boolean isPythonIdentifier(final String param) {
        final int len = param.length();
        if (len == 0) {
            return false;
        }
        char c = param.charAt(0);
        if (!Character.isLetter(c) && c != '_' && c <= 128) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            c = param.charAt(i);
            if ((!Character.isLetter(c) && !Character.isDigit(c) && c != '_') && (c <= 128)) {
                return false;
            }
        }
        return true;
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

    private static final Object md5CacheLock = new Object();
    private static final LRUCache<String, String> md5Cache = new LRUCache<String, String>(1000);

    public static String md5(String str) {
        synchronized (md5CacheLock) {
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

    public static String getExeAsFileSystemValidPath(String executableOrJar) {
        return "v1_" + StringUtils.md5(executableOrJar);
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

}
