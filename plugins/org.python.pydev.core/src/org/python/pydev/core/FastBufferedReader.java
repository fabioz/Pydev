/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.IOException;
import java.io.Reader;

import org.python.pydev.shared_core.string.FastStringBuffer;

/** 
 * A fast reader that'll read into a FastStringBuffer.
 */
public class FastBufferedReader {

    public final static int DEFAULT_BUFFER_SIZE = 16 * 1024;

    protected int availableSize;

    protected final char buf[];

    protected int pos;

    protected Reader r;

    public final FastStringBuffer stringBuf;

    public FastBufferedReader(final Reader r) {
        this(r, DEFAULT_BUFFER_SIZE);
    }

    public FastBufferedReader(final Reader r, final int bufSize) {
        this.r = r;
        buf = new char[bufSize];
        stringBuf = new FastStringBuffer(1024);
    }

    public void close() throws IOException {
        if (r == null)
            return;
        r.close();
        r = null;
    }

    /**
     * Fills the buffer with the line. Returns null if there was nothing to read, otherwise, returns the internal
     * buffer properly filled. Note that the same buffer will be used on a subsequent call.
     */
    public FastStringBuffer readLine() throws IOException {
        char c = 0;
        int i;
        stringBuf.clear();

        if (availableSize == 0) {
            availableSize = r.read(buf);
            //If we had nothing more to read, return null.
            if (availableSize <= 0) {
                availableSize = 0;
                return null;
            }
            pos = 0;
        }

        while (true) {
            i = 0;
            while (i < availableSize && (c = buf[pos + i]) != '\n' && c != '\r') {
                i++;
            }

            stringBuf.append(buf, pos, i);
            pos += i;
            availableSize -= i;

            if (availableSize > 0) {
                if (c == '\n') {
                    pos++;
                    availableSize--;
                } else { // found \r
                    if (availableSize > 1) {
                        if (buf[pos + 1] == '\n') {
                            pos += 2;
                            availableSize -= 2;
                        } else {
                            pos++;
                            availableSize--;
                        }
                    } else {
                        pos = 0;
                        availableSize = r.read(buf);
                        if (availableSize <= 0)
                            availableSize = 0;
                        else if (buf[0] == '\n') {
                            pos++;
                            availableSize--;
                        }
                    }
                }
                return stringBuf;
            } else {
                pos = 0;
                availableSize = r.read(buf);
                if (availableSize <= 0) {
                    availableSize = 0;
                    return stringBuf;
                }
            }
        }
    }

}
