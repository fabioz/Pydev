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
package com.python.pydev.analysis.search_index;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * A string pattern matcher supporting * and ?
 */
public class StringMatcherWithIndexSemantics {

    private Pattern compiled;

    public StringMatcherWithIndexSemantics(String text, boolean ignoreCase) {
        FastStringBuffer buf = new FastStringBuffer();
        FastStringBuffer finalRegexp = new FastStringBuffer();

        boolean skipLeftSep = false;
        boolean skipRightSep = false;

        if (text.startsWith("*")) {
            skipLeftSep = true;
            text = text.substring(1);
        }
        if (text.endsWith("*") && !text.endsWith("\\*")) {
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

        compiled = Pattern.compile(finalRegexp.toString(), ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
    }

    public static class Position {
        int start; //inclusive

        int end; //exclusive

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
}
