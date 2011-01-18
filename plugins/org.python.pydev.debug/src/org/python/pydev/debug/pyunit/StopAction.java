package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class StopAction extends Action {

    private WeakReference<PyUnitView> view;

    public StopAction(PyUnitView view){
        setToolTipText("Stops the execution of the current test being run.");
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.TERMINATE));
        this.view = new WeakReference<PyUnitView>(view);
    }
    
    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
        if(currentTestRun != null){
            currentTestRun.stop();
        }
    }
    
    
}
