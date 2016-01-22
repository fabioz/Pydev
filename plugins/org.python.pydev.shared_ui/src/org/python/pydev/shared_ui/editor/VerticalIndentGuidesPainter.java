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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class VerticalIndentGuidesPainter implements PaintListener, ModifyListener, ExtendedModifyListener,
        TextChangeListener, DisposeListener {

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
    private boolean askFullRedraw = true; //On the first one always make it full

    /**
     * Note: dispose doesn't need to be explicitly called (it'll be disposed when
     * the StyledText set at setStyledText is disposed). Still, calling it more than
     * once should be ok.
     */
    public void dispose() {
        styledText = null;
        currClientArea = null;
        lineToVerticalLinesToDraw = null;
        content = null;
        indentGuide.dispose();
    }

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
            boolean showIndentGuide = this.indentGuide.getShowIndentGuide();
            if (!showIndentGuide) {
                return;
            }

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
                return;
            }
            lastXOffset = xOffset;
            lastYOffset = yOffset;

            int topIndex;
            try {
                topIndex = JFaceTextUtil.getPartialTopIndex(styledText);
            } catch (IllegalArgumentException e1) {
                // Just silence it...
                // java.lang.IllegalArgumentException: Index out of bounds
                // at org.eclipse.swt.SWT.error(SWT.java:4458)
                // at org.eclipse.swt.SWT.error(SWT.java:4392)
                // at org.eclipse.swt.SWT.error(SWT.java:4363)
                // at org.eclipse.swt.custom.StyledText.getOffsetAtLine(StyledText.java:4405)
                // at org.eclipse.jface.text.JFaceTextUtil.getPartialTopIndex(JFaceTextUtil.java:103)
                // at org.python.pydev.shared_ui.editor.VerticalIndentGuidesPainter.paintControl(VerticalIndentGuidesPainter.java:93)
                return;
            }
            int bottomIndex = JFaceTextUtil.getPartialBottomIndex(styledText);
            if (redrawAll) {
                this.lineToVerticalLinesToDraw = this.indentGuide.computeVerticalLinesToDrawInRegion(styledText,
                        topIndex, bottomIndex);
                // This is a bit unfortunate: when something changes, we may have to repaint out of the clipping
                // region, but even setting the clipping region (e.gc.setClipping), the clipping region may still
                // be unchanged (because the system said that it only wants to repaint some specific area already
                // and we can't make it bigger -- so, what's left for us is asking for a repaint of the full area
                // in this case).
                if (askFullRedraw) {
                    askFullRedraw = false;
                    if (Math.abs(currClientArea.height - e.gc.getClipping().height) > 40) {
                        //Only do it if the difference is really high (some decorations make it usually a bit lower than
                        //the actual client area -- usually around 14 in my tests, but make it a bit higher as the usual
                        //difference when a redraw is needed is pretty high).
                        RunInUiThread.async(new Runnable() {

                            @Override
                            public void run() {
                                StyledText s = styledText;
                                if (s != null && !s.isDisposed()) {
                                    s.redraw();
                                }
                            }
                        });
                    } else {
                    }
                }
            }

            if (this.lineToVerticalLinesToDraw != null) {
                try (AutoCloseable temp = configGC(e.gc)) {
                    Collection<List<VerticalLinesToDraw>> values = lineToVerticalLinesToDraw.values();
                    for (List<VerticalLinesToDraw> list : values) {
                        for (VerticalLinesToDraw verticalLinesToDraw : list) {
                            verticalLinesToDraw.drawLine(e.gc);
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

        final Color foreground = gc.getForeground();
        final Color background = gc.getBackground();

        gc.setForeground(this.indentGuide.getColor(styledText));
        gc.setBackground(styledText.getBackground());
        gc.setAlpha(this.indentGuide.getTransparency());
        gc.setLineStyle(SWT.LINE_CUSTOM);
        gc.setLineDash(new int[] { 1, 2 });
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                gc.setForeground(foreground);
                gc.setBackground(background);
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

    @Override
    public void widgetDisposed(DisposeEvent e) {
        this.dispose();
    }

    public void setStyledText(StyledText styledText) {
        if (this.styledText != null) {
            this.styledText.removeModifyListener(this);
            this.styledText.removeExtendedModifyListener(this);
            if (this.content != null) {
                this.content.removeTextChangeListener(this);
            }
            this.styledText.removeDisposeListener(this);
        }
        this.styledText = styledText;
        this.content = this.styledText.getContent();

        this.styledText.addModifyListener(this);
        this.styledText.addExtendedModifyListener(this);
        this.content.addTextChangeListener(this);
        this.styledText.addDisposeListener(this);
    }

    @Override
    public void modifyText(ModifyEvent e) {
        this.currClientArea = null; //will force redrawing everything
        askFullRedraw = true;
    }

    @Override
    public void modifyText(ExtendedModifyEvent event) {
        this.currClientArea = null; //will force redrawing everything
        askFullRedraw = true;
    }

    @Override
    public void textChanging(TextChangingEvent event) {
        this.currClientArea = null; //will force redrawing everything
        askFullRedraw = true;
    }

    @Override
    public void textChanged(TextChangedEvent event) {
        this.currClientArea = null; //will force redrawing everything
        askFullRedraw = true;
    }

    @Override
    public void textSet(TextChangedEvent event) {
        this.currClientArea = null; //will force redrawing everything
        askFullRedraw = true;
    }

}
