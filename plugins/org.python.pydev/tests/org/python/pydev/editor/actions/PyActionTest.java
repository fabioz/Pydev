/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 22/08/2005
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.docutils.StringUtils;

import junit.framework.TestCase;

public class PyActionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyActionTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCountLineBreaks() {
        assertEquals(0, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa"));
        assertEquals(1, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa\n"));
        assertEquals(2, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa\n\r"));
        assertEquals(1, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa\r\n"));
        assertEquals(3, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa\nooo\nooo\n"));
        assertEquals(2, org.python.pydev.shared_core.string.StringUtils.countLineBreaks("aaa\r\nbb\r\n"));
    }
}
