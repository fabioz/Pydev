/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

public class LinePartRequireAdded extends LinePart {

    public LinePartRequireAdded(int beginCol, String string, Object token, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, string, token, lineEntry);
    }

}
