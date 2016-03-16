/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

public class LinePart extends AbstractLinePart implements ILinePart, ILinePart2 {

    private String string;

    public LinePart(int beginCol, String string, Object token, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.setString(string);
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#setString(java.lang.String)
     */
    @Override
    public void setString(String string) {
        this.string = string;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getString()
     */
    @Override
    public String getString() {
        return string;
    }

    @Override
    public String toString() {
        return getString();
    }

}
