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

    private final PyUnitTestRun testRun;
    
    
    public PyUnitViewServerListener(IPyUnitServer pyUnitServer, IPyUnitLaunch pyUnitLaunch) {
        this.testRun = new PyUnitTestRun(pyUnitLaunch);
        pyUnitServer.registerOnNotifyTest(this);
        updateJob.setPriority(JOBS_PRIORITY);
        updateJob.setSystem(true);
    }

    public static int TIMEOUT = 25;
    public static int JOBS_PRIORITY = Job.SHORT;
    private boolean finishedNotified = false;
    
    public void notifyTest(
            final String status, 
            final String location,
            final String test, 
            final String capturedOutput, 
            final String errorContents,
            final String time
            ) {
        synchronized (notifications) {
            notifications.add(new ICallback0<Object>() {

                public Object call() {
                    PyUnitTestResult result = new PyUnitTestResult(
                            testRun, status, location, test, capturedOutput, errorContents, time);
                    testRun.addResult(result);
                    view.notifyTest(result);
                    return null;
                }
            });
        }
        updateJob.schedule(TIMEOUT);
    }
    
    public void notifyFinished() {
        synchronized (notifications) {
            if(!finishedNotified){
                finishedNotified = true;
                notifications.add(new ICallback0<Object>() {
                    
                    public Object call() {
                        testRun.setFinished(true);
                        view.notifyFinished(testRun);
                        return null;
                    }
                });
            }
        }
        updateJob.schedule(TIMEOUT);
    }

    public void notifyDispose() {
        notifyFinished();
    }

    public void setView(PyUnitView view) {
        this.view = view;
    }

    public PyUnitTestRun getTestRun() {
        return testRun;
    }

    public void notifyTestsCollected(String totalTestsCount) {
        testRun.setTotalNumberOfRuns(totalTestsCount);
        view.notifyTestsCollected(testRun);
    }
}