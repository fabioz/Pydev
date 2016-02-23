/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.jython;

public final class SpecialStr implements ISpecialStr {

    public final String str;
    public final int beginLine;
    public final int beginCol;

    public SpecialStr(String str, int beginLine, int beginCol) {
        this.str = str;
        this.beginLine = beginLine;
        this.beginCol = beginCol;
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SpecialStr)) {
            return false;
        }
        return str.equals(((SpecialStr) obj).str);
    }

    @Override
    public int getBeginCol() {
        return beginCol;
    }

    @Override
    public int getBeginLine() {
        return beginLine;
    }
}
