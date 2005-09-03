/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StringUtilsTest.class);
    }

    public void testFormat() {
        assertEquals("teste", StringUtils.format("%s", new Object[]{"teste"}));
        assertEquals("teste 1", StringUtils.format("%s 1", new Object[]{"teste"}));
        assertEquals("%", StringUtils.format("%", new Object[]{}));
    }
}
