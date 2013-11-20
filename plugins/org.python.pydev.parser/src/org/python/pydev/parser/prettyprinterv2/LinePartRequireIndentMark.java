/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

public class LinePartRequireIndentMark extends LinePartRequireMark {

    public LinePartRequireIndentMark(int beginColumn, String string, PrettyPrinterDocLineEntry prettyPrinterDocLineEntry) {
        super(beginColumn, string, prettyPrinterDocLineEntry);
    }

    @Override
    public String toString() {
        return "INDENT";
    }

}
