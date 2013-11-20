/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;

/**
 * @author fabioz
 *
 */
public class PinHistoryAction extends Action {

    private WeakReference<PyUnitView> view;
    private PyUnitTestRun currentTestRun;
    public final CallbackWithListeners<PyUnitTestRun> onRunSelected;

    /**
     * @param pyUnitView
     */
    public PinHistoryAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        setInitialTooltipText();
        this.setImageDescriptor(PydevDebugPlugin.getImageCache().getDescriptor("icons/pin.png"));
        this.setChecked(false);
        this.currentTestRun = null;
        this.onRunSelected = new CallbackWithListeners<PyUnitTestRun>();
    }

    private void setInitialTooltipText() {
        this.setToolTipText("Click to mark the currently selected run as the base-run.");
    }

    public PyUnitTestRun getCurrentTestRun() {
        return currentTestRun;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        boolean worked = false;
        try {
            if (this.isChecked()) {
                if (view == null) {
                    return;
                }
                PyUnitView pyUnitView = view.get();
                if (pyUnitView == null) {
                    return;
                }
                PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
                if (currentTestRun != null) {
                    worked = true;
                    onRunSelected.call(currentTestRun);
                    this.currentTestRun = currentTestRun;
                    this.setImageDescriptor(PydevDebugPlugin.getImageCache().getDescriptor("icons/pin_arrow.png"));
                    this.setToolTipText("Currently pin: " + currentTestRun.name + ". Click again to unpin.");
                }
            }

        } finally {
            if (!worked) {
                this.setImageDescriptor(PydevDebugPlugin.getImageCache().getDescriptor("icons/pin.png"));
                this.setInitialTooltipText();
                this.setChecked(false);
                currentTestRun = null;
                onRunSelected.call(currentTestRun);
            }
        }
    }

}
