/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.FastStack;

public class WriterEraserV2 implements IWriterEraser {

    FastStack<FastStringBuffer> buf = new FastStack<FastStringBuffer>(30);

    public WriterEraserV2() {
        pushTempBuffer(); //this is the initial buffer (should never be removed)
    }

    @Override
    public void write(String o) {
        buf.peek().append(o);
    }

    @Override
    public void erase(String o) {
        FastStringBuffer buffer = buf.peek();
        if (buffer.toString().endsWith(o)) {
            //only delete if it ends with what was passed
            int len = o.length();
            int bufLen = buffer.length();
            buffer.delete(bufLen - len, bufLen);
        }
    }

    @Override
    public boolean endsWithSpace() {
        FastStringBuffer current = buf.peek();
        if (current.length() == 0) {
            return false;
        }
        return current.lastChar() == ' ';
    }

    @Override
    public FastStringBuffer getBuffer() {
        return buf.peek();
    }

    @Override
    public void pushTempBuffer() {
        buf.push(new FastStringBuffer());
    }

    @Override
    public String popTempBuffer() {
        return buf.pop().toString();
    }

    @Override
    public String toString() {
        return "WriterEraser<" + buf.peek().toString() + ">";
    }
}
