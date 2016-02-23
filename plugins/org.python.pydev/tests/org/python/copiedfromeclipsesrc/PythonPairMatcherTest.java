/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.copiedfromeclipsesrc;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PythonPairMatcher;

import junit.framework.TestCase;

public class PythonPairMatcherTest extends TestCase {

    public static void main(String[] args) {
        PythonPairMatcherTest test = new PythonPairMatcherTest();
        try {
            test.setUp();
            test.testSearchesOnlyInCurrentStatement();
            test.tearDown();

            junit.textui.TestRunner.run(PythonPairMatcherTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMatch() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "test (";
        assertEquals(5, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
        s = "test ";
        assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
        s = "test () ";
        assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
    }

    public void testSearchesOnlyInCurrentStatement() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "" +
                "a = (\n" +
                "def m1():\n" +
                "    b = ()";
        assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
    }

    public void testMatch1() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "\ntest ('[#') ";
        assertEquals(-1, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));

    }

    public void testMatch2() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "\ntest ('''\n[#''') ";
        assertEquals(-1, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));

        s = "\ntest (    ";
        assertEquals(6, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));

    }

    private PythonPairMatcher getMatcher() {
        return new PythonPairMatcher(new char[] { '(', ')', '[', ']' });
    }

}
