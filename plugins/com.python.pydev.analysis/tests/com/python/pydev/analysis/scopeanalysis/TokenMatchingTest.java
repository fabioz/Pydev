/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.python.pydev.shared_core.string.StringUtils;

public class TokenMatchingTest extends TestCase {

    public void testSearch() throws Exception {

        TokenMatching s = new TokenMatching("foo");
        assertTrue(s.hasMatch("foo"));
        assertTrue(s.hasMatch(" foo"));
        assertTrue(s.hasMatch("foo "));
        assertTrue(s.hasMatch(" foo "));
        assertTrue(s.hasMatch("a foo)o"));
        assertTrue(s.hasMatch("a foo.o"));

        //we only match on 'exact' matches
        assertTrue(!s.hasMatch("bar"));
        assertTrue(!s.hasMatch("fooo"));
        assertTrue(!s.hasMatch("afoo"));
        assertTrue(!s.hasMatch("fooa"));
        assertTrue(!s.hasMatch("foao"));
        assertTrue(!s.hasMatch("fo"));
    }

    public void testMatches() throws Exception {
        final ArrayList<Integer> offsets = new ArrayList<Integer>();
        TextSearchRequestor textSearchRequestor = new TextSearchRequestor() {
            @Override
            public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
                offsets.add(matchAccess.getMatchOffset());
                return true;
            }
        };
        TokenMatching matching = new TokenMatching(textSearchRequestor, "foo");
        matching.collectMatches(null, "foo , foo fooba, afoo, foo)a", new NullProgressMonitor(), false);
        compare(new Integer[] { 0, 6, 23 }, offsets);

        compare(new Integer[] { 0, 6, 23 }, TokenMatching.getMatchOffsets("foo", "foo , foo fooba, afoo, foo)a"));
    }

    private void compare(Integer[] is, ArrayList<Integer> offsets) {
        for (int i = 0; i < is.length; i++) {
            if (!is[i].equals(offsets.get(i))) {
                fail(StringUtils.format("%s != %s (%s)", is[i], offsets.get(i),
                        Arrays.deepToString(is)
                                + " differs from " + offsets));
            }
        }
    }
}
