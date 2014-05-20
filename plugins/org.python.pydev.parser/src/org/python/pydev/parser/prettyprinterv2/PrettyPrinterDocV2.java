/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This document is the temporary structure we create to put on the tokens and the comments.
 * 
 * It's line oriented and we should fill it with all things in the proper place (and properly indented) so
 * that we can just make a simple print later on.
 */
public class PrettyPrinterDocV2 {

    /**
     * Holds the lines to print.
     */
    public final SortedMap<Integer, PrettyPrinterDocLineEntry> linesToColAndContents = new TreeMap<Integer, PrettyPrinterDocLineEntry>();

    private Map<Integer, List<ILinePart>> recordedChanges = new HashMap<Integer, List<ILinePart>>();

    private int lastRecordedChangesId = 0;

    public void addBefore(int beginLine, int beginCol, String string, Object token) {
        PrettyPrinterDocLineEntry lineContents = getLine(beginLine);
        ILinePart linePart = lineContents.addBefore(beginCol, string, token);
        addToCurrentRecordedChanges(linePart);
    }

    private void addToCurrentRecordedChanges(ILinePart linePart) {
        for (List<ILinePart> lst : recordedChanges.values()) {
            lst.add(linePart);
        }
    }

    public void add(int beginLine, int beginCol, String string, Object token) {
        PrettyPrinterDocLineEntry lineContents = getLine(beginLine);
        ILinePart linePart = lineContents.add(beginCol, string, token);
        addToCurrentRecordedChanges(linePart);
    }

    public LinePartRequireMark addRequireOneOf(SimpleNode node, String... requireOneOf) {
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        LinePartRequireMark linePart = line.addRequireMark(node.beginColumn, requireOneOf);
        addToCurrentRecordedChanges(linePart);
        return linePart;
    }

    public LinePartRequireMark addRequire(String string, SimpleNode node) {
        checkLine(node);
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        LinePartRequireMark linePart = line.addRequireMark(node.beginColumn, string);
        addToCurrentRecordedChanges(linePart);
        return linePart;
    }

    private void checkLine(SimpleNode node) {
        if (node.beginLine < 0 || node.beginColumn < 0) {
            throw new RuntimeException("Node: " + node + " has invalid line " + node.beginLine + " or col "
                    + node.beginColumn);
        }

    }

    public LinePartRequireMark addRequireBefore(String string, ILinePart o1) {
        PrettyPrinterDocLineEntry line = getLine(o1.getLine());
        LinePartRequireMark linePart = line.addRequireMarkBefore(o1, string);
        addToCurrentRecordedChanges(linePart);
        return linePart;
    }

    public LinePartRequireMark addRequireAfter(String string, ILinePart o1) {
        PrettyPrinterDocLineEntry line = getLine(o1.getLine());
        LinePartRequireMark linePart = line.addRequireMarkAfterBefore(o1, string);
        addToCurrentRecordedChanges(linePart);
        return linePart;
    }

    public LinePartRequireIndentMark addRequireIndent(String string, SimpleNode node) {
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        LinePartRequireIndentMark linePart = line.addRequireIndentMark(node.beginColumn, string);
        addToCurrentRecordedChanges(linePart);
        return linePart;
    }

    //---------------- Mark that a statement has started (new lines need a '\')

    public void addStartStatementMark(ILinePart foundWithLowerLocation, SimpleNode node) {
        getLine(foundWithLowerLocation.getLine()).addStartStatementMark(foundWithLowerLocation, node);
    }

    public void addEndStatementMark(ILinePart foundWithHigherLocation, SimpleNode node) {
        getLine(foundWithHigherLocation.getLine()).addEndStatementMark(foundWithHigherLocation, node);
    }

    //------------ Get information

    PrettyPrinterDocLineEntry getLine(int beginLine) {
        if (beginLine < 0) {
            throw new RuntimeException("Cannot get negative line.");
        }
        PrettyPrinterDocLineEntry lineContents = linesToColAndContents.get(beginLine);
        if (lineContents == null) {
            lineContents = new PrettyPrinterDocLineEntry(beginLine);
            linesToColAndContents.put(beginLine, lineContents);
        }
        return lineContents;
    }

    int getLastLineKey() {
        return linesToColAndContents.lastKey();
    }

