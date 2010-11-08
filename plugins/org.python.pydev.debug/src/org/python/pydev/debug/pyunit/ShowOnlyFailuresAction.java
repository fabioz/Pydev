package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.ui.UIConstants;

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
