/******************************************************************************
* Copyright (C) 2011-2013  André Berg and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>           - ongoing maintenance
******************************************************************************/
/**
 * Convert %-format strings to {}-format.
 * 
 * @author      André Berg
 * @contact     http://github.com/andreberg
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * The <tt>PercentToBraceConverter</tt> class deals with converting 
 * traditional Python format strings (also known as <i>percent notation</i> or
 * <i>string interpolation syntax</i>) into the more recently advocated
 * template format mini-language. 
 * 
 * <p>Usage<p>
 * 
 * <tt>String strToConvert = "'Hi, my name is %s' % name";</tt><br>
 * <tt>PercentToBraceConverter ptbc = new PercentToBraceConverter(strToConvert);</tt><br>
 * <tt>String convertedResult = ptbc.convert();</tt>
 * 
 * <p><tt>convertedResult</tt> would now be <tt>'Hi, my name is {0!s}'.format(name)</tt></p>
 * 
 * <p>Caveats</p>
 * 
 * <p><b>PercentToBraceConverter is not synchronized.</b></p>
 * 
 * <p>A <tt>PercentToBraceConverter</tt> instance also can't be reused or reset.
 * If you want to convert another format string, you need to create a new instance.</p>
 * 
 * <p>If either the <i>format string contents</i> or the <i>interpolation values</i> 
 * span multiple lines, <tt>PercentToBraceConverter</tt> will currently (falsely) process 
 * them as being a single line. It can, however, process multiple lines containing multiple 
 * format strings where each format string is on one line.</p>
 * 
 * @version 0.7
 * @author André Berg
 */
public final class PercentToBraceConverter {

    private int argIndex;
    private String initialSourceString;

    private String matchedFormatString;
    private String head;
    private String tail;
    private boolean skipFormatCallReplacement;
    private int length;

    private static final boolean DEBUG = false;

    // for the pattern below <num>: gives the matched group number
    /**
     * <p>Pattern for matching a complete Python format string.<br>
     * Matches i.e.: <code>"value: %0.2f" % price</code>, etc.</p>
     * 
     * <p>Groups:</p>
     * 
     * <ol>
     *  <li>complete format string (e.g. <code>"value: %0.2f"</code>)</li>
     *  <li>string character (e.g. <code>",'</code>)</li>
     *  <li>interpolant token (e.g. <code> % </code>)</li>
     *  <li>interpolation values (e.g. <code>price</code>)</li>
     * </ol>
     */
    private static final Pattern FMTSTR_PATTERN = Pattern.compile(
            "(" + // 1: format string
                    "(?:r|u|ru|ur)?" + //    literal specifier (optional) 
                    "([\"']{1}|['\"]{3})" + // 2: string character " ', etc.   
                    "(?!\\2)" + //    make sure we don't match ''' or """ as '.' and "."
                    ".+?" + //    format string contents
                    "(?:\\2)" + // 2: string character backref     
                    ")" + //                                 
                    "(\\s*?%\\s*)" + // 3: interpolant token (%)        
                    "(.+)$", // 4: interpolation values         
            Pattern.COMMENTS
            );

    /**
     * Initialize a new <tt>PercentToBraceConverter</tt> instance.
     * @param formatStringToConvert - the <i>percent syntax</i> format 
     * string to convert.
     * @throws IllegalArgumentException if <tt>formatStringToConvert</tt>
     * is null. 
     * @pre formatStringToConvert != null
     */
    public PercentToBraceConverter(final String formatStringToConvert) {
        if (null == formatStringToConvert) {
            throw new IllegalArgumentException("formatStringToConvert can't be null!");
        }
        initialSourceString = formatStringToConvert;
        matchedFormatString = "";
        head = "";
        tail = "";
        length = 0;
        argIndex = 0;
    }

