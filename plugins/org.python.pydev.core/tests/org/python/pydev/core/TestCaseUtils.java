package org.python.pydev.core;

import junit.framework.TestCase;

import org.python.pydev.core.docutils.StringUtils;

public class TestCaseUtils extends TestCase{
    
    
    protected void assertContentsEqual(String expected, String generated) {
        System.out.println(generated);
        assertEquals(StringUtils.replaceNewLines(expected, "\n"), StringUtils.replaceNewLines(generated, "\n"));
    }

}
