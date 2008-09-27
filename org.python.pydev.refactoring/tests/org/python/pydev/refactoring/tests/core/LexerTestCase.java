package org.python.pydev.refactoring.tests.core;

import org.python.pydev.core.TestDependent;

import junit.framework.TestCase;

public class LexerTestCase extends TestCase {

    private IOTestCaseLexer scanner;

    @Override
    protected void setUp() throws Exception {
        this.scanner = new IOTestCaseLexer(new java.io.FileReader(TestDependent.TEST_PYDEV_REFACTORING_PLUGIN_LOC+"tests/python/core/testIOTestCaseLexer.py"));
        this.scanner.scan();
    }

    public void testSource() {
        String text = scanner.getSource();
        assertTrue(text.length() > 0);
        assertFalse(text.contains("##r"));
        assertFalse(text.contains("##c"));
    }

    public void testResult() {
        String text = scanner.getResult();
        assertTrue(text.length() > 0);
        assertFalse(text.contains("##s"));
        assertFalse(text.contains("##c"));
    }

    public void testConfig() {
        String text = scanner.getConfig();
        assertTrue(text.length() > 0);
        assertFalse(text.contains("'''"));
        assertFalse(text.contains("##s"));
        assertFalse(text.contains("##c"));
    }
}
