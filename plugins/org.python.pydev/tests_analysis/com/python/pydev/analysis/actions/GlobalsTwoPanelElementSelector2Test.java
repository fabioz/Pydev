/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import org.eclipse.ui.dialogs.SearchPattern;

import com.python.pydev.analysis.additionalinfo.ClassInfo;

import junit.framework.TestCase;

public class GlobalsTwoPanelElementSelector2Test extends TestCase {

    public void testPatternMatch() throws Exception {
        SearchPattern patternMatcher = new SearchPattern();
        patternMatcher.setPattern("aa");

        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aa", null, null, null)));

        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", null, null, null)));

        assertFalse(MatchHelper.matchItem(patternMatcher, new ClassInfo("baaa", null, null, null)));

        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "coi.foo", null, null)));

        patternMatcher.setPattern("xx.aa");
        assertFalse(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "invalid.foo", null, null)));

        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo", null, null)));

        patternMatcher.setPattern("xx.foo.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo.bar", null, null)));

        patternMatcher.setPattern("xx.foo.bar.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo.bar", null, null)));

        patternMatcher.setPattern("xx.foo.bar.aa.aa");
        assertFalse(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo.bar", null, null)));

        patternMatcher.setPattern("xx.foo.ba.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo.bar", null, null)));

        patternMatcher.setPattern("xx.fo*o.ba.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, new ClassInfo("aaa", "xx.foo.bar", null, null)));

        patternMatcher.setPattern("coi*.intersection");
        assertTrue(MatchHelper.matchItem(patternMatcher,
                new ClassInfo("Intersection", "coilib50.basic.native", null, null)));

        patternMatcher.setPattern("coilib50.intersection");
        assertTrue(MatchHelper.matchItem(patternMatcher,
                new ClassInfo("Intersection", "coilib50.basic.native", null, null)));

        patternMatcher.setPattern("coilib50.");
        assertTrue(MatchHelper.matchItem(patternMatcher,
                new ClassInfo("Intersection", "coilib50.basic.native", null, null)));
    }

    public void testPatternSubAndEquals() throws Exception {
        assertFalse(MatchHelper.equalsFilter("aa", "aa "));

        assertTrue(MatchHelper.equalsFilter("aa", "aa"));
        assertFalse(MatchHelper.equalsFilter("aa.", "aa"));
        assertFalse(MatchHelper.equalsFilter("aa", "aa."));
        assertTrue(MatchHelper.equalsFilter("aa.", "aa."));

        assertTrue(MatchHelper.isSubFilter("aa.", "aa."));
        assertTrue(MatchHelper.isSubFilter("aa", "aab"));
        assertFalse(MatchHelper.isSubFilter("", "a"));
        assertFalse(MatchHelper.isSubFilter("a.", "a"));
        assertFalse(MatchHelper.isSubFilter("a.", "a.a"));
        assertTrue(MatchHelper.isSubFilter("a.a", "a.ab"));
        assertTrue(MatchHelper.isSubFilter("aa.b", "aa.ba"));
        assertFalse(MatchHelper.isSubFilter("a.", "a.ab"));
        assertFalse(MatchHelper.isSubFilter("a", "a.ab"));
    }

}
