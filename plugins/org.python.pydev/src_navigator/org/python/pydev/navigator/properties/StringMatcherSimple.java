package org.python.pydev.navigator.properties;

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.ArrayList;

/**
 * A string pattern matcher, supporting "*" and "?" wild cards.
 * 
 * @since 3.2
 */
public class StringMatcherSimple {
    private static final char SINGLE_WILD_CARD = '\u0000';

    /**
     * Boundary value beyond which we don't need to search in the text 
     */
    private int bound = 0;

    private boolean hasLeadingStar;

    private boolean hasTrailingStar;

    private final String pattern;

    private final int patternLength;

    /**
     * The pattern split into segments separated by *
     */
    private String segments[];

    /**
     * StringMatcherSimple constructor takes in a String object that is a simple 
     * pattern which may contain '*' for 0 and many characters and
     * '?' for exactly one character.  
     *
     * Literal '*' and '?' characters must be escaped in the pattern 
     * e.g., "\*" means literal "*", etc.
     *
     * Escaping any other character (including the escape character itself), 
     * just results in that character in the pattern.
     * e.g., "\a" means "a" and "\\" means "\"
     *
     * If invoking the StringMatcherSimple with string literals in Java, don't forget
     * escape characters are represented by "\\".
     *
     * @param pattern the pattern to match text against
     */
    public StringMatcherSimple(String pattern) {
        if (pattern == null)
            throw new IllegalArgumentException();
        this.pattern = pattern;
        patternLength = pattern.length();
        parseWildCards();
    }

    /** 
     * @param text a simple regular expression that may only contain '?'(s)
     * @param start the starting index in the text for search, inclusive
     * @param end the stopping point of search, exclusive
     * @param p a simple regular expression that may contain '?'
     * @return the starting index in the text of the pattern , or -1 if not found 
     */
    private int findPosition(String text, int start, int end, String p) {
        boolean hasWildCard = p.indexOf(SINGLE_WILD_CARD) >= 0;
        int plen = p.length();
        for (int i = start, max = end - plen; i <= max; ++i) {
            if (hasWildCard) {
                if (regExpRegionMatches(text, i, p, 0, plen))
                    return i;
            } else {
                if (text.regionMatches(true, i, p, 0, plen))
                    return i;
            }
        }
        return -1;
    }

    /**
     * Given the starting (inclusive) and the ending (exclusive) positions in the   
     * <code>text</code>, determine if the given substring matches with aPattern  
     * @return true if the specified portion of the text matches the pattern
     * @param text a String object that contains the substring to match 
     */
    public boolean match(String text) {
        if (text == null)
            return false;
        final int end = text.length();
        final int segmentCount = segments.length;
        if (segmentCount == 0 && (hasLeadingStar || hasTrailingStar)) // pattern contains only '*'(s)
            return true;
        if (end == 0)
            return patternLength == 0;
        if (patternLength == 0)
            return false;
        int currentTextPosition = 0;
        if ((end - bound) < 0)
            return false;
        int segmentIndex = 0;
        String current = segments[segmentIndex];

        /* process first segment */
        if (!hasLeadingStar) {
            int currentLength = current.length();
            if (!regExpRegionMatches(text, 0, current, 0, currentLength))
                return false;
            segmentIndex++;
            currentTextPosition = currentTextPosition + currentLength;
        }
        if ((segmentCount == 1) && (!hasLeadingStar) && (!hasTrailingStar)) {
            // only one segment to match, no wild cards specified
            return currentTextPosition == end;
        }
        /* process middle segments */
        while (segmentIndex < segmentCount) {
            current = segments[segmentIndex];
            int currentMatch = findPosition(text, currentTextPosition, end, current);
            if (currentMatch < 0)
                return false;
            currentTextPosition = currentMatch + current.length();
            segmentIndex++;
        }

        /* process final segment */
        if (!hasTrailingStar && currentTextPosition != end) {
            int currentLength = current.length();
            return regExpRegionMatches(text, end - currentLength, current, 0, currentLength);
        }
        return segmentIndex == segmentCount;
    }

    /**
     * Parses the pattern into segments separated by wildcard '*' characters.
     */
    private void parseWildCards() {
        if (pattern.startsWith("*"))//$NON-NLS-1$
            hasLeadingStar = true;
        if (pattern.endsWith("*")) {//$NON-NLS-1$
            /* make sure it's not an escaped wildcard */
            if (patternLength > 1 && pattern.charAt(patternLength - 2) != '\\') {
                hasTrailingStar = true;
            }
        }

        ArrayList<String> temp = new ArrayList<String>();

        int pos = 0;
        StringBuffer buf = new StringBuffer();
        while (pos < patternLength) {
            char c = pattern.charAt(pos++);
            switch (c) {
                case '\\':
                    if (pos >= patternLength) {
                        buf.append(c);
                    } else {
                        char next = pattern.charAt(pos++);
                        /* if it's an escape sequence */
                        if (next == '*' || next == '?' || next == '\\') {
                            buf.append(next);
                        } else {
                            /* not an escape sequence, just insert literally */
                            buf.append(c);
                            buf.append(next);
                        }
                    }
                    break;
                case '*':
                    if (buf.length() > 0) {
                        /* new segment */
                        temp.add(buf.toString());
                        bound += buf.length();
                        buf.setLength(0);
                    }
                    break;
                case '?':
                    /* append special character representing single match wildcard */
                    buf.append(SINGLE_WILD_CARD);
                    break;
                default:
                    buf.append(c);
            }
        }

        /* add last buffer to segment list */
        if (buf.length() > 0) {
            temp.add(buf.toString());
            bound += buf.length();
        }
        segments = temp.toArray(new String[temp.size()]);
    }

    /**
     * 
     * @return boolean
     * @param text a String to match
     * @param tStart the starting index of match, inclusive
     * @param p a simple regular expression that may contain '?'
     * @param pStart The start position in the pattern
     * @param plen The length of the pattern
     */
    private boolean regExpRegionMatches(String text, int tStart, String p, int pStart, int plen) {
        while (plen-- > 0) {
            char tchar = text.charAt(tStart++);
            char pchar = p.charAt(pStart++);

            // process wild cards, skipping single wild cards
            if (pchar == SINGLE_WILD_CARD)
                continue;
            if (pchar == tchar)
                continue;
            if (Character.toUpperCase(tchar) == Character.toUpperCase(pchar))
                continue;
            // comparing after converting to upper case doesn't handle all cases;
            // also compare after converting to lower case
            if (Character.toLowerCase(tchar) == Character.toLowerCase(pchar))
                continue;
            return false;
        }
        return true;
    }
}