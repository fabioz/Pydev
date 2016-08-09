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
import org.python.pydev.shared_ui.ImageCache;

/**
 * @author fabioz
 *
 */
public class PinHistoryAction extends Action {

    private WeakReference<PyUnitView> view;

    /**
     * @param pyUnitView
     */
    public PinHistoryAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        updateState();
    }

    private void setInitialTooltipText() {
        this.setToolTipText("Click to mark the currently selected run as the base-run.");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
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
                    PyUnitViewTestsHolder.setCurrentPinned(currentTestRun);
                }
            } else {
                PyUnitViewTestsHolder.setCurrentPinned(null);
            }
        } finally {
            updateState();
        }
    }

    private void updateState() {
        PyUnitTestRun currentPinned = PyUnitViewTestsHolder.getCurrentPinned();
        ImageCache imageCache = PydevDebugPlugin.getImageCache();
        if (currentPinned == null) {
            if (imageCache != null) {
                this.setImageDescriptor(imageCache.getDescriptor("icons/pin.png"));
            }
            this.setInitialTooltipText();
            this.setChecked(false);
        } else {
            if (imageCache != null) {
                this.setImageDescriptor(imageCache.getDescriptor("icons/pin_arrow.png"));
            }
            this.setToolTipText("Currently pin: " + currentPinned.name + ". Click again to unpin.");
            this.setChecked(true);
        }
    }

}
