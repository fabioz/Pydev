/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Special input stream we can write to and read() listeners will get it later.
 * 
 * This class should be thread safe.
 */
public class PipedInputStream extends InputStream {

    private boolean closed = false;
    private ExtendedByteArrayOutputStream buf;
    private Object readLock = new Object();
    private Object writeLock = new Object();

    public final OutputStream internalOutputStream = new OutputStream() {

        @Override
        public void write(int b) throws IOException {
            PipedInputStream.this.write(b);
        };

        @Override
        public void write(byte[] b) throws IOException {
            PipedInputStream.this.write(b);
        }

        @Override
        public void close() throws IOException {
            PipedInputStream.this.close();
        };
    };

    public PipedInputStream() {
        buf = new ExtendedByteArrayOutputStream();
    }

    @Override
    public int read() throws IOException {
        while (!closed) {
            synchronized (writeLock) {
                if (buf.size() > 0) {
                    return buf.deleteFirst();
                }
            }
            try {
                synchronized (readLock) {
                    readLock.notifyAll();
                    readLock.wait(10000);
                }
            } catch (InterruptedException e) {
            }
        }
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        while (!closed) {
            synchronized (writeLock) {
                if (buf.size() > 0) {
                    return buf.delete(b, off, len);
                }
            }
            try {
                synchronized (readLock) {
                    readLock.notifyAll(); //let writers write.
                    readLock.wait(10000);
                }
            } catch (InterruptedException e) {
            }
        }

        return -1;
    }

    public void write(int b) throws IOException {
        synchronized (writeLock) {
            buf.write(b);
        }
        synchronized (readLock) {
            readLock.notifyAll();
        }
    }

    public void write(byte[] bytes) throws IOException {
        synchronized (writeLock) {
            buf.write(bytes);
        }
        synchronized (readLock) {
            readLock.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        synchronized (readLock) {
            readLock.notifyAll();
        }
    }

}
