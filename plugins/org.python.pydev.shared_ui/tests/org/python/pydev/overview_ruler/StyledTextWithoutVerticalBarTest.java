package org.python.pydev.overview_ruler;

import org.eclipse.swt.custom.StyleRange;
import org.junit.Assert;
import org.python.pydev.overview_ruler.StyledTextWithoutVerticalBar.RangesInfo;

import junit.framework.TestCase;

public class StyledTextWithoutVerticalBarTest extends TestCase {

    public void testCreateRanges() {
        StyleRange[] styles = new StyleRange[] {
                new StyleRange(0, 2, null, null),
                new StyleRange(2, 3, null, null),
                new StyleRange(5, 5, null, null),
        };
        RangesInfo rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 10);
        int[] ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 0, 2, 2, 3, 5, 5 }, ranges);
        assertEquals(3, rangesInfo.styles.length);

        rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 3);
        ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 0, 2, 2, 1 }, ranges);
        assertEquals(2, rangesInfo.styles.length);

        rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 2);
        ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 0, 2 }, ranges);
        assertEquals(1, rangesInfo.styles.length);

        rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 1);
        ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 0, 1 }, ranges);
        assertEquals(1, rangesInfo.styles.length);
        assertEquals(0, rangesInfo.styles[0].start);
        assertEquals(1, rangesInfo.styles[0].length);
    }

    public void testWrongRanges1() {
        StyleRange[] styles = new StyleRange[] {
                new StyleRange(0, 2, null, null),
                new StyleRange(0, 3, null, null),
                new StyleRange(5, 5, null, null),
        };
        RangesInfo rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 10);
        int[] ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 0, 2, 2, 1, 5, 5 }, ranges);
        assertEquals(3, rangesInfo.styles.length);
    }

    public void testWrongRanges2() {
        StyleRange[] styles = new StyleRange[] {
                new StyleRange(2, 2, null, null),
                new StyleRange(0, 2, null, null),
                new StyleRange(5, 5, null, null),
        };
        RangesInfo rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 10);
        int[] ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 2, 2, 5, 5 }, ranges);
        assertEquals(2, rangesInfo.styles.length);
        assertEquals(2, rangesInfo.styles[0].start);
        assertEquals(2, rangesInfo.styles[0].length);
        assertEquals(5, rangesInfo.styles[1].start);
        assertEquals(5, rangesInfo.styles[1].length);
    }

    public void testWrongRanges3() {
        StyleRange[] styles = new StyleRange[] {
                new StyleRange(2, 2, null, null),
                new StyleRange(0, 2, null, null),
        };
        RangesInfo rangesInfo = StyledTextWithoutVerticalBar.createRanges(styles, 10);
        int[] ranges = rangesInfo.newRanges;
        Assert.assertArrayEquals(new int[] { 2, 2 }, ranges);
        assertEquals(1, rangesInfo.styles.length);
        assertEquals(2, rangesInfo.styles[0].start);
        assertEquals(2, rangesInfo.styles[0].length);
    }

}
