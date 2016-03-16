/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
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
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;
import org.python.pydev.shared_ui.utils.UIUtils;

/**
 * Shows tooltips as an information presenter, so, links can be added and the user can interact with it.
 * 
 * Based on http://demo.spars.info/j/frameset.cgi?compo_id=146467&q=mouseexit&hl=mouseexit&packagename=org.eclipse.swt.examples.hoverhelp&componame=org.eclipse.swt.examples.hoverhelp.HoverHelp$ToolTipPresenterHandler&CASE=0&MORPHO=1&location=1111111111111111111&ref=1&mode=frameset&LANG=1
 * 
 * Emulated tooltip handler
 * Notice that we could display anything in a tooltip besides text and images.
 * For instance, it might make sense to embed large tables of data or buttons linking
 * data under inspection to material elsewhere, or perform dynamic lookup for creating
 * tooltip text on the fly.
 */
public class ToolTipPresenterHandler {

    public static final String TIP_DATA = "TIP_DATA";
    private Shell tipShell;
    private Label tipLabelImage, tipLabelText;
    private Widget tipWidget; // widget this tooltip is hovering over
    private IInformationPresenterControlManager informationPresenterManager;
    private IInformationPresenter presenter;

    public ToolTipPresenterHandler(Shell parent) {
        this(parent, null);

    }

    public ToolTipPresenterHandler(Shell parent, IInformationPresenter presenter) {
        this(parent, presenter, null);
    }

    /**
     * Creates a new tooltip handler
     *
     * @param parent the parent Shell
     */
    public ToolTipPresenterHandler(Shell parent, IInformationPresenter presenter, String tooltipAffordanceString) {
        this.presenter = presenter;
        informationPresenterManager = new InformationPresenterControlManager(presenter, tooltipAffordanceString);
    }

    private void disposeOfCurrentTipShell() {
        if (tipShell != null) {
            tipShell.dispose();
            tipShell = null;
        }
        tipWidget = null;
    }

    /**
     * Enables customized hover help for a specified control
     * 
     * @control the control on which to enable hoverhelp
     */
    public void install(final Control control) {

        informationPresenterManager.install(control);

        /*
         * Get out of the way if we attempt to activate the control underneath the tooltip
         */
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                disposeOfCurrentTipShell();
            }
        });

        /*
         * Trap hover events to pop-up tooltip
         */
        control.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                disposeOfCurrentTipShell();
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
                    disposeOfCurrentTipShell();
                    return;
                }
                if (widget == tipWidget) {
                    return;
                }

                tipWidget = widget;
                Object data = widget.getData(TIP_DATA);
                if (data == null) {
                    return;
                }
                final String text;
                if (data instanceof String) {
                    text = (String) data;
                } else {
                    text = data.toString();
                }
                if (text == null) {
                    return;
                }

                //It must be set before showing the tooltip, as we'll loose the focus to the tooltip and the
                //currently active bindings will become inactive.
                KeySequence activateEditorBinding = KeyBindingHelper
                        .getCommandKeyBinding("org.eclipse.ui.window.activateEditor");
                informationPresenterManager.setActivateEditorBinding(activateEditorBinding);
                Shell activeShell = UIUtils.getActiveShell();
                informationPresenterManager.setInitiallyActiveShell(activeShell);

                createControls();

                final Point pos = new Point(pt.x + 10, pt.y);
                ITooltipInformationProvider provider = new ITooltipInformationProvider() {

                    @Override
                    public Object getInformation(Control fControl) {
                        return text;
                    }

                    @Override
                    public Point getPosition(Control fControl) {
                        return pos;
                    }
                };

                if (presenter instanceof IInformationPresenterAsTooltip) {
                    IInformationPresenterAsTooltip iInformationPresenterAsTooltip = (IInformationPresenterAsTooltip) presenter;
                    iInformationPresenterAsTooltip.setData(data);
                }
                informationPresenterManager.setInformationProvider(provider);
                informationPresenterManager.showInformation();

            }
        });

    }

    private void createControls() {
        Display display = UIUtils.getDisplay();

        if (tipShell != null) {
            return;
        }

        tipShell = new Shell(display, SWT.ON_TOP | SWT.TOOL);
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

}
