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
import org.python.pydev.shared_ui.UIConstants;

public class ShowOnlyFailuresAction extends Action {

    private WeakReference<PyUnitView> pyUnitView;

    public ShowOnlyFailuresAction(PyUnitView pyUnitView) {
        this.pyUnitView = new WeakReference<PyUnitView>(pyUnitView);
        this.setChecked(false);
        this.setImageDescriptor(PydevDebugPlugin.getImageCache().getDescriptor(UIConstants.SHOW_ONLY_ERRORS));
        this.setToolTipText("If pressed, shows only errors and failures");
    }

    @Override
    public void run() {
        boolean checked2 = this.isChecked();
        pyUnitView.get().setShowOnlyErrors(checked2);
    }
}
