/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

/**
 * Used to properly pass notifications in the UI thread to the PyUnitView.
 * 
 * @author fabioz
 */
final class PyUnitViewServerListener implements IPyUnitServerListener {

    private PyUnitView view;
    private Object lockView = new Object();

    private LinkedList<ICallback0<Object>> notifications = new LinkedListWarningOnSlowOperations<ICallback0<Object>>();

    private Job updateJob = new UIJob("Update unittest view") {

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
                    Log.log(e);
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

    public void notifyTest(final String status, final String location, final String test, final String capturedOutput,
            final String errorContents, final String time) {
        synchronized (notifications) {
            notifications.add(new ICallback0<Object>() {

                public Object call() {
                    PyUnitTestResult result = new PyUnitTestResult(testRun, status, location, test, capturedOutput,
                            errorContents, time);
                    testRun.addResult(result);
                    synchronized (lockView) {
                        if (view != null) {
                            view.notifyTest(result);
                        }
                    }
                    return null;
                }
            });
        }
        updateJob.schedule(TIMEOUT);
    }

    public void notifyStartTest(final String location, final String test) {
        synchronized (notifications) {
            notifications.add(new ICallback0<Object>() {

                public Object call() {
                    PyUnitTestStarted result = new PyUnitTestStarted(testRun, location, test);
                    testRun.addStartTest(result);
                    synchronized (lockView) {
                        if (view != null) {
                            view.notifyTestStarted(result);
                        }
                    }
                    return null;
                }
            });
        }
        updateJob.schedule(TIMEOUT);
    }

    public void notifyFinished(final String totalTime) {
        synchronized (notifications) {
            if (!finishedNotified) {
                finishedNotified = true;
                notifications.add(new ICallback0<Object>() {

                    public Object call() {
                        testRun.setFinished(true);
                        if (totalTime != null) {
                            testRun.setTotalTime(totalTime);
                        }
                        synchronized (lockView) {
                            if (view != null) {
                                view.notifyFinished(testRun);
                            }
                        }
                        return null;
                    }
                });
            }
        }
        updateJob.schedule(TIMEOUT);
    }

    public void notifyDispose() {
        notifyFinished(null);
    }

    public void setView(PyUnitView view) {
        synchronized (lockView) {
            this.view = view;
        }
    }

    public PyUnitView getView() {
        synchronized (lockView) {
            return view;
        }
    }

    public PyUnitTestRun getTestRun() {
        return testRun;
    }

    public void notifyTestsCollected(String totalTestsCount) {
        testRun.setTotalNumberOfRuns(totalTestsCount);
        synchronized (lockView) {
            if (view != null) {
                view.notifyTestsCollected(testRun);
            }
        }
    }
}