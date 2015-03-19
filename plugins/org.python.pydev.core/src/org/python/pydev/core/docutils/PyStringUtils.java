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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This is an extension to the String utils so 
 * @author Fabio
 *
 */
public final class PyStringUtils {

    private PyStringUtils() {

    }

    private static final boolean DEBUG = false;

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
     *         {@link PyStringUtils#indexOf} returns <tt>-1</tt>
     * @see {@link PyStringUtils#indexOf} 
     */
    public static String findSubstring(final String string, final char character, final boolean ignoreInStringLiteral) {

        String result = null;
        int index = PyStringUtils.indexOf(string, character, ignoreInStringLiteral);

        if (index >= 0) {
            result = string.substring(index + 1);
        }
        return result;
    }

    /**
     * Formats a docstring to be shown and adds the indentation passed to all the docstring lines but the 1st one.
     */
    public static String fixWhitespaceColumnsToLeftFromDocstring(String docString, String indentationToAdd) {
        FastStringBuffer buf = new FastStringBuffer();
        List<String> splitted = StringUtils.splitInLines(docString);
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

        List<String> splitted = StringUtils.splitInLines(hoverInfo);
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

        List<String> splitted = StringUtils.splitInLines(code);
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
     * Splits some string given some char (that char will not appear in the returned strings)
     * Empty strings are also never added.
     */
    public static void splitWithIntern(String string, char toSplit, Collection<String> addTo) {
        synchronized (ObjectsInternPool.lock) {
            int len = string.length();

            int last = 0;

            char c = 0;

            for (int i = 0; i < len; i++) {
                c = string.charAt(i);
                if (c == toSplit) {
                    if (last != i) {
                        addTo.add(ObjectsInternPool.internUnsynched(string.substring(last, i)));
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
                    addTo.add(ObjectsInternPool.internUnsynched(string)); //it is equal to the original (no char to split)

                } else if (last < len) {
                    addTo.add(ObjectsInternPool.internUnsynched(string.substring(last, len)));
                }
            }
        }
    }

    /**
     * Tests whether each character in the given string is a valid identifier.
     *
     * @param str
     * @return <code>true</code> if the given string is a word
     */
    public static boolean isValidIdentifier(final String str, boolean acceptPoint) {
        int len = str.length();
        if (str == null || len == 0) {
            return false;
        }

        char c = '\0';
        boolean lastWasPoint = false;
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (i == 0) {
                if (!Character.isJavaIdentifierStart(c)) {
                    return false;
                }
            } else {
                if (!Character.isJavaIdentifierPart(c)) {
                    if (acceptPoint && c == '.') {
                        if (lastWasPoint) {
                            return false; //can't have 2 consecutive dots.
                        }
                        lastWasPoint = true;
                        continue;
                    }
                    return false;
                }
            }
            lastWasPoint = false;

        }
        if (c == '.') {
            //if the last char is a point, don't accept it (i.e.: only accept at middle).
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
                    char peer = StringUtils.getPeer(c);
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

    public static String urlEncodeKeyValuePair(String key, String value) {
        String result = null;

        try {
            result = URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.log(e);
        }

        return result;
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

    public static String getExeAsFileSystemValidPath(String executableOrJar) {
        return "v1_" + StringUtils.md5(executableOrJar);
    }

}
