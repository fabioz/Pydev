/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.formatter;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class LineRangesTest extends TestCase {

    public void testCollapsesConsecutiveLinesIntoOneBasedInclusiveRanges() {
        List<int[]> ranges = LineRanges.toOneBasedInclusiveRanges(new int[] { 0, 1, 2, 5, 7, 8 });
        assertEquals("[[1, 3], [6, 6], [8, 9]]", Arrays.deepToString(ranges.toArray()));
    }

    public void testNoLinesGivesNoRanges() {
        List<int[]> ranges = LineRanges.toOneBasedInclusiveRanges(new int[0]);
        assertEquals("[]", Arrays.deepToString(ranges.toArray()));
    }

    public void testUnsortedAndDuplicateLinesAreNormalized() {
        List<int[]> ranges = LineRanges.toOneBasedInclusiveRanges(new int[] { 5, 1, 2, 2 });
        assertEquals("[[2, 3], [6, 6]]", Arrays.deepToString(ranges.toArray()));
    }
}
