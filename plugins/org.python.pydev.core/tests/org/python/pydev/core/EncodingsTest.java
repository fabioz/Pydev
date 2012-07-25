/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.nio.charset.Charset;

import junit.framework.TestCase;

public class EncodingsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EncodingsTest.class);
    }

    public void testRefEncoding() throws Exception {
        String validEncoding = REF.getValidEncoding("latin-1", null);
        assertEquals("latin1", validEncoding);
        assertNull(REF.getValidEncoding("utf-8-*-", null));

        //supported
        assertTrue(Charset.isSupported("latin1"));
        assertTrue(Charset.isSupported("utf8"));
        assertTrue(Charset.isSupported("utf_16"));
        assertTrue(Charset.isSupported("UTF-8"));
        assertTrue(Charset.isSupported("utf-8"));

        //not supported
        assertFalse(Charset.isSupported("latin-1"));
        assertFalse(Charset.isSupported("utf_8")); //why utf_16 is supported and utf_8 is not is something that really amazes me.
    }
}
