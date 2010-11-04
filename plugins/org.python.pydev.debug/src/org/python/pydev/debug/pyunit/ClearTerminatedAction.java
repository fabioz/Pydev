package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;

public class ClearTerminatedAction extends Action{

    private WeakReference<PyUnitView> view;

    public ClearTerminatedAction(WeakReference<PyUnitView> view) {
        this.view = view;
        this.setText("Clear terminated");
        this.setToolTipText("Removes all terminated tests from the history");
    }
    
    @Override
    public void run() {
        PyUnitView pyUnitView = view.get();
        if(pyUnitView != null){
            pyUnitView.clearAllTerminated();
        }
    }

}
