package org.python.pydev.core;

import java.nio.charset.Charset;

import junit.framework.TestCase;

public class TestEncodings extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestEncodings.class);
    }
    public void testRefEncoding() throws Exception {
        String validEncoding = REF.getValidEncoding("latin-1");
        assertEquals("latin1", validEncoding);
        
        //supported
        assertTrue(Charset.isSupported("latin1"));
        assertTrue(Charset.isSupported("utf8"));
        assertTrue(Charset.isSupported("utf_16"));
        assertTrue(Charset.isSupported("UTF-8"));
        assertTrue(Charset.isSupported("utf-8"));

        //not supported
        assertTrue(!Charset.isSupported("latin-1"));
        assertTrue(!Charset.isSupported("utf_8")); //why utf_16 is supported and utf_8 is not is something that really amazes me.
    }
}
