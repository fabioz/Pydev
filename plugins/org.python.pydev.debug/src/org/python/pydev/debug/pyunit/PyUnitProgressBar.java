/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.debug.newconsole.prefs.ColorManager;

/**
 * A progress bar with a red/green indication for success or failure.
 * 
 * Copied from org.eclipse.jdt.internal.junit.ui.JUnitProgressBar.
 */
public class PyUnitProgressBar extends Canvas {
    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 18;

    private int fCurrentTickCount = 0;
    private int fMaxTickCount = 0;
    private int fColorBarWidth = 0;
    private Color fOKColor;
    private Color fFailureColor;
    private Color fStoppedColor;
    private boolean fError;
    private boolean fStopped = false;

    public PyUnitProgressBar(Composite parent) {
        super(parent, SWT.NONE);

        addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                fColorBarWidth = scale(fCurrentTickCount);
                redraw();
            }
        });
        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                paint(e);
            }
        });
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                fFailureColor.dispose();
                fOKColor.dispose();
                fStoppedColor.dispose();
            }
        });
        Display display = parent.getDisplay();
        fOKColor = new Color(display, 95, 191, 95);
        fStoppedColor = new Color(display, 120, 120, 120);
        updateErrorColor(false);
    }

    public void updateErrorColor(boolean redraw) {
        if (fFailureColor != null) {
            fFailureColor.dispose();
            fFailureColor = null;
        }
        TextAttribute attribute = ColorManager.getDefault().getConsoleErrorTextAttribute();

        fFailureColor = new Color(Display.getDefault(), attribute.getForeground().getRGB());
        if (redraw) {
            this.redraw();
        }
    }

    public void setMaximum(int max) {
        fMaxTickCount = max;
    }

    public void reset() {
        fError = false;
        fStopped = false;
        fCurrentTickCount = 0;
        fMaxTickCount = 0;
        fColorBarWidth = 0;
        redraw();
    }

    public void reset(boolean hasErrors, boolean stopped, int ticksDone, int maximum) {
        boolean noChange = fError == hasErrors && fStopped == stopped && fCurrentTickCount == ticksDone
                && fMaxTickCount == maximum;
        fError = hasErrors;
        fStopped = stopped;
        fCurrentTickCount = ticksDone;
        fMaxTickCount = maximum;
        fColorBarWidth = scale(ticksDone);
        if (!noChange) {
            redraw();
        }
    }

    private void paintStep(int startX, int endX) {
        GC gc = new GC(this);
        setStatusColor(gc);
        Rectangle rect = getClientArea();
        startX = Math.max(1, startX);
        gc.fillRectangle(startX, 1, endX - startX, rect.height - 2);
        gc.dispose();
    }

    private void setStatusColor(GC gc) {
        if (fStopped) {
            gc.setBackground(fStoppedColor);
        } else if (fError) {
            gc.setBackground(fFailureColor);
        } else {
            gc.setBackground(fOKColor);
        }
    }

    public void stopped() {
        fStopped = true;
        redraw();
    }

    private int scale(int value) {
        if (fMaxTickCount > 0) {
            Rectangle r = getClientArea();
            if (r.width != 0)
                return Math.max(0, value * (r.width - 2) / fMaxTickCount);
        }
        return value;
    }

    private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
        gc.setForeground(topleft);
        gc.drawLine(x, y, x + w - 1, y);
        gc.drawLine(x, y, x, y + h - 1);

        gc.setForeground(bottomright);
        gc.drawLine(x + w, y, x + w, y + h);
        gc.drawLine(x, y + h, x + w, y + h);
    }

    private void paint(PaintEvent event) {
        GC gc = event.gc;
        Display disp = getDisplay();

        Rectangle rect = getClientArea();
        gc.fillRectangle(rect);
        drawBevelRect(gc, rect.x, rect.y, rect.width - 1, rect.height - 1,
                disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
                disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

        setStatusColor(gc);
        fColorBarWidth = Math.min(rect.width - 2, fColorBarWidth);
        gc.fillRectangle(1, 1, fColorBarWidth, rect.height - 2);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        Point size = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        if (wHint != SWT.DEFAULT)
            size.x = wHint;
        if (hHint != SWT.DEFAULT)
            size.y = hHint;
        return size;
    }

    public void step(int failures) {
        fCurrentTickCount++;
        int x = fColorBarWidth;

        fColorBarWidth = scale(fCurrentTickCount);

        if (!fError && failures > 0) {
            fError = true;
            x = 1;
        }
        if (fCurrentTickCount == fMaxTickCount)
            fColorBarWidth = getClientArea().width - 1;
        paintStep(x, fColorBarWidth);
    }

    public void refresh(boolean hasErrors) {
        fError = hasErrors;
        redraw();
    }

    public boolean getHasErrors() {
        return fError;
    }

    public boolean getHasFinished() {
        return this.fMaxTickCount <= this.fCurrentTickCount;
    }

}
