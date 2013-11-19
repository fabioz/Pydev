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
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class PyCompletionPresentationUpdater {

    private StyleRange fRememberedStyleRange;

    private static Color getForegroundColor(StyledText text) {
        return Display.getDefault().getSystemColor(SWT.COLOR_RED);
    }

    private static Color getBackgroundColor(StyledText text) {
        return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
    }

    public PyCompletionPresentationUpdater() {
    }

    public void repairPresentation(ITextViewer viewer) {
        if (fRememberedStyleRange != null) {
            if (viewer instanceof ITextViewerExtension2) {
                // attempts to reduce the redraw area
                ITextViewerExtension2 viewer2 = (ITextViewerExtension2) viewer;

                if (viewer instanceof ITextViewerExtension5) {

                    ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
                    IRegion modelRange = extension.widgetRange2ModelRange(new Region(fRememberedStyleRange.start,
                            fRememberedStyleRange.length));
                    if (modelRange != null) {
                        viewer2.invalidateTextPresentation(modelRange.getOffset(), modelRange.getLength());
                    }

                } else {
                    viewer2.invalidateTextPresentation(fRememberedStyleRange.start
                            + viewer.getVisibleRegion().getOffset(), fRememberedStyleRange.length);
                }

            } else {
                viewer.invalidateTextPresentation();
            }
        }
        if (viewer instanceof ICompletionStyleToggleEnabler) {
            ICompletionStyleToggleEnabler pySourceViewer = (ICompletionStyleToggleEnabler) viewer;
            pySourceViewer.setInToggleCompletionStyle(false);
        }
    }

    public void updateStyle(ITextViewer viewer, int initialOffset, int len) {

        StyledText text = viewer.getTextWidget();
        if (text == null || text.isDisposed()) {
            return;
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
            repairPresentation(viewer);
            return;
        }

        int offset = widgetCaret;
        int length = initialOffset + len - modelCaret;

        Color foreground = getForegroundColor(text);
        Color background = getBackgroundColor(text);

        StyleRange range = text.getStyleRangeAtOffset(offset);
        int fontStyle = range != null ? range.fontStyle : SWT.NORMAL;

        repairPresentation(viewer);
        fRememberedStyleRange = new StyleRange(offset, length, foreground, background, fontStyle);
        if (range != null) {
            fRememberedStyleRange.strikeout = range.strikeout;
            fRememberedStyleRange.underline = range.underline;
        }

        // http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
        try {
            if (viewer instanceof ICompletionStyleToggleEnabler) {
                ICompletionStyleToggleEnabler pySourceViewer = (ICompletionStyleToggleEnabler) viewer;
                pySourceViewer.setInToggleCompletionStyle(true);
            }
            text.setStyleRange(fRememberedStyleRange);
        } catch (IllegalArgumentException x) {
            // catching exception as offset + length might be outside of the text widget
            fRememberedStyleRange = null;
        }
    }

}
