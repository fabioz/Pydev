/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;

import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.Base64Coder;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

public final class StringUtils extends org.python.pydev.shared_core.string.StringUtils {

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

        if (null == string || (character < 0) || string.length() == 0) {
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
        for (int i = 0; i < times; i++) {
            buffer.append(s);
        }
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
        if (str == null || len == 0) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
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
                    char peer = org.python.pydev.shared_core.string.StringUtils.getPeer(c);
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

    public static int count(String name, char c) {
        return org.python.pydev.shared_core.string.StringUtils.count(name, c);
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

    public static String getExeAsFileSystemValidPath(String executableOrJar) {
        return "v1_" + org.python.pydev.shared_core.string.StringUtils.md5(executableOrJar);
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
}
