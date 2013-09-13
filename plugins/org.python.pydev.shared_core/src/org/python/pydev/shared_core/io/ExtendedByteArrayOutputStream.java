/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.io;

import java.io.ByteArrayOutputStream;

/**
 * Byte array with methods to delete a part of its contents.
 * 
 * Note that it's not thread-safe!
 */
public class ExtendedByteArrayOutputStream extends ByteArrayOutputStream {

    public ExtendedByteArrayOutputStream() {
        super();
    }

    public ExtendedByteArrayOutputStream(int i) {
        super(i);
    }

    public int deleteFirst() {
        byte ret = this.buf[0];
        System.arraycopy(this.buf, 1, this.buf, 0, this.buf.length - 1);
        this.count--;
        return ret;
    }

    public int delete(byte[] b, int off, int len) {
        if (this.size() < len) {
            len = this.size();
        }
        if (len == 0) {
            return 0;
        }
        System.arraycopy(this.buf, 0, b, off, len);
        int diff = this.count - len;

        System.arraycopy(this.buf, len, this.buf, 0, diff);
        this.count -= len;
        return len;
    }

    public String readAndDelete() {
        String ret = new String(this.toByteArray());
        this.reset();
        return ret;
    }
}
