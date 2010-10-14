package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Used to properly pass notifications in the UI thread to the PyUnitView.
 * 
 * @author fabioz
 */
final class PyUnitViewServerListener implements IPyUnitServerListener {
    
    private PyUnitView view;

    private LinkedList<ICallback0<Object>> notifications = new LinkedList<ICallback0<Object>>();
    
    private Job updateJob = new UIJob("Update unittest view"){

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            ArrayList<ICallback0<Object>> arrayList;
            synchronized (notifications) {
                arrayList = new ArrayList<ICallback0<Object>>(notifications);
                notifications.clear();
            }
            for (ICallback0<Object> iCallback0 : arrayList) {
                try {
                    iCallback0.call();
                } catch (Exception e) {
                    PydevPlugin.log(e);
                }
            }
            return Status.OK_STATUS;
        }
    };
    
    
    public PyUnitViewServerListener(PyUnitServer pyUnitServer) {
        pyUnitServer.registerOnNotifyTest(this);
        updateJob.setPriority(Job.SHORT);
        updateJob.setSystem(true);
    }

    public void notifyTest(final String status, final String location, final String test) {
        synchronized (notifications) {
            notifications.add(new ICallback0<Object>() {

                public Object call() {
                    view.notifyTest(status, location, test);
                    return null;
                }
            });
        }
        updateJob.schedule(25);
    }

    public void notifyDispose() {
        
    }

    public void setView(PyUnitView view) {
        this.view = view;
    }
}