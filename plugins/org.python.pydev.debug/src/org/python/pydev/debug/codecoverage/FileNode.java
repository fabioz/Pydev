/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Fabio Zadrozny
 */
public class FileNode implements ICoverageLeafNode {

    public File node;
    public int stmts;
    public int miss;
    public String notExecuted;

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileNode)) {
            return false;
        }

        FileNode f = (FileNode) obj;
        return f.node.equals(node) && f.miss == miss && f.notExecuted.equals(notExecuted) && f.stmts == stmts;
    }

    @Override
    public int hashCode() {
        return node.hashCode() * 3 + ((miss + 1) * 7) + ((stmts + 1) * 5);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        FileNode.appendToBuffer(buf, node.toString(), stmts, miss, notExecuted,
                PyCoveragePreferences.getNameNumberOfColumns());
        return buf.toString();
    }

    public FastStringBuffer appendToBuffer(FastStringBuffer buffer, String baseLocation, int nameNumberOfColumns) {
        String name = node.toString();
        if (name.toLowerCase().startsWith(baseLocation.toLowerCase())) {
            name = name.substring(baseLocation.length());
        }
        if (name.startsWith("/") || name.startsWith("\\")) {
            name = name.substring(1);
        }
        if (name.length() == 0) {
            name = node.getName();
        }
        return appendToBuffer(buffer, name, stmts, miss, notExecuted, nameNumberOfColumns);
    }

    /**
     * @param buffer
     * @return
     */
    public static FastStringBuffer appendToBuffer(FastStringBuffer buffer, String str, int stmts, int miss,
            String notExecuted, int nameNumberOfColumns) {
        buffer.append(getName(str, nameNumberOfColumns)).append("   ").append(getStmts(stmts)).append("     ")
                .append(getStmts(miss)).append("      ").append(calcCover(stmts, miss)).append("  ")
                .append(notExecuted);
        return buffer;
    }

    public static String getName(String str, int nameNumberOfColumns) {
        FastStringBuffer buffer = new FastStringBuffer(str, str.length() > nameNumberOfColumns ? 0
                : nameNumberOfColumns - str.length());

        if (buffer.length() > nameNumberOfColumns) {
            buffer = buffer.delete(0, Math.abs((nameNumberOfColumns - 2) - str.length()));
            buffer.insert(0, "..");
        }
        if (buffer.length() < nameNumberOfColumns) {
            buffer.appendN(' ', nameNumberOfColumns - str.length());
        }
        return buffer.toString();
    }

    private static String getStmts(int stmts) {
        FastStringBuffer str = new FastStringBuffer();
        if (stmts == 0) {
            str.append('-');

        } else {
            str.append(stmts);
        }
        while (str.length() < 4) {
            str.insert(0, ' ');
        }
        return str.toString();
    }

    public static String calcCover(int stmts, int miss) {
        double v = 0;
        if (stmts > 0) {
            v = ((double) stmts - miss) / ((double) stmts) * 100.0;
        } else {
            return "   - ";
        }
        DecimalFormat format = new DecimalFormat("###.#");
        String str = format.format(v);
        str += "%";
        while (str.length() < 5) {
            str = " " + str;
        }
        return str;
    }

    /**
     * @return an iterator with the lines that were not executed
     */
    public Iterator<Tuple<Integer, Integer>> notExecutedIterator() {
        List<Tuple<Integer, Integer>> l = new ArrayList<Tuple<Integer, Integer>>();

        String[] toks = notExecuted.replaceAll(" ", "").split(",");
        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i].trim();
            if (tok.length() == 0) {
                continue;
            }
            if (tok.indexOf("-") == -1) {
                Integer startEnd = Integer.valueOf(tok);
                l.add(new Tuple<Integer, Integer>(startEnd, startEnd));
            } else {
                String[] begEnd = tok.split("-");
                l.add(new Tuple<Integer, Integer>(Integer.parseInt(begEnd[0]), Integer.parseInt(begEnd[1])));
            }
        }

        return l.iterator();
    }

}