    PrettyPrinterDocLineEntry getLastLine(boolean considerOnlyCommentOrEmptyLines) {
        Integer line = linesToColAndContents.lastKey();
        PrettyPrinterDocLineEntry last = null;
        if (line != null) {
            for (; line >= linesToColAndContents.firstKey(); line--) {
                PrettyPrinterDocLineEntry found = linesToColAndContents.get(line);
                if (found != null) {
                    last = found;
                    List<ILinePart> sortedParts = last.getSortedParts();
                    if (sortedParts.size() == 0) {
                        continue;
                    }
                    for (ILinePart iLinePart : sortedParts) {
                        if (!(iLinePart.getToken() instanceof commentType)) {
                            return last;
                        }
                    }
                }
            }
        }
        return last;
    }

    public ILinePart getLastPart() {
        PrettyPrinterDocLineEntry lastLine = getLastLine(true);
        java.util.List<ILinePart> sortedParts = lastLine.getSortedParts();
        return sortedParts.get(sortedParts.size() - 1);
    }

    //------------ Indentation
    public void addIndent(SimpleNode node) {
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        line.indent(node);
    }

    public LinePartIndentMark addIndent(SimpleNode node, boolean requireNewLine) {
        PrettyPrinterDocLineEntry line = getLine(node.beginLine);
        return line.indent(node, requireNewLine);
    }

    public LinePartIndentMark addDedent() {
        return addDedent(0);
    }

    public LinePartIndentMark addDedent(int emptyLinesRequiredAfterDedent) {
        PrettyPrinterDocLineEntry lastLine = getLastLine(false);
        return lastLine.dedent(emptyLinesRequiredAfterDedent);
    }

