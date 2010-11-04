package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class RelaunchAction extends Action{
    
    private WeakReference<PyUnitView> view;

    public RelaunchAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH1));
        this.setToolTipText("Relaunches the currently selected test run.");
    }
    
    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
        if(currentTestRun != null){
            currentTestRun.relaunch();
        }
    }
}
