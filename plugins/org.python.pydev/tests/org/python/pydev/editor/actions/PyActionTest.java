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

import junit.framework.TestCase;

import org.python.pydev.shared_core.string.StringUtils;

public class PyActionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyActionTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCountLineBreaks() {
        assertEquals(0, StringUtils.countLineBreaks("aaa"));
        assertEquals(1, StringUtils.countLineBreaks("aaa\n"));
        assertEquals(2, StringUtils.countLineBreaks("aaa\n\r"));
        assertEquals(1, StringUtils.countLineBreaks("aaa\r\n"));
        assertEquals(3, StringUtils.countLineBreaks("aaa\nooo\nooo\n"));
        assertEquals(2, StringUtils.countLineBreaks("aaa\r\nbb\r\n"));
    }
}
