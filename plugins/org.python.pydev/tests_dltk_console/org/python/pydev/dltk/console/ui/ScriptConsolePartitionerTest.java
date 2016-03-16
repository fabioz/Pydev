/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.dltk.console.ui;

import junit.framework.TestCase;

import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsolePartitioner;
import org.python.pydev.shared_interactive_console.console.ui.ScriptStyleRange;

public class ScriptConsolePartitionerTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testJoinPartitions() throws Exception {
        ScriptConsolePartitioner partitioner = new ScriptConsolePartitioner();
        partitioner.addRange(new ScriptStyleRange(0, 1, null, null, ScriptStyleRange.STDIN));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);

        partitioner.addRange(new ScriptStyleRange(1, 1, null, null, ScriptStyleRange.STDIN));
        assertEquals(1, partitioner.getStyleRanges(0, 2).length);

        partitioner.addRange(new ScriptStyleRange(1, 1, null, null, ScriptStyleRange.STDOUT));
        assertEquals(2, partitioner.getStyleRanges(0, 2).length);

        partitioner.addRange(new ScriptStyleRange(1, 1, null, null, ScriptStyleRange.STDIN));
        assertEquals(1, partitioner.getStyleRanges(0, 2).length);

    }

    public void testPartitioning() throws Exception {
        ScriptConsolePartitioner partitioner = new ScriptConsolePartitioner();
        partitioner.addRange(new ScriptStyleRange(0, 1, null, null, ScriptStyleRange.STDIN));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);

        partitioner.addRange(new ScriptStyleRange(0, 1, null, null, ScriptStyleRange.STDERR));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);

        partitioner.addRange(new ScriptStyleRange(0, 3, null, null, ScriptStyleRange.STDOUT));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);

        partitioner.addRange(new ScriptStyleRange(2, 1, null, null, ScriptStyleRange.PROMPT));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);

        StyleRange[] styleRanges = partitioner.getStyleRanges(0, 3);
        assertEquals(2, styleRanges.length);
        assertEquals(0, styleRanges[0].start);
        assertEquals(2, styleRanges[0].length);
        assertEquals(2, styleRanges[1].start);
        assertEquals(1, styleRanges[1].length);

        styleRanges = partitioner.getStyleRanges(0, 50);
        assertEquals(3, styleRanges.length);
        assertEquals(0, styleRanges[0].start);
        assertEquals(2, styleRanges[0].length);
        assertEquals(2, styleRanges[1].start);
        assertEquals(1, styleRanges[1].length);
        assertEquals(3, styleRanges[2].start);
        assertEquals(47, styleRanges[2].length);

        styleRanges = partitioner.getStyleRanges(1, 50);
        assertEquals(3, styleRanges.length);
        assertEquals(0, styleRanges[0].start);
        assertEquals(2, styleRanges[0].length);
        assertEquals(2, styleRanges[1].start);
        assertEquals(1, styleRanges[1].length);
        assertEquals(3, styleRanges[2].start);
        assertEquals(48, styleRanges[2].length);
    }

}
