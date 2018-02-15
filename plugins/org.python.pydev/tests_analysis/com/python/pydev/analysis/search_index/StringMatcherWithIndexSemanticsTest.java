/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package com.python.pydev.analysis.search_index;

import org.python.pydev.shared_core.string.StringMatcher;
import org.python.pydev.shared_ui.search.StringMatcherWithIndexSemantics;
import org.python.pydev.shared_ui.search.StringMatcherWithIndexSemantics.Position;

import junit.framework.TestCase;

public class StringMatcherWithIndexSemanticsTest extends TestCase {

    public void testStringMatcherWithIndexSemantics() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("a", true, true);
        Position find = matcher.find("a", 0);
        assertEquals(find.start, 0);
        assertEquals(find.end, 1);
    }

    public void testStringMatcher() throws Exception {
        StringMatcher matcher = new StringMatcher("a", true, false);
        StringMatcher.Position find = matcher.find("a", 0, 1);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 1);
    }

    public void testStringMatcherWithIndexSemantics2() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("ab", true, true);
        Position find = matcher.find("ab", 0);
        assertEquals(find.start, 0);
        assertEquals(find.end, 2);
    }

    public void testStringMatcher2() throws Exception {
        StringMatcher matcher = new StringMatcher("ab", true, false);
        StringMatcher.Position find = matcher.find("ab", 0, 2);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 2);
    }

    public void testStringMatcherWithIndexSemantics3() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("*ab*", true, true);
        Position find = matcher.find("ab", 0);
        assertEquals(find.start, 0);
        assertEquals(find.end, 2);
    }

    public void testStringMatcherWithIndexSemantics3a() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("ab", true, true);
        Position find = matcher.find("aab", 0);
        assertNull(find);
    }

    public void testStringMatcher3() throws Exception {
        StringMatcher matcher = new StringMatcher("*ab*", true, false);
        StringMatcher.Position find = matcher.find("ab", 0, 2);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 2);
    }

    public void testStringMatcher4() throws Exception {
        StringMatcher matcher = new StringMatcher("\\*ab*", true, false);
        StringMatcher.Position find = matcher.find("*ab", 0, 3);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 3);
    }

    public void testStringMatcher4a() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("\\*ab*", true, true);
        StringMatcherWithIndexSemantics.Position find = matcher.find("*ab", 0);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 3);
    }

    public void testStringMatcher4ab() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("\\?ab*", true, true);
        StringMatcherWithIndexSemantics.Position find = matcher.find("?ab", 0);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 3);
    }

    public void testStringMatcher5a() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("\\*ab\\*", true, true);
        StringMatcherWithIndexSemantics.Position find = matcher.find("*ab*", 0);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 4);
    }

    public void testStringMatcher5b() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("*ab.", true, true);
        StringMatcherWithIndexSemantics.Position find = matcher.find("ab.", 0);
        assertEquals(find.getStart(), 0);
        assertEquals(find.getEnd(), 3);
    }

    public void testStringMatcherMatch() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("*ab.", true, true);
        assertTrue(matcher.match("ab."));
        assertTrue(matcher.match("cab."));
        assertFalse(matcher.match("cab.x"));
    }

    public void testStringMatcherMatch2() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("*ab.*", true, true);
        assertTrue(matcher.match("ab."));
        assertTrue(matcher.match("cab."));
        assertTrue(matcher.match("cab.x"));
    }

    public void testStringMatcherMatch3() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("ab.*", true, true);
        assertTrue(matcher.match("ab."));
        assertFalse(matcher.match("cab."));
        assertFalse(matcher.match("cab.x"));
        assertTrue(matcher.match("ab.x"));
    }

    public void testStringMatcherMatch4() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("ab.", true, true);
        assertTrue(matcher.match("ab."));
        assertFalse(matcher.match("cab."));
        assertFalse(matcher.match("cab.x"));
        assertFalse(matcher.match("ab.x"));
    }

    public void testStringMatcherNotWholeWord() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("ab", true, false);
        assertTrue(matcher.match("cabx"));
    }

    public void testStringMatcherNotWholeWord2() throws Exception {
        StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics("**ab**", true, false);
        assertTrue(matcher.match("cabx"));
    }
}
