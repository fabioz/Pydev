/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns the 0-based line numbers computed for "format only changed lines" into the 1-based,
 * inclusive line ranges that external formatters (black, ruff) accept.
 */
public final class LineRanges {

    private LineRanges() {
    }

    /**
     * @param zeroBasedLines 0-based line indices, in any order, duplicates allowed (not modified).
     * @return sorted, non-overlapping {firstLine, lastLine} pairs, 1-based and inclusive on both ends.
     */
    public static List<int[]> toOneBasedInclusiveRanges(int[] zeroBasedLines) {
        List<int[]> ranges = new ArrayList<>();
        if (zeroBasedLines == null || zeroBasedLines.length == 0) {
            return ranges;
        }
        int[] sorted = zeroBasedLines.clone();
        Arrays.sort(sorted);

        int start = sorted[0];
        int end = sorted[0];
        for (int i = 1; i < sorted.length; i++) {
            int line = sorted[i];
            if (line <= end + 1) {
                // Contiguous with (or a duplicate of) the current range.
                end = Math.max(end, line);
            } else {
                ranges.add(new int[] { start + 1, end + 1 });
                start = line;
                end = line;
            }
        }
        ranges.add(new int[] { start + 1, end + 1 });
        return ranges;
    }
}
