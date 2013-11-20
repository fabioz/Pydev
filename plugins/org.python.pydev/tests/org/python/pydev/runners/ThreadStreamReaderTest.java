/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.runners;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

/**
 * @author Fabio
 *
 */
public class ThreadStreamReaderTest extends TestCase {

    public void testThreadStreamReaderTest() throws Exception {
        String s = "aabbccddee\n\n";

        FastStringBuffer buf = new FastStringBuffer(s, 0);
        buf.appendN(s, 1000);

        InputStream is = new ByteArrayInputStream(buf.getBytes());
        ThreadStreamReader reader = new ThreadStreamReader(is);
        assertEquals("", reader.getContents());

        reader.start();

        final String expected = buf.toString();
        int i = 0;
        while (!reader.getContents().equals(expected)) {
            i++;
            if (i > 100) {
                assertEquals(expected, reader.getContents());
            }
            waitABit();
        }
        for (i = 0; i < 100; i++) {
            if (!reader.isAlive()) {
                break;
            }
            waitABit();
        }
        assertFalse(reader.isAlive());

    }

    private void waitABit() {
        synchronized (this) {
            try {
                this.wait(10);
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
