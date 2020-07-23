/**
 * Copyright (c) 2020 by Brainwy Software Ltda
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.shared_core.partitioner.FastPartitioner;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class ReversePyDocIterator implements IPyDocIterator {
    private int offset;
    private IDocument doc;
    private int lastReturned = -1;
    private FastPartitioner fastPartitioner;
    private final FastStringBuffer buf = new FastStringBuffer();

    public ReversePyDocIterator(IDocument doc) throws BadLocationException {
        this(doc, doc.getLineOfOffset(doc.getLength() - 1));
    }

    public ReversePyDocIterator(IDocument doc, int startingLine) throws BadLocationException {
        this.doc = doc;
        this.offset = doc.getLineOffset(startingLine) + doc.getLineLength(startingLine) - 1;
        this.fastPartitioner = (FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc);
    }

    @Override
    public boolean hasNext() {
        return offset >= 0;
    }

    @Override
    public int getLastReturnedLine() {
        if (offset < doc.getLength() - 1) {
            try {
                lastReturned = doc.getLineOfOffset(offset + 1);
            } catch (BadLocationException e) {
                //ignore (keep the last one)
            }
        }
        return lastReturned;
    }

    /**
     * @return the next line in the document
     */
    @Override
    public String next() {
        buf.clear();
        try {
            char ch = doc.getChar(offset);
            //handle the \r, \n or \r\n
            if (ch == '\n' || ch == '\r') {
                offset--;
                if (ch == '\n') {
                    ch = doc.getChar(offset);
                    if (offset >= 0 && ch == '\r') {
                        offset--;
                        ch = doc.getChar(offset);
                    }
                }
            }
            while (offset >= 0) {
                ch = doc.getChar(offset);
                if (ch == '\n' || ch == '\r') {
                    break;
                }
                if (fastPartitioner.getContentType(offset) != IPythonPartitions.PY_DEFAULT) {
                    buf.append(' ');
                } else {
                    buf.append(ch);
                }
                offset--;
            }
            return buf.reverse().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not Impl.");
    }
}
