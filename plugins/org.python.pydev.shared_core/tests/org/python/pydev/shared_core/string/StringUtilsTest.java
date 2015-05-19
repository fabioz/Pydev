package org.python.pydev.shared_core.string;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public void testStringUtilsAsciiLetter() throws Exception {
        assertTrue(StringUtils.isAsciiLetter('a'));
        assertTrue(StringUtils.isAsciiLetter('A'));
        assertTrue(StringUtils.isAsciiLetter('z'));
        assertTrue(StringUtils.isAsciiLetter('Z'));

        assertFalse(StringUtils.isAsciiLetter('1'));
        assertFalse(StringUtils.isAsciiLetter('_'));
        assertFalse(StringUtils.isAsciiLetter('-'));
    }

    public void testStringUtilsAsciiLetterOrUnderline() throws Exception {
        assertTrue(StringUtils.isAsciiLetterOrUnderline('a'));
        assertTrue(StringUtils.isAsciiLetterOrUnderline('A'));
        assertTrue(StringUtils.isAsciiLetterOrUnderline('z'));
        assertTrue(StringUtils.isAsciiLetterOrUnderline('Z'));
        assertTrue(StringUtils.isAsciiLetterOrUnderline('_'));

        assertFalse(StringUtils.isAsciiLetterOrUnderline('1'));
        assertFalse(StringUtils.isAsciiLetterOrUnderline('-'));
    }
}
