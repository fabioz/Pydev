/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * A string pattern matcher supporting * and ?
 */
public class StringMatcherWithIndexSemantics {

    private final Pattern compiled;
    private boolean startsWithWildCard;
    private boolean endsWithWildCard;

    public StringMatcherWithIndexSemantics(String text, boolean ignoreCase, boolean wholeWord) {
        FastStringBuffer buf = new FastStringBuffer();
        FastStringBuffer finalRegexp = new FastStringBuffer();

        boolean skipLeftSep = false;
        boolean skipRightSep = false;

        if (!wholeWord) {
            skipLeftSep = true;
        }
        while (text.startsWith("*")) {
            skipLeftSep = true;
            text = text.substring(1);
        }

        if (!wholeWord) {
            skipRightSep = true;
        }
        while (text.endsWith("*") && !text.endsWith("\\*")) {
            skipRightSep = true;
            text = text.substring(0, text.length() - 1);
        }

        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                if (i < length) {
                    //Will be quoted
                    buf.append(text.charAt(i));
                }
                continue;
            }
            if (c == '*' || c == '?') {
                if (buf.length() > 0) {
                    finalRegexp.append(Pattern.quote(buf.toString()));
                    buf.clear();
                }
                finalRegexp.append(".").append(c);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            finalRegexp.append(Pattern.quote(buf.toString()));
        }

        if (!skipLeftSep) {
            if (!finalRegexp.startsWith('*')) {
                if (!finalRegexp.startsWith("\\Q")) {
                    finalRegexp.insert(0, "\\b");
                } else {
                    if (Character.isJavaIdentifierPart(finalRegexp.charAt(2))) {
                        finalRegexp.insert(0, "\\b");
                    }
                }
            }
        }

        if (!skipRightSep) {
            if (!finalRegexp.endsWith('*')) {
                if (!finalRegexp.endsWith("\\E")) {
                    finalRegexp.append("\\b");
                } else {
                    if (Character.isJavaIdentifierPart(finalRegexp.charAt(finalRegexp.length() - 3))) {
                        finalRegexp.append("\\b");
                    }
                }
            }
        }

        this.startsWithWildCard = skipLeftSep;
        this.endsWithWildCard = skipRightSep;

        compiled = Pattern.compile(finalRegexp.toString(), ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
    }

    public static class Position {
        public int start; //inclusive

        public int end; //exclusive

        public Position(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public Position find(String text, int start) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        if (start < 0) {
            start = 0;
        }
        Matcher matcher = compiled.matcher(text);
        boolean find = matcher.find(start);
        if (!find) {
            return null;
        } else {
            int startPos = matcher.start();
            int endPos = matcher.end();
            return new Position(startPos, endPos);
        }
    }

    public boolean match(String text) {
        Matcher matcher = compiled.matcher(text);
        if (!startsWithWildCard && !endsWithWildCard) {
            return matcher.matches();
        } else {
            Position found = this.find(text, 0);
            if (found == null) {
                return false;
            }
            if (!startsWithWildCard && found.start != 0) {
                return false;
            }
            if (!endsWithWildCard && found.end != text.length()) {
                return false;
            }
            return true;
        }
    }
}
