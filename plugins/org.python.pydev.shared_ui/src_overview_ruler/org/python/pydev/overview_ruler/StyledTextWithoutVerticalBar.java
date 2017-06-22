/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

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
            int[] newRanges = new int[styles.length << 1];
            for (int i = 0, j = 0; i < styles.length; i++) {
                StyleRange newStyle = styles[i];
                newRanges[j++] = newStyle.start;
                newRanges[j++] = newStyle.length;
            }
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

        int[] newRanges = new int[styles.length << 1];
        for (int i = 0, j = 0; i < styles.length; i++) {
            StyleRange newStyle = styles[i];
            newRanges[j++] = newStyle.start;
            newRanges[j++] = newStyle.length;
        }
        setStyleRanges(start, length, newRanges, styles);
    }
}