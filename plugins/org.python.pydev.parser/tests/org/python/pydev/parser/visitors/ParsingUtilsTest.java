/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 16/07/2005
 */
package org.python.pydev.parser.visitors;

import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

public class ParsingUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ParsingUtilsTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRemoveCommentsAndWhitespaces() {
        String s = "a , b = 0,#ignore\n*args, **kwargs";
        FastStringBuffer buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsAndWhitespaces(buf);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());
    }

    public void testRemoveCommentsWhitespacesAndLiterals() throws SyntaxErrorException {
        String s = "a , b = 0,#ignore\n" +
                "*args, **kwargs\n" +
                "'''";
        FastStringBuffer buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());

        s = "a , b = 0,#ignore\n" +
                "*args, **kwargs\n" +
                "'''remove'\"";
        buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());

        s = "a , b = 0,#ignore\n" +
                "*args, **kwargs\n" +
                "'''remove'''keep";
        buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, true);
        assertEquals("a,b=0,*args,**kwargskeep", buf.toString());
    }

}
