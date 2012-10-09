/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.rewriter;

import org.python.pydev.refactoring.tests.core.AbstractRewriterTestCase;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class RewriterTestCase extends AbstractRewriterTestCase {

    public RewriterTestCase(String name) {
        super(name);
    }

    public RewriterTestCase(String name, boolean ignoreEmptyLines) {
        super(name, ignoreEmptyLines);
    }

    public void runTest() throws Throwable {
        super.runRewriter();
        String expected = getExpected().replace("\r\n", "\n").replace("\r", "\n");
        //		System.out.println(">"+expected.replace(' ', '.').replace('\n', '|').replace('\r', '*')+"<");

        String generated = getGenerated();
        //        System.out.println(">"+generated.replace(' ', '.').replace('\n', '|').replace('\r', '*')+"<");
        assertEquals(expected, generated);
    }
}
