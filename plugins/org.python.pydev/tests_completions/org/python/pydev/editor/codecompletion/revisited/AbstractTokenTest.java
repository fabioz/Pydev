/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 02/10/2005
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

public class AbstractTokenTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AbstractTokenTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMakeRelative() throws Exception {
        String relative = AbstractToken.makeRelative("aa.bb", "aa.bb.xx.foo");
        assertEquals("aa.xx.foo", relative);
    }
}
