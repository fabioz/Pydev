/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.utils.ArrayUtils;

public final class StyledTextWithoutVerticalBar extends StyledText {

    public StyledTextWithoutVerticalBar(Composite parent, int style) {
        super(parent, style);
    }

    /**
     * Optimization:
     * The method:
     * org.eclipse.swt.custom.StyledTextRenderer.setStyleRanges(int[], StyleRange[])
     *
     * is *extremely* inefficient on huge documents with lots of styles when
     * ranges are not passed and have to be computed in the block:
     *
     * if (newRanges == null && COMPACT_STYLES) {
     *
     * So, we just pre-create the ranges here (Same thing on org.brainwy.liclipsetext.editor.common.LiClipseSourceViewer.StyledTextImproved)
     * A patch should later be given to SWT itself.
     */

    @Override
    public void setStyleRanges(StyleRange[] styles) {
        if (styles != null) {
            RangesInfo rangesInfo = createRanges(styles, this.getCharCount());
            int[] newRanges = rangesInfo.newRanges;
            styles = rangesInfo.styles;
            super.setStyleRanges(newRanges, styles);
            return;
        }
        super.setStyleRanges(styles);
    }

    @Override
    public void replaceStyleRanges(int start, int length, StyleRange[] styles) {
        checkWidget();
        if (isListening(ST.LineGetStyle)) {
            return;
        }
        if (styles == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        RangesInfo rangesInfo = createRanges(styles, this.getCharCount());
        int[] newRanges = rangesInfo.newRanges;
        styles = rangesInfo.styles;
        try {
            setStyleRanges(start, length, newRanges, styles);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    @Override
    public void redraw() {
        try {
            super.redraw();
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public static class RangesInfo {

        public final StyleRange[] styles;
        public final int[] newRanges;

        public RangesInfo(StyleRange[] styles, int[] newRanges) {
            this.styles = styles;
            this.newRanges = newRanges;
        }
    }

    public static RangesInfo createRanges(StyleRange[] styles, int charCount) throws AssertionError {

        int[] newRanges = new int[styles.length << 1];
        int removeRangesFrom = -1;
        List<Integer> removeRanges = new ArrayList<>();

        int endOffset = -1;
        int i = 0, j = 0;
        for (; i < styles.length; i++) {
            StyleRange newStyle = styles[i];
            if (newStyle.start >= charCount) {
                Log.log("Removing ranges past end.");
                removeRangesFrom = i;
                break;
            }
            if (endOffset > newStyle.start) {
                String msg = "Error endOffset (" + endOffset + ") > next style start (" + newStyle.start + ")";
                Log.log(msg);
                int diff = endOffset - newStyle.start;
                newStyle.start = endOffset;
                newStyle.length -= diff;
                if (newStyle.length < 0) {
                    // Unable to fix it (remove element).
                    removeRanges.add(i);
                    continue;
                }
            }

            endOffset = newStyle.start + newStyle.length;
            if (endOffset > charCount) {
                String msg = "Error endOffset (" + endOffset + ") > charCount (" + charCount + ")";
                Log.log(msg);
                newStyle.length -= endOffset - charCount;
                if (newStyle.length < 0) {
                    Log.log("Removing ranges past end.");
                    removeRangesFrom = i;
                    break;
                }
            }

            newRanges[j++] = newStyle.start;
            newRanges[j++] = newStyle.length;
        }
        if (j < newRanges.length - 1) {
            int[] reallocate = new int[j];
            System.arraycopy(newRanges, 0, reallocate, 0, j);
            newRanges = reallocate;
        }
        if (removeRangesFrom != -1) {
            StyleRange[] reallocate = new StyleRange[removeRangesFrom];
            System.arraycopy(styles, 0, reallocate, 0, removeRangesFrom);
            styles = reallocate;
        }
        if (removeRanges.size() > 0) {
            Collections.reverse(removeRanges);
        }
        for (int remove : removeRanges) {
            if (remove < styles.length) {
                styles = ArrayUtils.remove(styles, remove, StyleRange.class);
            }
        }
        return new RangesInfo(styles, newRanges);
    }
}