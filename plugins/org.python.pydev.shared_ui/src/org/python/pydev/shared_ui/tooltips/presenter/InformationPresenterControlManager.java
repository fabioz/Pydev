/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.IWidgetTokenKeeperExtension;
import org.eclipse.jface.text.IWidgetTokenOwner;
import org.eclipse.jface.text.IWidgetTokenOwnerExtension;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;
import org.python.pydev.shared_ui.tooltips.presenter.InformationPresenterHelpers.PyInformationControl;

/**
 * Based on org.eclipse.jface.text.information.InformationPresenter (but without the references to 
 * an org.eclipse.jface.text.information.InformationPresenter nor partitions).
 * 
 * To be used on controls to show tooltips that 'stick' and the user can interact with.
 */
public final class InformationPresenterControlManager extends AbstractInformationControlManager implements
        IWidgetTokenKeeper, IWidgetTokenKeeperExtension, IInformationPresenterControlManager {

    /**
     * The string that should be shown below the tooltip (if not given, a default is provided).
     */
    private final String tooltipAffordanceString;

    /**
     * @param tooltipAffordanceString if null, a default is given.
     */
    public InformationPresenterControlManager(IInformationPresenter presenter, String tooltipAffordanceString) {
        super(new InformationPresenterHelpers.TooltipInformationControlCreator(presenter));
        this.tooltipAffordanceString = tooltipAffordanceString;
        if (presenter instanceof IInformationPresenterAsTooltip) {
            IInformationPresenterAsTooltip presenterAsTooltip = (IInformationPresenterAsTooltip) presenter;
            presenterAsTooltip.setInformationPresenterControlManager(this);
        }
        setCloser(new Closer());
        takesFocusWhenVisible(true);
        ((InformationPresenterHelpers.TooltipInformationControlCreator) this.fInformationControlCreator)
                .setInformationPresenterControlManager(this);
    }

    /**
     * Priority of the info controls managed by this information presenter.
     * Default value: <code>5</code>.
     */
    public static final int WIDGET_PRIORITY = 5;

    /**
     * Internal information control closer. Listens to several events issued by its subject control
     * and closes the information control when necessary.
     */
    class Closer implements IInformationControlCloser, ControlListener, MouseListener, FocusListener, KeyListener,
            MouseMoveListener, Listener {

        /** The subject control. */
        private Control fSubjectControl;
        /** The information control. */
        private PyInformationControl fInformationControlToClose;
        /** Indicates whether this closer is active. */
        private boolean fIsActive = false;
        private Rectangle fShellTooltipArea;
        private Display fDisplay;

        /*
         * @see IInformationControlCloser#setSubjectControl(Control)
         */
        public void setSubjectControl(Control control) {
            fSubjectControl = control;
        }

        /*
         * @see IInformationControlCloser#setInformationControl(IInformationControl)
         */
        public void setInformationControl(IInformationControl control) {
            Assert.isTrue(control == null || control instanceof PyInformationControl);
            fInformationControlToClose = (PyInformationControl) control;
        }

        /*
         * @see IInformationControlCloser#start(Rectangle)
         */
        public void start(Rectangle informationArea) {
            fShellTooltipArea = fInformationControlToClose.getShellTooltipBounds();
            if (fIsActive) {
                return;
            }
            fIsActive = true;

            if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
                fSubjectControl.addControlListener(this);
                fSubjectControl.addMouseListener(this);
                fSubjectControl.addMouseMoveListener(this);
                fSubjectControl.addFocusListener(this);
                fSubjectControl.addKeyListener(this);

                fDisplay = fSubjectControl.getDisplay();
                if (!fDisplay.isDisposed()) {
                    fDisplay.addFilter(SWT.Activate, this);
                    fDisplay.addFilter(SWT.MouseVerticalWheel, this);

                    fDisplay.addFilter(SWT.FocusOut, this);

                    fDisplay.addFilter(SWT.MouseDown, this);
                    fDisplay.addFilter(SWT.MouseUp, this);

                    fDisplay.addFilter(SWT.MouseMove, this);
                    fDisplay.addFilter(SWT.MouseEnter, this);
                    fDisplay.addFilter(SWT.MouseExit, this);

                    fDisplay.addFilter(SWT.KeyDown, this);
                }
            }

            if (fInformationControlToClose != null) {
                fInformationControlToClose.addFocusListener(this);
            }

        }

        /*
         * @see IInformationControlCloser#stop()
         */
        public void stop() {

            if (!fIsActive) {
                return;
            }
            fIsActive = false;

            if (fInformationControlToClose != null) {
                fInformationControlToClose.removeFocusListener(this);
            }

            if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
                fSubjectControl.removeControlListener(this);
                fSubjectControl.removeMouseMoveListener(this);
                fSubjectControl.removeMouseListener(this);
                fSubjectControl.removeFocusListener(this);
                fSubjectControl.removeKeyListener(this);
            }

            if (fDisplay != null && !fDisplay.isDisposed()) {
                fDisplay.removeFilter(SWT.Activate, this);
                fDisplay.removeFilter(SWT.MouseVerticalWheel, this);

                fDisplay.removeFilter(SWT.FocusOut, this);

                fDisplay.removeFilter(SWT.MouseDown, this);
                fDisplay.removeFilter(SWT.MouseUp, this);

                fDisplay.removeFilter(SWT.MouseMove, this);
                fDisplay.removeFilter(SWT.MouseEnter, this);
                fDisplay.removeFilter(SWT.MouseExit, this);
                fDisplay.removeFilter(SWT.KeyDown, this);
            }
            fDisplay = null;

        }

        /*
         * @see ControlListener#controlResized(ControlEvent)
         */
        public void controlResized(ControlEvent e) {
            hideInformationControl();
        }

        /*
         * @see ControlListener#controlMoved(ControlEvent)
         */
        public void controlMoved(ControlEvent e) {
            hideInformationControl();
        }

        /*
         * @see MouseListener#mouseDown(MouseEvent)
         */
        public void mouseDown(MouseEvent e) {
            hideInformationControl();
        }

        /*
         * @see MouseListener#mouseUp(MouseEvent)
         */
        public void mouseUp(MouseEvent e) {
        }

        /*
         * @see MouseListener#mouseDoubleClick(MouseEvent)
         */
        public void mouseDoubleClick(MouseEvent e) {
            hideInformationControl();
        }

        /*
         * @see FocusListener#focusGained(FocusEvent)
         */
        public void focusGained(FocusEvent e) {
        }

        /*
         * @see FocusListener#focusLost(FocusEvent)
         */
        public void focusLost(FocusEvent e) {
            Display d = fSubjectControl.getDisplay();
            d.asyncExec(new Runnable() {
                // Without the asyncExec, mouse clicks to the workbench window are swallowed.
                public void run() {
                    if (fInformationControlToClose == null || !fInformationControlToClose.isFocusControl()) {
                        hideInformationControl();
                    }
                }
            });
        }

        /*
         * @see KeyListener#keyPressed(KeyEvent)
         */
        public void keyPressed(KeyEvent e) {
            hideInformationControl();
        }

        /*
         * @see KeyListener#keyReleased(KeyEvent)
         */
        public void keyReleased(KeyEvent e) {
        }

        public void mouseMove(MouseEvent e) {
            if (fInformationControl != null && fInformationControl.isFocusControl()) {
                if (!inKeepUpZone(e.x, e.y, fSubjectControl, fSubjectControl.getDisplay())) {
                    hideInformationControl();
                }
            }
        }

        public void handleEvent(Event event) {
            switch (event.type) {
                case SWT.Activate:
                case SWT.MouseVerticalWheel:
                case SWT.MouseUp:
                case SWT.MouseDown:
                case SWT.FocusOut:
                    IInformationControl iControl = fInformationControl;
                    if (iControl != null && !iControl.isFocusControl()) {
                        hideInformationControl();
                    }
                    break;

                case SWT.MouseMove:
                case SWT.MouseEnter:
                case SWT.MouseExit:
                    handleMouseMove(event);
                    break;

                case SWT.KeyDown:
                    if (event.keyCode == SWT.ESC) {
                        hideInformationControl();

                    } else if (fActivateEditorBinding != null
                            && KeyBindingHelper.matchesKeybinding(event.keyCode, event.stateMask,
                                    fActivateEditorBinding)) {
                        hideInformationControl(true, true);
                    }
                    break;
            }
        }

        private void handleMouseMove(Event event) {
            if (!(event.widget instanceof Control)) {
                return;
            }
            Control control = (Control) event.widget;
            Display display = control.getDisplay();

            if (!inKeepUpZone(event.x, event.y, control, display)) {
                hideInformationControl();
            }
        }

        /**
         * Note that all parameters (x, y, shellTooltipArea) must be in display coordinates.
         * @param control 
         * @param display 
         */
        private boolean inKeepUpZone(int x, int y, Control control, Display display) {
            if (display.isDisposed()) {
                return true; //received something from a dead display? Let's keep on showing it... (not sure if this actually happens)
            }
            Point point = display.map(control, null, x, y);

            int margin = 20;
            //the bounds are in display coordinates
            Rectangle bounds = Geometry.copy(fShellTooltipArea);

            //expand so that we have some tolerance to keep it open 
            Geometry.expand(bounds, margin, margin, margin, margin);
            return bounds.contains(point.x, point.y);
        }
    }

    private Control fControl;
    private ITooltipInformationProvider fProvider;
    private KeySequence fActivateEditorBinding;
    private Shell fInitiallyActiveShell;
    private Control fFocusControl;
    private boolean onHide;

    public void setInformationProvider(ITooltipInformationProvider provider) {
        this.fProvider = provider;
    }

    /*
     * @see IInformationPresenter#getInformationProvider(String)
     */
    public ITooltipInformationProvider getInformationProvider() {
        return fProvider;
    }

    /*
     * @see AbstractInformationControlManager#computeInformation()
     */
    @Override
    protected void computeInformation() {
        if (fProvider == null) {
            return;
        }

        Object info = fProvider.getInformation(this.fControl);
        Point point = fProvider.getPosition(this.fControl);
        //the width and height are later calculated base on the size of the information to be shown.
        setInformation(info, new Rectangle(point.x, point.y, 0, 0));
    }

    /*
     * @see IInformationPresenter#uninstall()
     */
    public void uninstall() {
        dispose();
    }

    /*
     * @see AbstractInformationControlManager#showInformationControl(Rectangle)
     */
    @Override
    protected void showInformationControl(Rectangle subjectArea) {
        if (fControl instanceof IWidgetTokenOwnerExtension && fControl instanceof IWidgetTokenOwner) {
            IWidgetTokenOwnerExtension extension = (IWidgetTokenOwnerExtension) fControl;
            if (extension.requestWidgetToken(this, WIDGET_PRIORITY)) {
                super.showInformationControl(subjectArea);
            }
        } else if (fControl instanceof IWidgetTokenOwner) {
            IWidgetTokenOwner owner = (IWidgetTokenOwner) fControl;
            if (owner.requestWidgetToken(this)) {
                super.showInformationControl(subjectArea);
            }

        } else {
            super.showInformationControl(subjectArea);
        }
    }

    @Override
    public void hideInformationControl() {
        hideInformationControl(false, true);
    }

    /*
     * @see AbstractInformationControlManager#hideInformationControl()
     */
    public void hideInformationControl(boolean activateEditor, boolean restoreFocus) {
        //When hiding it may call hide again (because as it gets hidden our handlers are still connected).
        if (this.onHide) {
            return;
        }
        this.onHide = true;
        try {
            try {
                super.hideInformationControl();
            } finally {
                if (fControl instanceof IWidgetTokenOwner) {
                    IWidgetTokenOwner owner = (IWidgetTokenOwner) fControl;
                    owner.releaseWidgetToken(this);
                }
            }
            this.disposeInformationControl();

            //Restore previous active shell?
            if (this.fInitiallyActiveShell != null && !this.fInitiallyActiveShell.isDisposed()) {
                if (restoreFocus) {
                    this.fInitiallyActiveShell.setActive();
                }
                this.fInitiallyActiveShell = null;
            }

            if (this.fFocusControl != null && !this.fFocusControl.isDisposed()) {
                if (restoreFocus) {
                    this.fFocusControl.setFocus();
                }
                this.fFocusControl = null;
            }

            if (activateEditor) {
                KeyBindingHelper.executeCommand("org.eclipse.ui.window.activateEditor");
            }
        } finally {
            this.onHide = false;
        }

    }

    /*
     * @see AbstractInformationControlManager#handleInformationControlDisposed()
     */
    @Override
    protected void handleInformationControlDisposed() {
        try {
            super.handleInformationControlDisposed();
        } finally {
            if (fControl instanceof IWidgetTokenOwner) {
                IWidgetTokenOwner owner = (IWidgetTokenOwner) fControl;
                owner.releaseWidgetToken(this);
            }
        }
    }

    /*
     * @see org.eclipse.jface.text.IWidgetTokenKeeper#requestWidgetToken(IWidgetTokenOwner)
     */
    public boolean requestWidgetToken(IWidgetTokenOwner owner) {
        return false;
    }

    /*
     * @see org.eclipse.jface.text.IWidgetTokenKeeperExtension#requestWidgetToken(org.eclipse.jface.text.IWidgetTokenOwner, int)
     * @since 3.0
     */
    public boolean requestWidgetToken(IWidgetTokenOwner owner, int priority) {
        return false;
    }

    /*
     * @see org.eclipse.jface.text.IWidgetTokenKeeperExtension#setFocus(org.eclipse.jface.text.IWidgetTokenOwner)
     * @since 3.0
     */
    public boolean setFocus(IWidgetTokenOwner owner) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_ui.tooltips.presenter.IInformationPresenterControlManager#setActivateEditorBinding(org.eclipse.jface.bindings.keys.KeySequence)
     */
    public void setActivateEditorBinding(KeySequence activateEditorBinding) {
        fActivateEditorBinding = activateEditorBinding;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_ui.tooltips.presenter.IInformationPresenterControlManager#setInitiallyActiveShell(org.eclipse.swt.widgets.Shell)
     */
    public void setInitiallyActiveShell(Shell activeShell) {
        this.fInitiallyActiveShell = activeShell;
        this.fFocusControl = null;
        if (activeShell != null) {
            Display display = activeShell.getDisplay();
            if (display != null) {
                this.fFocusControl = display.getFocusControl();
            }
        }
    }

    public String getTooltipAffordanceString() {
        if (tooltipAffordanceString != null) {
            return tooltipAffordanceString;
        }
        String defaultStr = "ESC to close, ENTER activate link.";
        if (this.fActivateEditorBinding != null) {
            return StringUtils.format("%s to activate editor, %s", fActivateEditorBinding.toString(), defaultStr);
        }
        return defaultStr;
    }

}
