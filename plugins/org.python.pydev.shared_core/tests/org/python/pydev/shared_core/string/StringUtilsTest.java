package org.python.pydev.shared_core.string;

import java.util.Arrays;

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

    public void testValidForIndexMatching() throws Exception {
        StringUtils.checkTokensValidForWildcardQuery("a.b");
        StringUtils.checkTokensValidForWildcardQuery("a.b.c");
        StringUtils.checkTokensValidForWildcardQuery("a.b.c.*");
        try {
            StringUtils.checkTokensValidForWildcardQuery("?.*?");
            fail("expected to fail");
        } catch (RuntimeException e) {

        }
        StringUtils.checkTokensValidForWildcardQuery("a.b?*.c");
    }

    public void testSplitForIndexMatching() throws Exception {
        assertEquals(StringUtils.splitForIndexMatching("a.b"), Arrays.asList("a", "b"));
        assertEquals(StringUtils.splitForIndexMatching("a."), Arrays.asList("a"));
        assertEquals(StringUtils.splitForIndexMatching("a*."), Arrays.asList("a*"));
        assertEquals(StringUtils.splitForIndexMatching("*a"), Arrays.asList("*a"));
        assertEquals(StringUtils.splitForIndexMatching("*"), Arrays.asList()); // Note: this is actually invalid for searching afterwards
        assertEquals(StringUtils.splitForIndexMatching("*?"), Arrays.asList()); // Note: this is actually invalid for searching afterwards
        assertEquals(StringUtils.splitForIndexMatching("\"!@#@\""), Arrays.asList()); // Note: this is actually invalid for searching afterwards
    }
}
