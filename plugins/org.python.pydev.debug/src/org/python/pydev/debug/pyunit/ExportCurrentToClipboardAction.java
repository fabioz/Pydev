/**
 * Copyright (c) 2016 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;

public class ExportCurrentToClipboardAction extends Action {

    private WeakReference<PyUnitView> view;

    public ExportCurrentToClipboardAction(WeakReference<PyUnitView> view) {
        this.view = view;
        this.setText("Export current to clipboard");
        this.setToolTipText("Exports the currently selected test session to the clipboard");
    }

    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        if (pyUnitView != null) {
            pyUnitView.exportCurrentToClipboard();
        }
    }

}
