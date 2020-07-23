/**
 * Copyright (c) 2020 by Brainwy Software Ltda
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
        FastStringBuffer buf = new FastStringBuffer();
        FastStringBuffer convertedLiteralsBuf = new FastStringBuffer();
        boolean gotContents = false;
        boolean gotLiterals = false;
        try {
            char ch = doc.getChar(offset);
            //handle the \r, \n or \r\n
            if (ch == '\n' || ch == '\r') {
                offset--;
                ch = doc.getChar(offset);
                if (offset >= 0 && ch == '\r') {
                    offset--;
                    ch = doc.getChar(offset);
                }
            }

            while (offset >= 0) {
                ch = doc.getChar(offset);
                if (ch == '\n') {
                    break;
                }

                if (fastPartitioner.getContentType(offset) != IPythonPartitions.PY_DEFAULT) {
                    convertedLiteralsBuf.insert(0, ' ');
                    gotLiterals = true;
                } else {
                    buf.insert(0, ch);
                    gotContents = true;
                }
                offset--;
            }

            if (gotContents && gotLiterals) {
                // checks if line starts with contents, has literals in the middle and then has contents again at the end
                // e.g.: s = """ something """ some random content
                // because it may return a shuffled string if line is not properly handled like that
                // using the above example, a return like: "s = some random content" + "                 "
                boolean gotStartingContents = fastPartitioner
                        .getContentType(offset + 1) == IPythonPartitions.PY_DEFAULT;
                boolean gotEndingContents = fastPartitioner
                        .getContentType(
                                offset + doc.getLineLength(getLastReturnedLine())) == IPythonPartitions.PY_DEFAULT;
                if (gotStartingContents && gotEndingContents) {
                    // if line has both contents and literals and start and end with contents, just return the whole line...
                    FastStringBuffer alterBuf = new FastStringBuffer();
                    for (int o = offset + 1; o < offset + doc.getLineLength(getLastReturnedLine()); o++) {
                        alterBuf.append(doc.getChar(o));
                    }
                    return alterBuf.toString();
                } else if (gotStartingContents) {
                    // if line only starts with content, concat both buffer strings on return
                    return buf.toString() + convertedLiteralsBuf.toString();
                } else {
                    // just ignore the content at the end
                    return convertedLiteralsBuf.toString();
                }
            }

            if (gotContents) {
                // line does not have literals
                return buf.toString();
            }

            // line is inside a literal
            return convertedLiteralsBuf.toString();
        } catch (

        Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not Impl.");
    }

}
