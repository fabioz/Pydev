/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.model;

import org.python.pydev.shared_core.io.ExtendedByteArrayOutputStream;

import junit.framework.TestCase;

public class MyByteArrayOutputStreamTest extends TestCase {

    public void testIt() throws Exception {
        ExtendedByteArrayOutputStream myByteArrayOutputStream = new ExtendedByteArrayOutputStream();
        myByteArrayOutputStream.write(new byte[] { 4, 10 });
        assertEquals(2, myByteArrayOutputStream.size());
        assertEquals(4, myByteArrayOutputStream.deleteFirst());

        assertEquals(1, myByteArrayOutputStream.size());
        assertEquals(10, myByteArrayOutputStream.deleteFirst());

        assertEquals(0, myByteArrayOutputStream.size());

        myByteArrayOutputStream.write(new byte[] { 1, 2, 3, 4 });
        byte[] b = new byte[2];
        assertEquals(2, myByteArrayOutputStream.delete(b, 0, 2));
        assertEquals(1, b[0]);
        assertEquals(2, b[1]);
        assertEquals(2, myByteArrayOutputStream.size());

        assertEquals(2, myByteArrayOutputStream.delete(b, 0, 2));
        assertEquals(3, b[0]);
        assertEquals(4, b[1]);
        assertEquals(0, myByteArrayOutputStream.size());

        assertEquals(0, myByteArrayOutputStream.delete(b, 0, 2));
        myByteArrayOutputStream.write(new byte[] { 7 });

        assertEquals(1, myByteArrayOutputStream.delete(b, 0, 2));
        assertEquals(7, b[0]);

        b = new byte[1024];
        myByteArrayOutputStream.write(new byte[] { 1, 2, 3, 4 });
        assertEquals(4, myByteArrayOutputStream.delete(b, 0, 1024));

        myByteArrayOutputStream.write(new byte[] { 1, 2 });
        assertEquals(2, myByteArrayOutputStream.delete(b, 0, 1024));

        myByteArrayOutputStream = new ExtendedByteArrayOutputStream(5);
        myByteArrayOutputStream.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        assertEquals(10, myByteArrayOutputStream.delete(b, 512, 1024));
    }
}
