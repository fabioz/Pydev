package org.python.pydev.dltk.console.ui;

import org.eclipse.swt.custom.StyleRange;

import junit.framework.TestCase;

public class ScriptConsolePartitionerTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testPartitioning() throws Exception {
        ScriptConsolePartitioner partitioner = new ScriptConsolePartitioner();
        partitioner.addRange(new StyleRange(0, 1, null, null));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);
        
        partitioner.addRange(new StyleRange(0, 1, null, null));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);
        
        partitioner.addRange(new StyleRange(0, 3, null, null));
        assertEquals(1, partitioner.getStyleRanges(0, 1).length);
        
        partitioner.addRange(new StyleRange(2, 1, null, null));
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
        assertEquals(47, styleRanges[2].length);
    }

}
