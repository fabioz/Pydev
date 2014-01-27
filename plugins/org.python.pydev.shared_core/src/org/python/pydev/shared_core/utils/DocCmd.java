/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 16, 2006
 */
package org.python.pydev.shared_core.utils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Mostly a DocumentCommand, but with a custom customizable constructor, a proper toString method
 * and a way to pass an IDocument and have it applied.
 */
public class DocCmd extends DocumentCommand {

    public DocCmd(int offset, int length, String text) {
        this.offset = offset;
        this.length = length;
        this.text = text;
        this.caretOffset = -1;
        this.shiftsCaret = true;
        this.doit = true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer("DocCmd(\n", 100);
        buf.append("  offset: ");
        buf.append(offset);
        buf.append("\n  len: ");
        buf.append(length);
        buf.append("\n  text: ");
        buf.append(text);
        buf.append("\n  doit: ");
        buf.append(doit);
        buf.append("\n  caretOffset: ");
        buf.append(caretOffset);
        buf.append("\n  shiftsCaret: ");
        buf.append(shiftsCaret);
        buf.append("\n)");
        return buf.toString();
    }

    public void doExecute(IDocument document) throws BadLocationException {
        document.replace(offset, length, text);
    }

}
