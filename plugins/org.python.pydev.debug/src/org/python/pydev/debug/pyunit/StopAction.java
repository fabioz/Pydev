/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;

public class StopAction extends Action {

    private WeakReference<PyUnitView> view;

    public StopAction(PyUnitView view) {
        setToolTipText("Stops the execution of the current test being run.");
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.TERMINATE));
        this.view = new WeakReference<PyUnitView>(view);
    }

    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
        if (currentTestRun != null) {
            currentTestRun.stop();
        }
    }

}
