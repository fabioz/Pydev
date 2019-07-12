/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.model;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class ErrorDescription {
    public String message;
    // line 1-based
    public int errorLine;

    // offset 0-based
    public int errorStart;

    // offset 0-based
    public int errorEnd;

    public ErrorDescription(String message, int errorLine, int errorStart, int errorEnd) {
        super();
        this.message = message;
        this.errorLine = errorLine;
        this.errorStart = errorStart;
        if (errorEnd < errorStart) {
            errorEnd = errorStart;
        }
        this.errorEnd = errorEnd;
    }

    public int getBeginLine(IDocument doc) {
        return errorLine;
    }

    public int getBeginColumn(IDocument doc) {
        try {
            IRegion lineInformationOfOffset = doc.getLineInformationOfOffset(errorStart);
            return errorStart - lineInformationOfOffset.getOffset();
        } catch (BadLocationException e) {
        }
        return 0;
    }

    public int getEndLine(IDocument doc) {
        try {
            return doc.getLineOffset(errorEnd);
        } catch (BadLocationException e) {
        }
        return errorLine;
    }

    public int getEndCol(IDocument doc) {
        try {
            IRegion lineInformationOfOffset = doc.getLineInformationOfOffset(errorEnd);
            return errorEnd - lineInformationOfOffset.getOffset();
        } catch (BadLocationException e) {
        }
        return 0;
    }
}