    /**
     * Process the format string that the instance was initialized with.
     * @return the converted format string, including a replaced interpolant 
     * token if applicable (i.e. <tt>"…" % (…)</tt> replaced with a <tt>format()</tt> 
     * call: <tt>"…".format(…)</tt>).
     * @pre argIndex != 0 && length == 0
     * @post argIndex == 0 && length != 0
     */
    public String convert() {

        if (initialSourceString.isEmpty()) {
            return initialSourceString;
        }

        final String input = initialSourceString;
        final String[] lines = input.split("\\r?\\n");

        final List<String> results = new ArrayList<String>();

        // FIXME: might need to get the appropriate line delimiter from Workbench prefs store.
        // see org.eclipse.core.runtime.Platform.PREF_LINE_SEPARATOR
        final String sep = System.getProperty("line.separator");

        for (String line : lines) {

            String result = "";

            // needs to be stored back because of head and tail assessment
            initialSourceString = line;

            final Matcher formatStringMatcher = FMTSTR_PATTERN.matcher(line);
            final Matcher tokenMatcher = PercentConversion.getTokenPattern().matcher(new String(line));

            boolean isFormatString = false;
            if (formatStringMatcher.find()) {
                if (4 == formatStringMatcher.groupCount()) {

                    isFormatString = true;
                    matchedFormatString = formatStringMatcher.group(0);

                    if (DEBUG) {
                        System.out.printf(
                                "------" + sep +
                                        "Match: ‘%s‘" + sep +
                                        "------" + sep, matchedFormatString);

                        final String fmtString = formatStringMatcher.group(1);
                        final String strChar = formatStringMatcher.group(2);
                        final String interpToken = formatStringMatcher.group(3);
                        final String interpValues = formatStringMatcher.group(4);

                        System.out.printf(sep +
                                "  Format String: ‘%s‘" + sep +
                                "  String Character: ‘%s‘" + sep +
                                "  Interpolant Token: ‘%s‘" + sep +
                                "  Interpolant Values: ‘%s‘" + sep,
                                fmtString, strChar, interpToken, interpValues);
                    }

                    storeHeadAndTail();

                    if (!skipFormatCallReplacement) {
                        String replaced = replaceInterpolantTokenWithFormatCall(formatStringMatcher);
                        if (!matchedFormatString.equals(replaced)) {
                            updateStrings(replaced);
                        }
                    }
                } else {
                    isFormatString = false;
                }
            }
            if (isFormatString) {
                final List<PercentConversion> conversions = new ArrayList<PercentConversion>();
                PercentConversion conv = null;

                while (tokenMatcher.find()) {
                    if (tokenMatcher.groupCount() >= 1) {
                        if (DEBUG) {
                            System.out.printf(sep +
                                    "------" + sep +
                                    "Match: ‘%s‘" + sep +
                                    "------" + sep, tokenMatcher.group(0));

                            final String key = tokenMatcher.group(1);
                            final String flags = tokenMatcher.group(2);
                            final String width = tokenMatcher.group(3);
                            final String precision = tokenMatcher.group(4);
                            final String length = tokenMatcher.group(5);
                            final String conversion = tokenMatcher.group(6);

                            System.out.printf(sep +
                                    "  Mapping Key: ‘%s‘" + sep +
                                    "  Flags: ‘%s‘" + sep +
                                    "  Width ‘%s‘" + sep +
                                    "  Precision: ‘%s‘" + sep +
                                    "  Length: ‘%s‘" + sep +
                                    "  Conversion: ‘%s‘" + sep,
                                    key, flags, width, precision, length, conversion);
                        }

                        conv = new PercentConversion(this, tokenMatcher.toMatchResult());
                        //System.out.println("Conversion.toString = " + conv.toString());
                        conversions.add(conv);
                    }
                }

                final ListIterator<PercentConversion> li = conversions.listIterator(conversions.size());

                String converted = null;
                while (li.hasPrevious()) {
                    conv = li.previous();

                    int[] span = conv.getSpan();
                    converted = conv.toBrace();

                    if (DEBUG) { // $codepro.audit.disable constantCondition, constantConditionalExpression
                        System.out.println(sep +
                                "Converted: " + converted);
                        System.out.format("Span: [%d:%d]" + sep, span[0], span[1]);
                    }

                    result = insertIntoResult(converted, span);
                    updateStrings(result);
                }

                // check post-condition
                Assert.isTrue(argIndex <= 0,
                        "W: argIndex shouldn't be greater than zero when all tokens are consumed, but is " + argIndex);

            } else {
                result = initialSourceString;
            }

            results.add(result);
        }

        String convertedString = null;

        if (1 == results.size()) {
            // single line - most common case
            convertedString = results.get(0);
        } else if (results.size() > 1) {
            // multiple lines
            convertedString = StringUtils.join(sep, results.toArray());
        } else {
            // this should never happen
            Assert.isTrue(false, "E: there must always be one result even if " +
                    "the source string is not a valid percent format string.");
        }

        return convertedString;
    }

