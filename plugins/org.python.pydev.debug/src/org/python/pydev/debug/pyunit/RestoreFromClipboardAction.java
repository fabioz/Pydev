package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;

public class RestoreFromClipboardAction extends Action {

    private WeakReference<PyUnitView> view;

    public RestoreFromClipboardAction(WeakReference<PyUnitView> view) {
        this.view = view;
        this.setText("Restore from clipboard");
        this.setToolTipText("Restores a test session from the clipboard");
    }

    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        if (pyUnitView != null) {
            pyUnitView.restoreFromClipboard();
        }
    }

}
