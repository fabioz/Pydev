/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;

public class SetCurrentRunAction extends Action {

    private WeakReference<PyUnitView> view;
    private WeakReference<PyUnitTestRun> pyUnitTestRun;

    public SetCurrentRunAction(WeakReference<PyUnitView> view, PyUnitTestRun pyUnitTestRun) {
        this.view = view;
        this.pyUnitTestRun = new WeakReference<PyUnitTestRun>(pyUnitTestRun);
    }

    @Override
    public void run() {
        PyUnitView pyUnitView = this.view.get();
        if (pyUnitView != null) {
            PyUnitTestRun run = this.pyUnitTestRun.get();
            if (run != null) {
                pyUnitView.setCurrentRun(run);
            }
        }
    }

}
