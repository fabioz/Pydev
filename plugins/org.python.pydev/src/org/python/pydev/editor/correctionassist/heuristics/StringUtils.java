package org.python.pydev.editor.correctionassist.heuristics;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <p><tt>StringUtils</tt> provides implementations of missing 
 * <tt>String</tt> operations.</p>
 * 
 * <p>Note: ideally these should be merged with
 * {@link org.python.pydev.core.docutils.StringUtils} but let's
 * see if my code get's accepted by upstream first.</p>
 * 
 * @author André Berg
 * @version 0.2
 */
public final class StringUtils {

    private static final boolean DEBUG = false;

    /**
     * Prevent instance creation. 
     */
    private StringUtils() {
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
        
        if (null == string || null == regex || string.isEmpty() || regex.isEmpty()) {
            return index;
        }
        
        Pattern pat;
        try {
            pat = Pattern.compile(regex);
        } catch (PatternSyntaxException pse) {
            return index;
        }

        int len = string.length();
        int i = len-1;
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
        
        if (null == string || ((int) character < 0) || string.isEmpty()) {
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
                    if ((i+1) < len) {
                        nextc = string.charAt(i+1);
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
            result = string.substring(index+1);
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
    public static <T> String joinIterable(final Iterable<T> objs, final String delimiter) throws IllegalArgumentException {

        if (null == objs) {
            throw new IllegalArgumentException("objs can't be null!");
        }
        if (null == delimiter) {
            throw new IllegalArgumentException("delimiter can't be null");
        }

        Iterator<T> iter = objs.iterator();
        if (!iter.hasNext()) return "";
        StringBuffer buffer = new StringBuffer(String.valueOf(iter.next()));
        while (iter.hasNext()) {
            buffer.append(delimiter).append(String.valueOf(iter.next()));
        }
        
        return buffer.toString();
    }
    
    /**
     * <p>Join the elements of an array of <tt>Object</tt>s by using <tt>delimiter</tt> 
     * as separator.</p>
     * 
     * @param objs - array of <tt>Object</tt>s
     * @param delimiter - string used as separator
     * 
     * @throws IllegalArgumentException if <tt>objs</tt> or <tt>delimiter</tt> 
     *         is <tt>null</tt>.
     *         
     * @return joined string
     */
    public static String joinArray(final Object[] objs, final String delimiter) throws IllegalArgumentException {
        
        if (null == objs) {
            throw new IllegalArgumentException("objs can't be null!");
        }
        if (null == delimiter) {
            throw new IllegalArgumentException("delimiter can't be null");
        }
        if (0 == objs.length) {
            return "";
        }
        
        StringBuffer buffer = new StringBuffer();
        int len = objs.length;
        int stopLen = len-1;
        for (int i = 0; i < len; i++) {
            Object obj = objs[i];
            buffer.append(String.valueOf(obj));
            if (i < stopLen) {
                buffer.append(delimiter);
            }
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
        if (s.isEmpty() || times <= 0) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < times; i++) {
            buffer.append(s);
        }
        return buffer.toString();
    }
}
