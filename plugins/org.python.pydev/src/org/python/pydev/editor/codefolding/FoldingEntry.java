/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/**
 * @author fabioz
 */
package org.python.pydev.editor.codefolding;

import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class FoldingEntry {

    public FoldingEntry(int type, int startLine, int endLine, ASTEntry astEntry, boolean isCollapsed) {
        if (endLine < startLine) {
            endLine = startLine;
        }
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
        this.astEntry = astEntry;
        this.isCollapsed = isCollapsed;
    }

    public FoldingEntry(int type, int startLine, int endLine, ASTEntry astEntry)
    {
        this(type, startLine, endLine, astEntry, false);
    }

    public final static int TYPE_IMPORT = 1;
    public final static int TYPE_DEF = 2;
    public final static int TYPE_COMMENT = 3;
    public final static int TYPE_STR = 4;
    public final static int TYPE_STATEMENT = 5;
    public final static int TYPE_ELSE = 6;
    public final static int TYPE_EXCEPT = 7;
    public final static int TYPE_FINALLY = 8;
    public int type;
    public int startLine;
    public int endLine;
    public ASTEntry astEntry;
    public boolean isCollapsed;

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + endLine;
        result = PRIME * result + startLine;
        result = PRIME * result + type;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FoldingEntry other = (FoldingEntry) obj;
        if (endLine != other.endLine)
            return false;
        if (startLine != other.startLine)
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("FoldingEntry<");
        buf.append("type:");
        buf.append(type);
        buf.append(" startLine:");
        buf.append(startLine);
        buf.append(" endLine:");
        buf.append(endLine);
        buf.append(">");
        return buf.toString();
    }

    public ASTEntry getAstEntry() {
        return astEntry;
    }
}