    //------------ toString

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("PrettyPrinterDocV2[\n");
        Set<Entry<Integer, PrettyPrinterDocLineEntry>> entrySet = linesToColAndContents.entrySet();
        for (Entry<Integer, PrettyPrinterDocLineEntry> entry : entrySet) {
            buf.append(entry.getKey() + ": " + entry.getValue() + "\n");
        }
        return "PrettyPrinterDocV2[" + buf + "]";
    }

    //------------ Changes Recording

    public int pushRecordChanges() {
        lastRecordedChangesId++;
        recordedChanges.put(lastRecordedChangesId, new ArrayList<ILinePart>());
        return lastRecordedChangesId;
    }

    /**
     * @return The line parts recorded. Guaranteed to be sorted by line/col.
     */
    public List<ILinePart> popRecordChanges(int id) {
        List<ILinePart> ret = recordedChanges.remove(id);
        Collections.sort(ret, new Comparator<ILinePart>() {

            @Override
            public int compare(ILinePart o1, ILinePart o2) {
                if (o1.getLine() < o2.getLine()) {
                    return -1;
                }
                if (o2.getLine() < o1.getLine()) {
                    return 1;
                }
                //same line
                if (o1.getBeginCol() < o2.getBeginCol()) {
                    return -1;
                }
                if (o2.getBeginCol() < o1.getBeginCol()) {
                    return 1;
                }
                return 0;
            }
        });
        return ret;
    }

    public int replaceRecorded(List<ILinePart> recordChanges, String... replacements) {
        int replaced = 0;
        Assert.isTrue(replacements.length % 2 == 0);
        for (ILinePart linePart : recordChanges) {
            if (linePart instanceof ILinePart2) {
                ILinePart2 iLinePart2 = (ILinePart2) linePart;
                for (int i = 0; i < replacements.length; i += 2) {
                    String toReplace = replacements[i];
                    String newToken = replacements[i + 1];
                    if (iLinePart2.getString().equals(toReplace)) {
                        iLinePart2.setString(newToken);
                        replaced += 1;
                    }
                }
            }
        }
        return replaced;
    }

    public Tuple<ILinePart, ILinePart> getLowerAndHigerFound(List<ILinePart> recordChanges) {
        return getLowerAndHigerFound(recordChanges, true);
    }

    public Tuple<ILinePart, ILinePart> getLowerAndHigerFound(List<ILinePart> recordChanges, boolean acceptToken) {
        Tuple<ILinePart, ILinePart> lowerAndHigher = null;
        ILinePart foundWithLowerLocation = null;
        ILinePart foundWithHigherLocation = null;

        for (ILinePart p : recordChanges) {
            if (p.getToken() instanceof commentType) {
                continue;
            }
            if (!acceptToken) {
                if (p.getToken() instanceof ISpecialStr) {
                    continue;
                }
            }
            if (foundWithHigherLocation == null) {
                foundWithHigherLocation = p;

            } else if (p.getLine() > foundWithHigherLocation.getLine()) {
                foundWithHigherLocation = p;

            } else if (p.getLine() == foundWithHigherLocation.getLine()
                    && p.getBeginCol() > foundWithHigherLocation.getBeginCol()) {
                foundWithHigherLocation = p;
            }

            if (foundWithLowerLocation == null) {
                foundWithLowerLocation = p;

            } else if (p.getLine() < foundWithLowerLocation.getLine()) {
                foundWithLowerLocation = p;

            } else if (p.getLine() == foundWithLowerLocation.getLine()
                    && p.getBeginCol() < foundWithLowerLocation.getBeginCol()) {
                foundWithLowerLocation = p;
            }
        }
        if (foundWithLowerLocation != null && foundWithHigherLocation != null) {
            lowerAndHigher = new Tuple<ILinePart, ILinePart>(foundWithLowerLocation, foundWithHigherLocation);
        }
        return lowerAndHigher;
    }

    /**
     * In this method, all the require marks have to be either already given in the parsing
     * (and removed from the line) or should be replaced by actual nodes.
     */
    public void validateRequireMarks() {
        if (linesToColAndContents.size() == 0) {
            return;//nothing to validate (no entries there)
        }
        Tuple<ILinePart, Boolean> search = null;

        for (int line = linesToColAndContents.firstKey(); line <= linesToColAndContents.lastKey(); line++) {
            PrettyPrinterDocLineEntry prettyPrinterDocLineEntry = linesToColAndContents.get(line);
            if (prettyPrinterDocLineEntry == null) {
                continue;
            }
            List<ILinePart> parts = prettyPrinterDocLineEntry.getSortedParts();
            for (int position = 0; position < parts.size(); position++) {
                ILinePart iLinePart = parts.get(position);
                if (iLinePart instanceof LinePartRequireMark) {
                    LinePartRequireMark linePartRequireMark = (LinePartRequireMark) iLinePart;

                    Tuple<ILinePart, Boolean> lastSearch = search;
                    //Ok, go forwards and see if we have a match somewhere
                    search = search(line, position, linePartRequireMark, false, lastSearch);
                    boolean found = search.o2;
                    if (!found) {
                        search = search(line, position, linePartRequireMark, true, lastSearch);
                        found = search.o2;
                    }

                    ILinePart next = search.o1;

                    if (!found) {
                        if (iLinePart instanceof LinePartRequireIndentMark) {
                            throw new RuntimeException("Unable to find place to add indent");
                        }
                        ILinePart removed = parts.remove(position);
                        LinePartRequireAdded linePartRequireAdded = new LinePartRequireAdded(removed.getBeginCol(),
                                linePartRequireMark.getToken(), linePartRequireMark.getToken(),
                                prettyPrinterDocLineEntry);

                        int addAt = position;
                        if (lastSearch != null) {
                            int i = parts.indexOf(lastSearch.o1);
                            if (i > -1 && position == i - 1) {
                                addAt = i + 1;
                            }
                        }
                        parts.add(addAt, linePartRequireAdded);
                        //Generate the search so that the last search is correct.
                        search = new Tuple<ILinePart, Boolean>(linePartRequireAdded, true);
                    } else {
                        if (iLinePart instanceof LinePartRequireIndentMark) {
                            //add the indent on the last position returned
                            PrettyPrinterDocLineEntry l = this.getLine(next.getLine());
                            l.indentAfter(next, true);
                        } else {
                            search.o1.setMarkAsFound();
                        }
                        int i = parts.indexOf(iLinePart);
                        parts.remove(i);
                        position--; //make up for the removed part
                    }
                }
            }
        }

    }

    private Tuple<ILinePart, Boolean> search(int line, int position, LinePartRequireMark linePartRequireMark,
            boolean forward, Tuple<ILinePart, Boolean> lastSearch) {
        boolean found = false;
        ILinePart next = null;
        LinePartsIterator it = getLinePartsIterator(line, position, forward, lastSearch);
        boolean searchForIndentMark = linePartRequireMark instanceof LinePartRequireIndentMark;

        OUTER: while (it.hasNext() && !found) {
            next = it.next();
            if (next instanceof ILinePart2) {
                if (!searchForIndentMark && next instanceof LinePartRequireAdded) {
                    break; //As the require parts are in order, finding a previously added require, we just mark it as not found.
                }
                if (!searchForIndentMark) {
                    if (next.isMarkedAsFound()) {
                        continue;
                    }
                }
                ILinePart2 part2 = (ILinePart2) next;
                if (linePartRequireMark.requireOneOf != null) {
                    for (String s : linePartRequireMark.requireOneOf) {
                        if (part2.getString().equals(s)) {
                            found = true;
                            break OUTER;
                        }
                    }
                } else {
                    if (part2.getString().equals(linePartRequireMark.getToken())) {
                        found = true;
                        break;
                    }
                }

                Object token = next.getToken();
                if (token instanceof SimpleNode && !(token instanceof commentType)) {
                    break; //didn't find it.
                }
            }
        }
        return new Tuple<ILinePart, Boolean>(next, found);
    }

    public LinePartsIterator getLinePartsIterator(int initialLine, int initialPos, boolean forward,
            Tuple<ILinePart, Boolean> lastSearch) {
        return new LinePartsIterator(this, initialLine, initialPos, forward, lastSearch);
    }

    public LinePartIndentMark getLastDedent() {
        for (int line = this.getLastLineKey(); line >= linesToColAndContents.firstKey(); line--) {
            PrettyPrinterDocLineEntry prettyPrinterDocLineEntry = linesToColAndContents.get(line);
            if (prettyPrinterDocLineEntry != null) {
                List<ILinePart> sortedParts = prettyPrinterDocLineEntry.getSortedParts();
                for (int i = sortedParts.size() - 1; i >= 0; i--) {
                    ILinePart iLinePart = sortedParts.get(i);
                    if (iLinePart instanceof LinePartIndentMark) {
                        LinePartIndentMark linePartIndentMark = (LinePartIndentMark) iLinePart;
                        if (!linePartIndentMark.isIndent()) {
                            return linePartIndentMark;
                        }
                    }
                }
            }
        }
        return null;
    }

}

