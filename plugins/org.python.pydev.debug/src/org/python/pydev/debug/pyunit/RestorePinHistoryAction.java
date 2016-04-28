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
import org.python.pydev.shared_core.callbacks.ICallbackListener;

/**
 * @author fabioz
 */
public class RestorePinHistoryAction extends Action implements ICallbackListener<PyUnitTestRun> {

    private WeakReference<PyUnitView> view;
    private PyUnitTestRun testRun;

    /**
     * @param pyUnitView
     */
    public RestorePinHistoryAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        this.setImageDescriptor(PydevDebugPlugin.getImageCache().getDescriptor("icons/refresh.png"));
        PyUnitViewTestsHolder.onPinSelected.registerListener(this);
        this.call(PyUnitViewTestsHolder.getCurrentPinned());
    }

    private void setInitialTooltipText() {
        this.setToolTipText("Click to restore pinned test run.");
    }

    @Override
    public Object call(PyUnitTestRun obj) {
        if (obj != null) {
            this.setToolTipText("Click to restore test run: " + obj.name);
        } else {
            setInitialTooltipText();
        }
        this.setEnabled(obj != null);
        this.testRun = obj;
        return null;
    }

    @Override
    public void run() {
        if (testRun != null) {
            SetCurrentRunAction setCurrentRunAction = new SetCurrentRunAction(view, testRun);
            setCurrentRunAction.run();
        }
    }

}
