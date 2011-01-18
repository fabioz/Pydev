package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;

public class ShowTestRunnerPreferencesAction extends Action{
    private WeakReference<PyUnitView> pyUnitView;

    public ShowTestRunnerPreferencesAction(PyUnitView pyUnitView) {
        this.pyUnitView = new WeakReference<PyUnitView>(pyUnitView);
        this.setText("Configure test runner preferences");
        this.setToolTipText("Opens preferences for configuring the test runner default parameters.");
    }

    @Override
    public void run() {
        PyUnitPrefsPage2.showPage();
    }
    
}
