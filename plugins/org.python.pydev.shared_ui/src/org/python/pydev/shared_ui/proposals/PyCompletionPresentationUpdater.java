/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 13, 2006
 * @author Fabio
 */
package org.python.pydev.shared_ui.proposals;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class PyCompletionPresentationUpdater {

    private StyleRange fRememberedStyleRange;
    private ITextPresentationListener fTextPresentationListener;

    private static Color getForegroundColor() {
        return Display.getDefault().getSystemColor(SWT.COLOR_RED);
    }

    private static Color getBackgroundColor() {
        return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
    }

    public PyCompletionPresentationUpdater() {
    }

    private StyleRange createStyleRange(ITextViewer viewer, int initialOffset, int len) {
        StyledText text = viewer.getTextWidget();
        if (text == null || text.isDisposed()) {
            return null;
        }

        int widgetCaret = text.getCaretOffset();

        int modelCaret = 0;
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            modelCaret = extension.widgetOffset2ModelOffset(widgetCaret);
        } else {
            IRegion visibleRegion = viewer.getVisibleRegion();
            modelCaret = widgetCaret + visibleRegion.getOffset();
        }

        if (modelCaret >= initialOffset + len) {
            return null;
        }

        int length = initialOffset + len - modelCaret;

        Color foreground = getForegroundColor();
        Color background = getBackgroundColor();

        return new StyleRange(modelCaret, length, foreground, background);
    }

    public void selected(final ITextViewer viewer, final int initialOffset, final int len) {
        repairPresentation(viewer);
        fRememberedStyleRange = null;

        StyleRange range = createStyleRange(viewer, initialOffset, len);
        if (range == null) {
            return;
        }
        fRememberedStyleRange = range;

        if (fTextPresentationListener == null) {
            fTextPresentationListener = new ITextPresentationListener() {
                @Override
                public void applyTextPresentation(TextPresentation textPresentation) {
                    fRememberedStyleRange = createStyleRange(viewer, initialOffset, len);
                    if (fRememberedStyleRange != null) {
                        textPresentation.mergeStyleRange(fRememberedStyleRange);
                    }
                }
            };
            ((ITextViewerExtension4) viewer).addTextPresentationListener(fTextPresentationListener);
        }
        repairPresentation(viewer);
    }

    public void unselected(ITextViewer viewer) {
        if (fTextPresentationListener != null) {
            ((ITextViewerExtension4) viewer).removeTextPresentationListener(fTextPresentationListener);
            fTextPresentationListener = null;
        }
        repairPresentation(viewer);
        fRememberedStyleRange = null;
    }

    private void repairPresentation(ITextViewer viewer) {
        if (fRememberedStyleRange != null) {
            if (viewer instanceof ITextViewerExtension2) {
                // attempts to reduce the redraw area
                ITextViewerExtension2 viewer2 = (ITextViewerExtension2) viewer;
                viewer2.invalidateTextPresentation(fRememberedStyleRange.start, fRememberedStyleRange.length);
            } else {
                viewer.invalidateTextPresentation();
            }
        }
    }
}
