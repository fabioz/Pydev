/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

public class LinePartIndentMark extends AbstractLinePart implements ILinePartIndentMark {

    private boolean isIndent;
    private boolean requireNewLine = false;

    public LinePartIndentMark(int beginCol, Object token, boolean isIndent, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.isIndent = isIndent;
    }

    @Override
    public String toString() {
        return isIndent ? "INDENT" : "DEDENT";
    }

    public void setRequireNewLine(boolean requireNewLine) {
        this.requireNewLine = requireNewLine;
    }

    @Override
    public boolean getRequireNewLineOnIndent() {
        return this.requireNewLine;
    }

    @Override
    public boolean isIndent() {
        return this.isIndent;
    }

}
