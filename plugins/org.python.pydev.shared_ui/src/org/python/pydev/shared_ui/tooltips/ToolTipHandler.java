/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

/**
 * This shows a tooltip. For showing a tooltip as an information presenter (which can handle
 * links and the user can interact with it, use org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler)
 * 
 * 
 * Based on http://demo.spars.info/j/frameset.cgi?compo_id=146467&q=mouseexit&hl=mouseexit&packagename=org.eclipse.swt.examples.hoverhelp&componame=org.eclipse.swt.examples.hoverhelp.HoverHelp$ToolTipPresenterHandler&CASE=0&MORPHO=1&location=1111111111111111111&ref=1&mode=frameset&LANG=1
 * 
 * Emulated tooltip handler
 * Notice that we could display anything in a tooltip besides text and images.
 * For instance, it might make sense to embed large tables of data or buttons linking
 * data under inspection to material elsewhere, or perform dynamic lookup for creating
 * tooltip text on the fly.
 */
public class ToolTipHandler {
    public static final String TIP_DATA = "TIP_DATA";
    public static final String TIP_IMAGE = "TIP_IMAGE";

    private Shell parentShell;
    private Shell tipShell;
    private Label tipLabelImage, tipLabelText;
    private Widget tipWidget; // widget this tooltip is hovering over
    private Point tipPosition; // the position being hovered over

    /**
     * Creates a new tooltip handler
     *
     * @param parent the parent Shell
     */
    public ToolTipHandler(Shell parent) {
        final Display display = parent.getDisplay();
        this.parentShell = parent;

        tipShell = new Shell(parent, SWT.ON_TOP | SWT.TOOL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = 2;
        tipShell.setLayout(gridLayout);

        tipShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        tipLabelImage = new Label(tipShell, SWT.NONE);
        tipLabelImage.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        tipLabelImage.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        tipLabelImage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

        tipLabelText = new Label(tipShell, SWT.NONE);
        tipLabelText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        tipLabelText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        tipLabelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
    }

    /**
     * Enables customized hover help for a specified control
     * 
     * @control the control on which to enable hoverhelp
     */
    public void install(final Control control) {
        /*
         * Get out of the way if we attempt to activate the control underneath the tooltip
         */
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (tipShell.isVisible())
                    tipShell.setVisible(false);
            }
        });

        /*
         * Trap hover events to pop-up tooltip
         */
        control.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                if (tipShell.isVisible())
                    tipShell.setVisible(false);
                tipWidget = null;
            }

            @Override
            public void mouseHover(MouseEvent event) {
                Point pt = new Point(event.x, event.y);
                Widget widget = event.widget;
                if (widget instanceof ToolBar) {
                    ToolBar w = (ToolBar) widget;
                    widget = w.getItem(pt);
                }
                if (widget instanceof Table) {
                    Table w = (Table) widget;
                    widget = w.getItem(pt);
                }
                if (widget instanceof Tree) {
                    Tree w = (Tree) widget;
                    widget = w.getItem(pt);
                }
                if (widget == null) {
                    tipShell.setVisible(false);
                    tipWidget = null;
                    return;
                }
                if (widget == tipWidget)
                    return;
                tipWidget = widget;
                tipPosition = control.toDisplay(pt);
                String text = (String) widget.getData(TIP_DATA);
                Image image = (Image) widget.getData(TIP_IMAGE);
                tipLabelText.setText(text != null ? text : "");
                tipLabelImage.setImage(image); // accepts null
                tipShell.pack();
                setHoverLocation(tipShell, tipPosition);
                tipShell.setVisible(true);
            }
        });

        /*
         * Trap F1 Help to pop up a custom help box
         */
        control.addHelpListener(new HelpListener() {
            @Override
            public void helpRequested(HelpEvent event) {
                if (tipWidget == null)
                    return;
                ToolTipHelpTextHandler handler = (ToolTipHelpTextHandler) tipWidget.getData("TIP_HELPTEXTHANDLER");
                if (handler == null)
                    return;
                String text = handler.getHelpText(tipWidget);
                if (text == null)
                    return;

                if (tipShell.isVisible()) {
                    tipShell.setVisible(false);
                    Shell helpShell = new Shell(parentShell, SWT.SHELL_TRIM);
                    helpShell.setLayout(new FillLayout());
                    Label label = new Label(helpShell, SWT.NONE);
                    label.setText(text);
                    helpShell.pack();
                    setHoverLocation(helpShell, tipPosition);
                    helpShell.open();
                }
            }
        });
    }

    /**
     * Sets the location for a hovering shell
     * @param shell the object that is to hover
     * @param position the position of a widget to hover over
     * @return the top-left location for a hovering box
     */
    private void setHoverLocation(Shell shell, Point position) {
        Rectangle displayBounds = shell.getDisplay().getBounds();
        Rectangle shellBounds = shell.getBounds();
        shellBounds.x = Math.max(Math.min(position.x, displayBounds.width - shellBounds.width), 0);
        shellBounds.y = Math.max(Math.min(position.y + 16, displayBounds.height - shellBounds.height), 0);
        shell.setBounds(shellBounds);
    }
}

/**
 * ToolTip help handler
 */
interface ToolTipHelpTextHandler {
    /**
     * Get help text
     * @param widget the widget that is under help
     * @return a help text string
     */
    public String getHelpText(Widget widget);
}
