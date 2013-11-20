/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class DocUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DocUtilsTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPeer() throws Exception {
        assertEquals('(', org.python.pydev.shared_core.string.StringUtils.getPeer(')'));
        assertEquals(')', org.python.pydev.shared_core.string.StringUtils.getPeer('('));

        assertEquals('{', org.python.pydev.shared_core.string.StringUtils.getPeer('}'));
        assertEquals('}', org.python.pydev.shared_core.string.StringUtils.getPeer('{'));

        assertEquals('[', org.python.pydev.shared_core.string.StringUtils.getPeer(']'));
        assertEquals(']', org.python.pydev.shared_core.string.StringUtils.getPeer('['));
    }
}
