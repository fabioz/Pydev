package org.python.pydev.shared_core.version;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public void testVersionComparison() {
        Version v1 = new Version("1.2");
        Version v2 = new Version("1.2.1");
        Version v3 = new Version("1.2.2");
        Version v4 = new Version("1.3");

        Version v5 = new Version("2.0");
        Version v6 = new Version("2.0.0");

        assertTrue(v1.isGreaterThanOrEqualTo(v1));
        assertTrue(v5.isGreaterThanOrEqualTo(v6));
        assertTrue(v5.isGreaterThanOrEqualTo(v4));

        assertFalse(v1.isGreaterThanOrEqualTo(v2));
        assertFalse(v2.isGreaterThanOrEqualTo(v3));
        assertFalse(v3.isGreaterThanOrEqualTo(v4));
        assertFalse(v4.isGreaterThanOrEqualTo(v5));
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(VersionTest.class);
    }
}