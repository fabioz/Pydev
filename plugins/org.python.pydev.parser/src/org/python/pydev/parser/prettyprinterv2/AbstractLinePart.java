/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

/**
 * Base class for line parts.
 */
public abstract class AbstractLinePart implements ILinePart {

    protected Object token;
    private final int beginCol;
    private PrettyPrinterDocLineEntry lineEntry;
    private boolean found;

    public AbstractLinePart(int beginCol, Object token, PrettyPrinterDocLineEntry lineEntry) {
        this.beginCol = beginCol;
        this.token = token;
        this.lineEntry = lineEntry;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getToken()
     */
    @Override
    public Object getToken() {
        return token;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getBeginCol()
     */
    @Override
    public int getBeginCol() {
        return beginCol;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getLine()
     */
    @Override
    public int getLine() {
        return this.lineEntry.line;
    }

    @Override
    public int getLinePosition() {
        return this.lineEntry.getSortedParts().indexOf(this);
    }

    @Override
    public void setMarkAsFound() {
        this.found = true;
    }

    @Override
    public boolean isMarkedAsFound() {
        return found;
    }
}
