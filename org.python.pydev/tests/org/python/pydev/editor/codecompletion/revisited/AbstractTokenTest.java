/*
 * Created on 02/10/2005
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

public class AbstractTokenTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AbstractTokenTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMakeRelative() throws Exception {
        String relative = AbstractToken.makeRelative("aa.bb", "aa.bb.xx.foo");
        assertEquals("aa.xx.foo",relative);
    }
}
