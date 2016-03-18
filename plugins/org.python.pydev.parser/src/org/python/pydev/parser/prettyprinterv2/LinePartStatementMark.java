/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

public class LinePartStatementMark extends AbstractLinePart implements ILinePartStatementMark {

    private boolean isStart;

    public LinePartStatementMark(int beginCol, Object token, boolean isStart, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.isStart = isStart;
    }

    @Override
    public boolean isStart() {
        return isStart;
    }

    @Override
    public String toString() {
        return isStart ? "START_STMT" : "END_STMT";
    }
}