/**
 * Helper class to iterate over the line parts in sequence (forward or backward) while traversing the lines.
 */
class LinePartsIterator implements Iterator<ILinePart> {

    private int line;
    private int position;

    List<ILinePart> currentPart;
    private PrettyPrinterDocV2 doc;
    private ILinePart next;
    private boolean forward;
    private Tuple<ILinePart, Boolean> lastSearch;

    public LinePartsIterator(PrettyPrinterDocV2 prettyPrinterDocV2, int initialLine, int initialPos, boolean forward,
            Tuple<ILinePart, Boolean> lastSearch) {
        this.doc = prettyPrinterDocV2;
        this.line = initialLine;
        this.position = initialPos;
        this.forward = forward;
        this.lastSearch = lastSearch;
        calcNext();
    }

    private void calcNext() {
        next = null;

        if (forward) {
            for (; line <= doc.linesToColAndContents.lastKey(); line++) {
                PrettyPrinterDocLineEntry prettyPrinterDocLineEntry = doc.linesToColAndContents.get(line);
                if (prettyPrinterDocLineEntry == null) {
                    continue;
                }
                List<ILinePart> parts = prettyPrinterDocLineEntry.getSortedParts();
                int onlyAfter = lastSearch != null ? parts.indexOf(lastSearch.o1) : -1;
                while (next == null) {
                    if (position < parts.size()) {
                        if (position < onlyAfter) {
                            position++;
                            continue;
                        }
                        next = parts.get(position);
                        position++;
                        return;
                    }
                    if (position >= parts.size()) {
                        position = 0;
                        break;
                    }
                }
            }
        } else {//backward
            for (; line >= doc.linesToColAndContents.firstKey(); line--) {
                PrettyPrinterDocLineEntry prettyPrinterDocLineEntry = doc.linesToColAndContents.get(line);
                if (prettyPrinterDocLineEntry == null) {
                    continue;
                }
                List<ILinePart> parts = prettyPrinterDocLineEntry.getSortedParts();
                int onlyAfter = lastSearch != null ? parts.indexOf(lastSearch.o1) : -1;
                while (next == null) {
                    if (position >= parts.size()) {
                        position = parts.size() - 1;
                    }
                    if (position < onlyAfter) {
                        return; //going backwards, when we reach the onlyAfter position, there's nowhere to go.
                    }
                    if (position >= 0) {
                        next = parts.get(position);
                        position--;
                        return;
                    }
                    if (position < 0) {
                        position = Integer.MAX_VALUE;
                        break;
                    }
                }
            }
        }

    }

    public void remove() {
        throw new RuntimeException("Not impl");
    }

    public ILinePart next() {
        ILinePart ret = next;
        calcNext();
        return ret;
    }

    public boolean hasNext() {
        return next != null;
    }

}