    /**
     * Two <tt>PercentToBraceConverter</tt> instances are equal if 
     * their <tt>initialSourceString</tt> and <tt>skipFormatCallReplacement</tt> 
     * fields are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PercentToBraceConverter)) {
            return false;
        }
        PercentToBraceConverter other = (PercentToBraceConverter) obj;
        if (!initialSourceString.equals(other.initialSourceString)) {
            return false;
        }
        if (skipFormatCallReplacement != other.skipFormatCallReplacement) {
            return false;
        }
        return true;
    }

    /**
     * Get the final length.
     * @return length of the processed and converted string
     */
    public int getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (1 + initialSourceString.hashCode()) << (skipFormatCallReplacement ? 1 : 0);
    }

    /**
     * Return status of the <tt>.format(…)</tt> call replacement.
     */
    public boolean isSkippingFormatCallReplacement() {
        return skipFormatCallReplacement;
    }

    /**
     * <p>Pass <tt>true</tt> to disable support for replacement of the interpolant 
     * term with a <tt>.format(…)</tt> call.</p>
     * 
     * <p>Technically there should be no need to do this because Python will respond
     * to a converted format string that attempts to use an interpolant term instead of a 
     * <tt>.format(…)</tt> call with a <tt>TypeError: not all arguments converted during 
     * string formatting</tt>.</p>
     * 
     * <p>Only use this if you have tricky input data where you need to perform the 
     * replacement yourself.</p>
     */
    public void setSkipFormatCallReplacement(boolean skipFormatCallReplacement) {
        this.skipFormatCallReplacement = skipFormatCallReplacement;
    }

    /**
     * <p>Returns <tt>true</tt> if <tt>aString</tt> is or contains a Python 
     * format string in <i>interpolation syntax</i>.</p>
     * 
     * <p>Used as a means to determine up-front if a conversion is necessary.</p>
     * 
     * @param aString - the string to test
     * @param splitLines - if true, split the string using regex <tt>\r?\n</tt> and 
     *        return true if one line contains a format string. Otherwise, the 
     *        {@link #FMTSTR_PATTERN} will be matched against the input string,
     *        without special behavior if it is spanning multiple lines.
     * @return <tt>true</tt>/<tt>false</tt>
     */
    public static boolean isValidPercentFormatString(final String aString, final boolean splitLines) {

        boolean result = false;
        Matcher matcher = null;

        if (true == splitLines) {
            final String[] lines = aString.split("\\r?\\n");
            for (String line : lines) {
                matcher = PercentToBraceConverter.getFormatStringPattern().matcher(line);
                if (matcher.find()) {
                    result = true;
                    break;
                }
            }
        } else {
            matcher = PercentToBraceConverter.getFormatStringPattern().matcher(aString);
            if (matcher.find()) {
                result = true;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String description =
                MessageFormat.format("<{0}@0x{1} | source={2} match={3} argIndex={4} head={5} tail={6}>",
                        this.getClass().getSimpleName(), Integer.toHexString(this.hashCode()),
                        initialSourceString, matchedFormatString, argIndex, head, tail);
        return description;
    }

    /**
     * @return the Pattern used to match Python format strings.<br>
     * @see {@link #FMTSTR_PATTERN}, 
     * @see {@link #extractFormatStringGroups}
     */
    private static Pattern getFormatStringPattern() {
        return PercentToBraceConverter.FMTSTR_PATTERN;
    }

    /**
     * Build a group dictionary from the matched groups. <br>
     * Guaranteed to contain 4 keys:<br>
     * <ol>
     *   <li><tt>FormatString</tt>: the complete format string part</li>
     *   <li><tt>StringCharacter</tt>: the literal delimiter used (e.g. <tt>",',''',"""</tt></li>
     *   <li><tt>InterpolantToken</tt>: the percent sign, used to signal string interpolation</li>
     *   <li><tt>InterpolationValues</tt>: the values for filling in the specifier tokens in the format string.</li>
     * </ol>
     * @param matchResult
     * @return the K, V mapped groups.
     * @see {@link #FMTSTR_PATTERN}
     */
    private static Map<String, String> extractFormatStringGroups(final MatchResult matchResult) {

        // in a strict sense the assertion here is superfluous since the enclosing 
        // context in the caller already does a check if the group count is 4 and 
        // the execution branch in which this method is called will not be run if
        // it isn't 4.
        Assert.isLegal(4 == matchResult.groupCount(),
                "E: Match result from FMTSTR_PATTERN is malformed. Group count must be 4.");

        final Map<String, String> result = new HashMap<String, String>(4);

        final String fmtString = matchResult.group(1);
        final String strChar = matchResult.group(2);
        final String interpToken = matchResult.group(3);
        final String interpValues = matchResult.group(4);

        result.put("FormatString", fmtString);
        result.put("StringCharacter", strChar);
        result.put("InterpolantToken", interpToken);
        result.put("InterpolationValues", interpValues);

        return result;
    }

    /**
     * Partitions the initial source string using the passed span, as well as the
     * offset between source and match string, and inserts in the middle the converted 
     * token.
     * 
     * @param convertedToken the converted token substring to insert
     * @param span int array with <tt>from,to</tt> values of the match span
     * @return the string with the converted token inserted.
     */
    private String insertIntoResult(String convertedToken, int[] span) {

        String result = null;
        String formatStringMatch = matchedFormatString;

        if (!head.isEmpty() && formatStringMatch.indexOf(head) == -1) {
            formatStringMatch = head + formatStringMatch;
        }
        if (!tail.isEmpty() && formatStringMatch.lastIndexOf(tail) == -1) {
            formatStringMatch += tail;
        }

        final int from = span[1];
        final int to = span[0];
        final int len = formatStringMatch.length();

        // Prepare a solution string from the whole match earlier
        final String beginning = formatStringMatch.substring(0, to);
        final String end = formatStringMatch.substring(from, len);

        // Keep track of how many args we have consumed. but only
        // decrement if the specifier actually is positional
        if (Pattern.matches("\\{[0-9]{1,}.*?\\}", convertedToken)) {
            argIndex--;
        }

        result = beginning + convertedToken + end;
        length = result.length();

        return result;
    }

    /**
     * Get the next argument index. Used when determining the 
     * index of positional specifiers in the format string.
     * <p>This method is not intended to be called by
     * clients of <tt>PercentToBraceConverter</tt>. Instead, 
     * It is called by {@link #PercentConversion}.</p>
     * <p>Note that the underlying implementation of this method
     * may change in the future. This will be reflected then in
     * the <tt>PercentConversion</tt>.</p>
     * @return the argument index as string
     */
    private String nextIndex() {
        final String result = String.format("%d", argIndex);
        argIndex++;
        return result;
    }

    /**
     * <p>Replace the interpolant term with a <tt>.format(…)</tt> call.</p>
     * 
     * <p>E.g. replaces <tt>"Hi, my name is %s" % (person_name)</tt> with
     * <tt>"Hi, my name is %s".format(person_name)</tt></p>
     * 
     * @param formatStringMatcher the format string matcher for extracting 
     * the needed groups
     * @return string with replaced substring or unmodified string
     * @note if the interpolation values match group includes a previously identified tail
     * it will be chopped off here as a fix. Inclusion of the tail may happen because
     * the {@link #FMTSTR_PATTERN} can't be made unambiguous enough.
     */
    private String replaceInterpolantTokenWithFormatCall(Matcher formatStringMatcher) {

        String result = initialSourceString;

        final Map<String, String> groups = extractFormatStringGroups(formatStringMatcher.toMatchResult());

        final String fmtStr = groups.get("FormatString");
        final String interpToken = groups.get("InterpolantToken");
        final String interpValues = groups.get("InterpolationValues");

        if (null != fmtStr &&
                null != interpToken &&
                null != interpValues) {
            // When replacing the interpolation token with a .format() call
            // one needs to be careful not to turn a tuple of interpolation values
            // into a tuple of a tuple of interpolation values, because when
            // there's multiple format specifiers in the format string, the
            // tuple will be inserted for the first positional specifier and
            // the second specifier will not have any values left to being 
            // interpolated with, resulting in a runtime error.
            String s = "(";
            String e = ")";
            if ('(' == interpValues.charAt(0)) {
                s = "";
                e = "";
            }
            // fix falsely included tail in interpolation values
            if (!tail.isEmpty()) {
                int index = interpValues.indexOf(tail);
                if (index > 0) { // yes, '>' not '>=', since substring(0,0) doesn't make sense
                    result = String.format("%s%s%s%s%s", fmtStr, ".format", s, interpValues.substring(0, index), e);
                }
            } else {
                result = String.format("%s%s%s%s%s", fmtStr, ".format", s, interpValues, e);
            }

        }

        return result;
    }

    /**
     * <p>Store the left and right substring parts of the initial source string
     * that are not matched by {@link #FMTSTR_PATTERN} so they
     * can be re-attached at the end of processing.</p>
     * 
     * <p>Normally we would expect to be only passed complete and compact format 
     * strings. Unfortunately it can't be assumed this will always be the case.</p>
     * 
     * <p>E.g. we expect this:<br>
     * <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<tt>"Hello, my name is %s" % (personName)</tt>
     * </p>
     * 
     * <p>However we also need to handle cases like this:<br>
     * <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;<tt>s = "Hello, my name is %s" % (personName)  
     * # this is a comment</tt>
     * </p>
     * 
     * <p>The latter case also has a head (the variable assignment) and a tail (the comment part).
     * This method then chops off head and tail and stores them for later.</p>
     * 
     */
    private void storeHeadAndTail() {
        final String initial = initialSourceString;
        final String matched = matchedFormatString;

        if (!initial.equals(matched)) {
            final int to = initial.indexOf(matched);
            final int from = matched.length();
            final int initlen = initial.length();
            head = initial.substring(0, to);
            tail = initial.substring(from + to, initlen);
            if (tail.isEmpty()) {
                // As a last effort, try to find the tail because the FMTSTR_PATTERN matcher may have 
                // failed in the face of ambiguity.
                //
                // Ambiguity may arise because the interpolant values group can be an identifier (e.g. 
                // a variable), a literal, or a tuple of identifiers or literals. 
                // Take for example the string literal, which itself can contain an arbitrary mix of 
                // brace characters, hash characters (which would otherwise indicate an end-of-line 
                // comment), or escaped versions thereof. 
                // The best way to combat this ambiguity I could find was to employ a statemachine 
                // variation, that works its way inwards from both sides, to the most probable location
                // that marks the true end of the interpolation values group.
                //
                String valuesPart = PyStringUtils.findSubstring(matched, '%', true);

                int lastBracePos = StringUtils.lastIndexOf(valuesPart, "\\)");
                int lastDoubleQuotePos = StringUtils.lastIndexOf(valuesPart, "\\\"");
                int lastSingleQuotePos = StringUtils.lastIndexOf(valuesPart, "'");
                int hashPos = PyStringUtils.indexOf(valuesPart, '#', true);
                int lastAlnumPos = StringUtils.lastIndexOf(valuesPart, "[a-zA-Z0-9_]");

                if (hashPos == -1) {
                    // has no comment tail, set to max length
                    hashPos = from;
                }

                int[] positions = { hashPos, lastBracePos, lastDoubleQuotePos, lastSingleQuotePos, lastAlnumPos };

                int min = lastBracePos;
                int max = 0;
                for (int i = 0; i < positions.length; i++) {
                    int cur = positions[i];
                    if (cur == -1) {
                        continue;
                    }
                    if (cur < hashPos) {
                        if (cur < min) {
                            min = cur;
                        }
                        if (cur > max) {
                            max = cur;
                        }
                    }
                }
                tail = valuesPart.substring(max + 1);
            }
        } else {
            head = "";
            tail = "";
        }
    }

    /**
     * Update the strings with a new result.
     * @param newResult the new value
     */
    private void updateStrings(final String newResult) throws IllegalArgumentException {
        Assert.isNotNull(newResult, "E: newResult can't be null!");
        String processedNewResult = new String(newResult);
        initialSourceString = processedNewResult;
        matchedFormatString = processedNewResult;
        length = processedNewResult.length();
    }

    /**
     * <p>The purpose of the <tt>PercentConversion</tt> class is two-fold:</p>
     * 
     * <ol>
     *   <li>it identifies and stores all the combined parts that make up 
     *       <b>one</b> complete specifier token in a format string.</li>
     *   <li>
     *       it splits up and converts each complete specifier token into  
     *       the brace notation used by the format mini-language, e.g. from
     *       <tt>%0.2f</tt> to <tt>{0:>0.2f}</tt>.
     *   </li>
     * </ol>
     *  
     * <p>It's only client is the outer class {@link #PercentToBraceConverter}.</p>
     * 
     * @see {@link PercentToBraceConverter#convert()}.
     * @author André Berg
     * @version 0.5
     */
    private static final class PercentConversion {

        // for the pattern below <num>: gives the matched group number

        /**
         * <p>Pattern for matching a Python format specifier.<br>
         * Matches, i.e.: <code>%2.2f</code>, <code>%(mapping)s</code>, 
         * <code>%s</code>, etc.</p>
         * 
         * <p>Groups:</p>
         * 
         * <ol>
         *  <li>mapping key, e.g. <code>mapping</code> (optional)</li>
         *  <li>conversion flags, e.g. <code>#,+,0</code> etc. (optional)</li>
         *  <li>minimum width, e.g. <code>*,2</code> (optional)</li>
         *  <li>precision, e.g. <code>.*,.2</code> (optional)</li>
         *  <li>length modifier, e.g. <code>h,l,L</code> (optional)</li>
         *  <li>conversion, e.g. <code>d,i,o,u,x,X,e,E,f,F,g,G,c,r,s,%</code></li>
         * </ol>
         */
        private static final Pattern TOKEN_PATTERN = Pattern.compile(
                "(?<!%)%" + // specifier start                
                        "(?:" + //                                
                        "\\(([^\\)]+)\\)" + // 1: mapping key (optional)      
                        ")?" + //                                
                        "([#+ -]{1,})?" + // 2: conversion flags (optional) 
                        "(" + // 3: minimum width (optional)    
                        "(?:\\*|(?:[0-9][0-9]*?))" + //                                
                        ")?" + //                                
                        "(?:" + //                                
                        "\\.((?:\\*|(?:[0-9][0-9]*?)))?" + // 4: precision (optional)        
                        ")?" + //                                
                        "([hlL])?" + // 5: length modifier (optional)  
                        "(?<!\\s)([diouxXeEfFgGcrs%])" // 6: conversion                  
                );

        private final int[] span;
        private final String source;
        private final String key;
        private final String width;
        private final String precision;
        private final String flags;
        private final String conversion;

        /**
         * <p>Create a new {@link #PercentConversion} instance.</p>
         * 
         * <p>A <tt>PercentConversion</tt> instance is created from one
         * particular specifier match result and is fixed after creation.</p>
         * 
         * This is because for some format strings, it is expected that 
         * multiple <tt>PercentConversions</tt> will be needed to fully convert
         * the format string and each <tt>PercentConversion</tt> should represent
         * one specifier and one specifier only in the format string.
         * 
         * @param aConverter - the enclosing {@link #PercentToBraceConverter} instance
         * @param aMatch - a specific {@link java.util.regex#MatchResult MatchResult} that holds 
         *                 information about the matched specifier token.
         * @throws IllegalArgumentException
         *          if <tt>aConverter</tt> or <tt>aMatch</tt> is <tt>null</tt>
         *          
         * @throws IllegalStateException 
         *          if <tt>aMatch</tt> is passed before a successful match could be made
         *          it is said to have inconsistent state.
         */
        public PercentConversion(PercentToBraceConverter aConverter, MatchResult aMatch)
                throws IllegalArgumentException, IllegalStateException {

            if (null == aConverter) {
                throw new IllegalArgumentException("Converter can't be null!");
            }
            if (null == aMatch) {
                throw new IllegalArgumentException("Match can't be null!");
            }

            source = aMatch.group(0);
            span = new int[] { aMatch.start(), aMatch.end() };

            final Map<String, String> groups = extractTokenGroups(aMatch);

            String spec = groups.get("Key");
            if (null == spec) {
                if ("%%".equals(source)) {
                    key = "";
                } else {
                    key = aConverter.nextIndex();
                }
            } else {
                key = spec;
            }

            spec = groups.get("Width");
            if (null != spec && "*".equals(spec)) {
                // TODO: {} representation is hard-wired, could generalize this if needed
                width = String.format("{%s}", aConverter.nextIndex());
            } else {
                width = spec;
            }

            spec = groups.get("Precision");
            if (null != spec && "*".equals(spec)) {
                precision = String.format("{%s}", aConverter.nextIndex());
            } else {
                precision = spec;
            }

            flags = groups.get("Flags");
            conversion = groups.get("Conversion");
        }

        /**
         * Two <tt>PercentConversion</tt> instances are equal if 
         * their <tt>source</tt> and <tt>span</tt> are equal.
         * 
         * @param obj the object to compare to
         * @return <tt>true</tt> if both objects are <i>field equal</i>, 
         * <tt>false</tt> otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (null == obj) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PercentConversion)) {
                return false;
            }
            PercentConversion other = (PercentConversion) obj;
            if (!((span[0] == other.span[0]) && (span[1] == other.span[1]))) {
                return false;
            }
            if (!source.equals(other.source)) {
                return false;
            }
            return true;
        }

        /**
         * Returns the combination of this <tt>PercentConversion</tt>'s <tt>span</tt>
         * plus the hash code of the <tt>source</tt> which makes the hash code of two
         * <tt>PercentConversions</tt> identical if their <tt>source</tt>, as well as 
         * the span in the surrounding text (the <i>context</i>) the conversion applies 
         * to, are equal.
         * 
         * @return hash code
         */
        @Override
        public int hashCode() {
            return (span[0] + span[1]) + source.hashCode();
        }

        @Override
        public String toString() {
            final String description =
                    MessageFormat.format("<{0}@0x{1} | source={2} span=[{3}:{4}]>",
                            this.getClass().getSimpleName(), Integer.toHexString(this.hashCode()),
                            source, span[0], span[1]);
            return description;
        }

        /**
         * Perform conversion to format-style brace syntax on a per-specifier or per-token basis.
         * @return the converted string or <tt>null</tt>.
         */
        public String toBrace() {

            String result = null;

            String conversion = this.conversion;
            String key = "";
            String flags = "";

            if ("%".equals(conversion)) {
                return source;
            } else {
                final boolean isNumber = Pattern.compile("\\d+").matcher(this.key).matches();
                if (isNumber) {
                    key = this.key;
                } else {
                    key = String.format("[%s]", this.key);
                }
            }

            if (null != this.flags) {
                flags = this.flags;
            }

            String align = "";
            if (null == width) {
                align = "";
            } else if (flags.indexOf('-') > -1) {
                align = "<";
            } else if (flags.indexOf('0') > -1 &&
                    "diouxXbB".indexOf(conversion) > -1) {
                align = "=";
            } else {
                align = ">";
            }

            String sign = "";
            String fill = "";
            String alt = "";

            if (null != flags && flags.length() > 0) {
                if (flags.indexOf('+') > -1) {
                    sign = "+";
                } else if (flags.indexOf(' ') > -1) {
                    sign = " ";
                }
                if (flags.indexOf('0') > -1 &&
                        flags.indexOf('-') == -1 &&
                        "crs".indexOf(conversion) == -1) {
                    fill = "0";
                }
                if (flags.indexOf('#') > -1 &&
                        "diuxXbB".indexOf(conversion) > -1) {
                    alt = "#";
                }
            }

            String transform = "";
            if ("iu".indexOf(conversion) > -1) {
                conversion = "d";
            } else if ("rs".indexOf(conversion) > -1) {
                // %s is interpreted as calling str() on the operand, so
                // we specify !s in the {}-format. If we don't do this, then
                // we can't convert the case where %s is used to print e.g. integers
                // or floats.
                transform = "!" + conversion;
                conversion = "";
            }

            final String prefix = String.format("%s%s", key, transform);
            String suffix = String.format("%s%s%s%s", fill, align, sign, alt);

            if (null != width) {
                suffix += width;
            }
            if (null != precision) {
                suffix += "." + precision;
            }
            suffix += conversion;

            result = prefix;
            if (!suffix.isEmpty()) {
                result += ":" + suffix;
            }

            result = String.format("{%s}", result);

            return result;
        }

        /**
         * Return the source string the PercentConversion was initialized with.
         * @return the string representing the complete specifier token, e.g. <tt>%0.2f</tt>.
         */
        @SuppressWarnings("unused")
        private String getSource() {
            return source;
        }

        private int[] getSpan() {
            return span;
        }

        /**
         * Build a group dictionary from the matched groups. <br>
         * Guaranteed to have 6 keys:<br>
         * <ol>
         *   <li><tt>Key</tt>: a key mapping (str) as used for dict interpolation, or a positional index (num as str)</li>
         *   <li><tt>Flags</tt>: notational flags (e.g. <tt>"#,0,+,-,<i>&lt;space&gt;</i>)</tt></li>
         *   <li><tt>Width</tt>: minimum width, e.g. <code>*,2</code> (optional)</li>
         *   <li><tt>Precision</tt>: precision specifier, e.g. <code>.*,.2</code> (optional)</li>
         *   <li><tt>Length</tt>: length modifier, e.g. <code>h,l,L</code> (optional)</li>
         *   <li><tt>Conversion</tt>: conversion specifier <code>d,i,o,u,x,X,e,E,f,F,g,G,c,r,s,%</code></li>
         * </ol>
         * @param matchResult
         * @return the K, V mapped groups.
         * @see {@link #TOKEN_PATTERN}
         */
        private static Map<String, String> extractTokenGroups(final MatchResult matchResult) {

            Assert.isLegal(6 == matchResult.groupCount(),
                    "E: match result from TOKEN_PATTERN is malformed. Group count must be 6.");

            final Map<String, String> result = new HashMap<String, String>(6);

            final String key = matchResult.group(1);
            final String flags = matchResult.group(2);
            final String width = matchResult.group(3);
            final String precision = matchResult.group(4);
            final String length = matchResult.group(5);
            final String conversion = matchResult.group(6);

            result.put("Key", key);
            result.put("Flags", flags);
            result.put("Width", width);
            result.put("Precision", precision);
            result.put("Length", length);
            result.put("Conversion", conversion);

            return result;
        }

        /**
         * @return the Pattern to match Python format specifier tokens.
         * @see {@link #TOKEN_PATTERN}
         * @see {@link #extractTokenGroups}
         */
        private static Pattern getTokenPattern() {
            return TOKEN_PATTERN;
        }
    }
}
