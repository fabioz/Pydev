/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import java.util.LinkedList;
import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern DOT = Pattern.compile("\\.");
    private static final Pattern SLASH = Pattern.compile("\\/");

    private StringUtils() {
    }

    /**
     * Joins the supplied parts together, using dots as a glue
     * 
     * @param parts parts to join
     * @return string containing joined parts 
     */
    public static String join(char delimiter, Iterable parts) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (Object part : parts) {

            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(part);
        }

        return sb.toString();
    }

    /**
     * Returns the string with the first character in upper case.
     * 
     * @param string "example string"
     * @return "Example string"
     */
    public static String capitalize(String string) {
        if (string.length() == 0) {
            return string;
        } else {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

    /**
     * Takes the '/' separated string and removes level parts of it
     * 
     * Example:
     * input is foo/bar/baz
     * 
     * with level 1: "foo/bar"
     * with level 2: "foo"
     * with level 3  ""
     * 
     * @param string
     * @param level level
     * @return truncated string
     */
    public static String stripParts(String string, int level) {
        /* level times remove /.*$ */
        for (int i = 0; i < level; i++) {
            int index = string.lastIndexOf('/');

            if (index < 0) {
                /* no slashhes.. very well */
                return "";
            }

            string = string.substring(0, index);
        }
        return string;
    }

    public static LinkedList<String> dotSplitter(String string) {
        return splitter(string, DOT);
    }

    public static LinkedList<String> slashSplitter(String string) {
        return splitter(string, SLASH);
    }

    public static LinkedList<String> splitter(String string, Pattern re) {
        LinkedList<String> parts = new LinkedList<String>();
        for (String part : re.split(string)) {
            parts.add(part);
        }
        return parts;
    }

}
