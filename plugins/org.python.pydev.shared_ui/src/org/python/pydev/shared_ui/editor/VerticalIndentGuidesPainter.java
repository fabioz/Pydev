/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.python.pydev.shared_core.log.Log;

public class VerticalIndentGuidesPainter implements PaintListener, ModifyListener, ExtendedModifyListener,
        TextChangeListener {

    private StyledText styledText;
    private boolean inDraw;
    private Rectangle currClientArea;
    private int currCharCount;
    private Map<Integer, List<VerticalLinesToDraw>> lineToVerticalLinesToDraw;
    private StyledTextContent content;
    private final IVerticalLinesIndentGuideComputer indentGuide;
    private int lastXOffset = -1;
    private int lastYOffset = -1;
    private int currTabWidth = -1;

    public VerticalIndentGuidesPainter(IVerticalLinesIndentGuideComputer indentGuide) {
        Assert.isNotNull(indentGuide);
        this.indentGuide = indentGuide;
    }

    @Override
    public void paintControl(PaintEvent e) {
        if (inDraw || styledText == null || styledText.isDisposed()) {
            return;
        }
        try {
            inDraw = true;
            int xOffset = styledText.getHorizontalPixel();
            int yOffset = styledText.getTopPixel();

            //Important: call all to cache the new values (instead of doing all inside the or below).
            boolean styledTextContentChanged = getStyledTextContentChangedAndStoreNew();
            boolean clientAreaChanged = getClientAreaChangedAndStoreNew();
            boolean charCountChanged = getCharCountChangedAndStoreNew();
            boolean tabWidthChanged = getTabWidthChangedAndStoreNew();

            boolean redrawAll = styledTextContentChanged || clientAreaChanged || charCountChanged || tabWidthChanged
                    || xOffset != lastXOffset || yOffset != lastYOffset;

            StyledTextContent currentContent = this.content;
            if (currClientArea == null || currClientArea.width < 5 || currClientArea.height < 5 || currCharCount < 1
                    || currentContent == null || currTabWidth <= 0) {
                Image oldImage = styledText.getBackgroundImage();
                if (oldImage != null) {
                    styledText.setBackgroundImage(null);
                    oldImage.dispose();
                }
                return;
            }
            lastXOffset = xOffset;
            lastYOffset = yOffset;

            int topIndex = JFaceTextUtil.getPartialTopIndex(styledText);
            int bottomIndex = JFaceTextUtil.getPartialBottomIndex(styledText);
            if (redrawAll) {
                this.lineToVerticalLinesToDraw = internalPaintStyledTextRegion(e, topIndex, bottomIndex);
            }
            if (this.lineToVerticalLinesToDraw != null) {
                // The caret line must always be redrawn anyways in the current e.gc.
                int caretOffset = styledText.getCaretOffset();
                int caretLine = currentContent.getLineAtOffset(caretOffset);
                List<VerticalLinesToDraw> list = lineToVerticalLinesToDraw.get(caretLine);
                if (list != null) {
                    try (AutoCloseable temp = configGC(e.gc)) {
                        for (VerticalLinesToDraw next : list) {
                            next.drawLine(e.gc);
                        }
                    }
                }
            }
        } catch (Exception e1) {
            Log.log(e1);
        } finally {
            inDraw = false;
        }
    }

    private boolean getStyledTextContentChangedAndStoreNew() {
        StyledTextContent currentContent = this.styledText.getContent();
        StyledTextContent oldContent = this.content;
        if (currentContent != oldContent) {
            //Important: the content may change during runtime, so, we have to stop listening the old one and
            //start listening the new one.
            if (oldContent != null) {
                oldContent.removeTextChangeListener(this);
            }
            this.content = currentContent;
            currentContent.addTextChangeListener(this);
            return true;
        }
        return false;
    }

    private AutoCloseable configGC(final GC gc) {
        final int lineStyle = gc.getLineStyle();
        final int alpha = gc.getAlpha();
        final int[] lineDash = gc.getLineDash();

        gc.setAlpha(125);
        gc.setLineStyle(SWT.LINE_CUSTOM);
        gc.setLineDash(new int[] { 1, 2 });
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                gc.setAlpha(alpha);
                gc.setLineStyle(lineStyle);
                gc.setLineDash(lineDash);
            }
        };
    }

    boolean getClientAreaChangedAndStoreNew() {
        Rectangle clientArea = styledText.getClientArea();
        if (currClientArea == null || !currClientArea.equals(clientArea)) {
            currClientArea = clientArea;
            return true;
        }
        return false;
    }

    boolean getCharCountChangedAndStoreNew() {
        int charCount = styledText.getCharCount();
        if (currCharCount != charCount) {
            currCharCount = charCount;
            return true;
        }
        return false;
    }

    boolean getTabWidthChangedAndStoreNew() {
        int tabWidth = indentGuide.getTabWidth();
        if (currTabWidth != tabWidth) {
            currTabWidth = tabWidth;
            return true;
        }
        return false;
    }

    /**
     * Here we'll paint the styled text background image with the indent guides.
     */
    public SortedMap<Integer, List<VerticalLinesToDraw>> internalPaintStyledTextRegion(PaintEvent e, int topIndex,
            int bottomIndex) {
        SortedMap<Integer, List<VerticalLinesToDraw>> lineToVerticalLinesToDraw = this.indentGuide
                .computeVerticalLinesToDrawInRegion(styledText, topIndex, bottomIndex);

        try {
            //note: at this point we know we have at least 2 lines (styledText.getLineCount)

            Image newImage = null;
            GC gc = null;
            newImage = new Image(null, currClientArea.width, currClientArea.height);
            gc = new GC(newImage);
            try {
                Rectangle rec = newImage.getBounds();
                gc.setBackground(styledText.getBackground());
                gc.setForeground(styledText.getForeground());
                gc.fillRectangle(rec.x, rec.y, rec.width, rec.height);
                try (AutoCloseable temp = configGC(gc)) {
                    Collection<List<VerticalLinesToDraw>> values = lineToVerticalLinesToDraw.values();
                    for (List<VerticalLinesToDraw> list : values) {
                        for (VerticalLinesToDraw verticalLinesToDraw : list) {
                            verticalLinesToDraw.drawLine(gc);
                        }
                    }
                }
                Image oldImage = styledText.getBackgroundImage();
                styledText.setBackgroundImage(newImage);
                if (oldImage != null) {
                    oldImage.dispose();
                }
            } finally {
                gc.dispose();
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
        return lineToVerticalLinesToDraw;
    }

    public void setStyledText(StyledText styledText) {
        if (this.styledText != null) {
            this.styledText.removeModifyListener(this);
            this.styledText.removeExtendedModifyListener(this);
            if (this.content != null) {
                this.content.removeTextChangeListener(this);
            }
        }
        this.styledText = styledText;
        this.content = this.styledText.getContent();

        this.styledText.addModifyListener(this);
        this.styledText.addExtendedModifyListener(this);
        this.content.addTextChangeListener(this);
    }

    @Override
    public void modifyText(ModifyEvent e) {
        this.currClientArea = null; //will force redrawing everything
    }

    @Override
    public void modifyText(ExtendedModifyEvent event) {
        this.currClientArea = null; //will force redrawing everything
    }

    @Override
    public void textChanging(TextChangingEvent event) {
        this.currClientArea = null; //will force redrawing everything
    }

    @Override
    public void textChanged(TextChangedEvent event) {
        this.currClientArea = null; //will force redrawing everything
    }

    @Override
    public void textSet(TextChangedEvent event) {
        this.currClientArea = null; //will force redrawing everything
    }

}
