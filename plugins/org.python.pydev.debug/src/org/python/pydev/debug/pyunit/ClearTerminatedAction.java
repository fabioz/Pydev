/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;

public class ClearTerminatedAction extends Action {

    private WeakReference<PyUnitView> view;

    public ClearTerminatedAction(WeakReference<PyUnitView> view) {
        this.view = view;
        this.setText("Clear terminated");
        this.setToolTipText("Removes all terminated tests from the history");
    }

    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        if (pyUnitView != null) {
            pyUnitView.clearAllTerminated();
        }
    }

}
