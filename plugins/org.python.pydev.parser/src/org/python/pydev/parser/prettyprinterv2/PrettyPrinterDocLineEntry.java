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
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Defines a line in the document. The items are added based on the order in which items are added
 * and their actual columns.
 * 
 * So, if 2 items are added to the same column, the one added 1st has precedence over the other.
 *
 */
public class PrettyPrinterDocLineEntry {

    /**
     * Holds the line parts available.
     */
    private ArrayList<ILinePart> lineParts = new ArrayList<ILinePart>();

    /**
     * The difference from indents/dedents in the current line (e.g.: if there are 2 indents
     * and 1 dedent in this line, indentDiff = 1)
     */
    private int indentDiff;

    /**
     * The number of empty lines required after a dedent in this line.
     */
    private int emptyLinesRequiredAfterDedent;

    /**
     * The number of this line in the document.
     */
    public final int line;

    /**
     * Marks if the line is currently sorted or not. Whenever the line is changed, this
     * attribute has to be marked as false.
     */
    private boolean lineSorted = false;

    public PrettyPrinterDocLineEntry(int line) {
        this.line = line;
    }

    private void addPart(ILinePart linePart) {
        int before = -1;
        if (linePart instanceof LinePartRequireMark) {
            String token = ((LinePartRequireMark) linePart).getToken().trim();
            if (token.equals(":") || token.equals(",") || token.equals("(") || token.equals(")") || token.equals("[")
                    || token.equals("]") || token.equals("=") || token.equals("{") || token.equals("}")) {
                if (lineParts.size() > 0) {
                    for (int i = lineParts.size() - 1; i >= 0; i--) {
                        ILinePart existing = lineParts.get(i);
                        if (existing instanceof LinePartStatementMark || existing.getToken() instanceof commentType) {
                            before = i;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        if (before != -1) {
            lineParts.add(before, linePart);
        } else {
            lineParts.add(linePart);
        }
        lineSorted = false;
    }

    private void addPart(int i, ILinePart linePart) {
        lineParts.add(i, linePart);
        lineSorted = false;
    }

    private void sortLineParts() {
        if (!lineSorted) {
            Collections.sort(lineParts, new Comparator<ILinePart>() {

                @Override
                public int compare(ILinePart o1, ILinePart o2) {
                    return (o1.getBeginCol() < o2.getBeginCol() ? -1 : (o1.getBeginCol() == o2.getBeginCol() ? 0 : 1));
                }
            });
            lineSorted = true;
        }
    }

    public ILinePart add(int beginCol, String string, Object token) {
        ILinePart linePart = new LinePart(beginCol, string, token, this);
        addPart(linePart);
        return linePart;
    }

    public ILinePart addBefore(int beginCol, String string, Object token) {
        ILinePart linePart = new LinePart(beginCol, string, token, this);

        //Now, on the start, we want to add it before any existing in the same column.
        for (int i = 0; i < this.lineParts.size(); i++) {
            if (beginCol == this.lineParts.get(i).getBeginCol()) {
                addPart(i, linePart);
                return linePart;
            }
        }
        addPart(linePart);
        return linePart;
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        for (ILinePart c : getSortedParts()) {
            if (c instanceof ILinePart2) {
                ILinePart2 iLinePart2 = (ILinePart2) c;
                buf.append(iLinePart2.getString());
            } else {
                buf.append(c.toString());
            }
            buf.append(c.isMarkedAsFound() ? "+" : "?");
            buf.append(" ");
        }
        return buf.toString();
    }

    public List<ILinePart> getSortedParts() {
        sortLineParts();
        return this.lineParts;
    }

    public void indent(SimpleNode node) {
        indent(node, false);
    }

    public LinePartIndentMark indent(SimpleNode node, boolean requireNewLine) {
        this.indentDiff += 1;
        LinePartIndentMark linePartIndentMark = new LinePartIndentMark(node.beginColumn, node, true, this);
        linePartIndentMark.setRequireNewLine(requireNewLine);
        addPart(linePartIndentMark);
        return linePartIndentMark;
    }

    public LinePartIndentMark dedent(int emptyLinesRequiredAfterDedent) {
        if (this.emptyLinesRequiredAfterDedent < emptyLinesRequiredAfterDedent) {
            this.emptyLinesRequiredAfterDedent = emptyLinesRequiredAfterDedent;
        }
        this.indentDiff -= 1;
        List<ILinePart> sortedParts = this.getSortedParts();
        LinePartIndentMark dedentMark;
        if (sortedParts.size() > 0) {
            dedentMark = new LinePartIndentMark(sortedParts.get(sortedParts.size() - 1).getBeginCol(), "", false, this);
            sortedParts.add(dedentMark);
        } else {
            dedentMark = new LinePartIndentMark(0, "", false, this);
            addPart(dedentMark);
        }
        return dedentMark;
    }

    public LinePartIndentMark indentAfter(ILinePart after, boolean requireNewLine) {
        this.indentDiff += 1;
        LinePartIndentMark linePartIndentMark = new LinePartIndentMark(after.getBeginCol(), after.getToken(), true,
                this);
        linePartIndentMark.setRequireNewLine(requireNewLine);
        addPart(lineParts.indexOf(after) + 1, linePartIndentMark);
        return linePartIndentMark;
    }

    public int getIndentDiff() {
        return this.indentDiff;
    }

    public int getFirstCol() {
        sortLineParts();
        if (this.lineParts.size() > 0) {
            ILinePart iLinePart0 = this.lineParts.get(0);
            if (!(iLinePart0 instanceof LinePartRequireAdded)) {
                return iLinePart0.getBeginCol();
            }
        }
        return -1;
    }

    public void addStartStatementMark(ILinePart foundWithLowerLocation, SimpleNode node) {
        sortLineParts();

        //Now, on the start, we want to add it before any existing in the same column.
        for (int i = 0; i < this.lineParts.size(); i++) {
            if (foundWithLowerLocation == this.lineParts.get(i)) {
                addPart(i, new LinePartStatementMark(foundWithLowerLocation.getBeginCol(), node, true, this));
                return;
            }
        }
        addPart(new LinePartStatementMark(foundWithLowerLocation.getBeginCol(), node, true, this));
    }

    public void addEndStatementMark(ILinePart foundWithHigherLocation, SimpleNode node) {
        addPart(new LinePartStatementMark(foundWithHigherLocation.getBeginCol(), node, false, this));
    }

    public int getNewLinesRequired() {
        return this.emptyLinesRequiredAfterDedent;
    }

    public LinePartRequireMark addRequireMark(int beginColumn, String string) {
        LinePartRequireMark mark = new LinePartRequireMark(beginColumn, string, this);
        addPart(mark);
        return mark;
    }

    public LinePartRequireMark addRequireMark(int beginColumn, String... string) {
        LinePartRequireMark mark = new LinePartRequireMark(beginColumn, this, string);
        addPart(mark);
        return mark;
    }

    public LinePartRequireIndentMark addRequireIndentMark(int beginColumn, String string) {
        LinePartRequireIndentMark ret = new LinePartRequireIndentMark(beginColumn, string, this);
        addPart(ret);
        return ret;
    }

    public LinePartRequireMark addRequireMarkBefore(ILinePart o1, String string) {
        LinePartRequireMark linePart = new LinePartRequireMark(o1.getBeginCol(), string, this);
        for (int i = 0; i < this.lineParts.size(); i++) {
            if (o1 == this.lineParts.get(i)) {
                addPart(i, linePart);
                return linePart;
            }
        }
        addPart(linePart);
        return linePart;
    }

    public LinePartRequireMark addRequireMarkAfterBefore(ILinePart o1, String string) {
        LinePartRequireMark linePart = new LinePartRequireMark(o1.getBeginCol(), string, this);
        for (int i = 0; i < this.lineParts.size(); i++) {
            if (o1 == this.lineParts.get(i)) {
                addPart(i + 1, linePart);
                return linePart;
            }
        }
        addPart(linePart);
        return linePart;
    }

}
