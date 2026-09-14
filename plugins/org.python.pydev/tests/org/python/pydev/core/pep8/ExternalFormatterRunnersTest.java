/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.pep8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.python.pydev.core.formatter.ExternalFormatterException;

import junit.framework.TestCase;

/**
 * Command-line construction for range formatting with black and ruff.
 *
 * black: --line-ranges=START-END, 1-based, inclusive on both ends, repeatable.
 * ruff:  --range=START-END, 1-based, END is exclusive; a single range per invocation.
 */
public class ExternalFormatterRunnersTest extends TestCase {

    public void testBlackGetsOneLineRangesFlagPerContiguousBlock() {
        String[] args = BlackRunner.lineRangesArgs(new int[] { 0, 1, 2, 5 });
        assertEquals("[--line-ranges=1-3, --line-ranges=6-6]", Arrays.toString(args));
    }

    public void testRuffRangeEndsOnTheFollowingLineBecauseTheEndIsExclusive() {
        assertEquals("--range=1-4", RuffRunner.rangeArg(new int[] { 1, 3 }, 10));
    }

    public void testRuffRangeReachingTheLastLineHasNoEnd() {
        assertEquals("--range=8", RuffRunner.rangeArg(new int[] { 8, 10 }, 10));
    }

    public void testRuffWithoutRangesRunsOnceOnTheWholeFile() throws ExternalFormatterException {
        List<String> seen = new ArrayList<>();
        RuffRunner.IRuffInvocation invocation = (current, rangeArg) -> {
            seen.add(rangeArg);
            return current + "\n# " + rangeArg;
        };

        String result = RuffRunner.formatWithInvocation(invocation, "l1\nl2", null);

        assertEquals("[null]", seen.toString());
        assertEquals("l1\nl2\n# null", result);
    }

    public void testRuffRangesAreFormattedBottomUpChainingEachOutput() throws ExternalFormatterException {
        String contents = "l1\nl2\nl3\nl4\nl5\nl6";
        List<String> seen = new ArrayList<>();
        RuffRunner.IRuffInvocation invocation = (current, rangeArg) -> {
            seen.add(rangeArg);
            return current + "\n# " + rangeArg;
        };

        String result = RuffRunner.formatWithInvocation(invocation, contents, new int[] { 0, 4 });

        // The lower range goes first so that the line numbers of the upper range stay valid
        // even though each pass may add or remove lines.
        assertEquals("[--range=5-6, --range=1-2]", seen.toString());
        assertEquals(contents + "\n# --range=5-6\n# --range=1-2", result);
    }
}
