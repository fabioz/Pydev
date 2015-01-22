package org.python.pydev.debug.console;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.python.pydev.core.log.Log;

class PromptOverlay implements DisposeListener, Listener {

    private StyledText interactiveConsole;
    private StyledText styledText;
    private Layout originalParentLayout;
    private Composite styledTextParent;
    private CustomPageBookLayout customLayout;

    public PromptOverlay(IOConsolePage consolePage) {
        TextConsoleViewer viewer = consolePage.getViewer();
        final StyledText styledText = (StyledText) viewer.getControl();
        this.styledText = styledText;
        styledTextParent = styledText.getParent();
        originalParentLayout = styledTextParent.getLayout();
        final StyledText interactiveConsole = new StyledText(styledTextParent, SWT.None);
        this.customLayout = new CustomPageBookLayout();
        this.interactiveConsole = interactiveConsole;

        styledText.addDisposeListener(this);
        styledText.addListener(SWT.Hide, this);
        styledText.addListener(SWT.Show, this);
        styledText.addListener(SWT.Paint, this);
        styledText.addListener(SWT.Resize, this);
        styledText.addListener(SWT.Selection, this);
        adjust();
    }

    @Override
    public void widgetDisposed(DisposeEvent e) {
        try {
            styledText = null;
            if (interactiveConsole != null) {
                interactiveConsole.setVisible(false);
                interactiveConsole.dispose();
                interactiveConsole = null;
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
        try {
            if (!styledTextParent.isDisposed()) {
                styledTextParent.setLayout(originalParentLayout);
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
    }

    @Override
    public void handleEvent(Event event) {
        adjust();
    }

    private void adjust() {
        System.out.println("Adjust");
        if (styledText != null && !styledText.isDisposed() && styledText.isVisible()) {
            if (styledTextParent.getLayout() == originalParentLayout) {
                styledTextParent.setLayout(customLayout);
                styledTextParent.layout(true);
            }
            if (!interactiveConsole.isVisible()) {
                interactiveConsole.setVisible(true);
            }
            if (!interactiveConsole.getBackground().equals(styledText.getBackground())) {
                interactiveConsole.setBackground(styledText.getBackground());
            }
            //            if (!interactiveConsole.getForeground().equals(styledText.getForeground())) {
            //                interactiveConsole.setForeground(styledText.getForeground());
            //            }
            interactiveConsole.setForeground(new Color(Display.getCurrent(), new RGB(255, 255, 255)));
            if (!interactiveConsole.getFont().equals(styledText.getFont())) {
                interactiveConsole.setFont(styledText.getFont());
            }
        } else {
            if (styledTextParent.getLayout() != originalParentLayout) {
                styledTextParent.setLayout(originalParentLayout);
            }
            if (interactiveConsole.isVisible()) {
                interactiveConsole.setVisible(false);
            }
        }
    }

    private class CustomPageBookLayout extends Layout {

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint,
                boolean flushCache) {
            if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
                return new Point(wHint, hHint);
            }

            Point result = null;
            if (styledText != null) {
                result = styledText.computeSize(wHint, hHint, flushCache);
            } else {
                result = new Point(0, 0);
            }
            if (wHint != SWT.DEFAULT) {
                result.x = wHint;
            }
            if (hHint != SWT.DEFAULT) {
                result.y = hHint;
            }
            return result;
        }

        @Override
        protected void layout(Composite composite, boolean flushCache) {
            if (styledText != null && !styledText.isDisposed()) {
                Rectangle bounds = composite.getClientArea();

                interactiveConsole.setBounds(bounds.x, bounds.y + bounds.height - 50, bounds.width, bounds.height - 50);
                styledText.setBounds(bounds.x, bounds.y, bounds.width, bounds.height - 50);
                System.out.println(interactiveConsole.isVisible());
            }
        }
    }

}