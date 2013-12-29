/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.concurrency;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.MathUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is a pool where we can register runnables to run -- and it'll let only X runnables run at the same time.
 * 
 * The runnables will be run as eclipse jobs. 
 */
public class RunnableAsJobsPoolThread extends Thread {
    /**
     * 
     * We cannot have more than XX jobs scheduled at any time. 
     */
    private final Semaphore jobsCreationSemaphore;

    /**
     * Semaphore to run: only let it go if there's a release() acknowledging something happened.
     */
    private final Semaphore canRunSemaphore = new Semaphore(0);

    /**
     * List of runnables and their names to run.
     */
    private final List<Tuple<Runnable, String>> runnables = new ArrayList<Tuple<Runnable, String>>();

    /**
     * Lock to access the runnables field.
     */
    private final Object lockRunnables = new Object();

    /**
     * Constructor
     * 
     * @param maxSize the maximum number of runnables that can run at the same time.
     */
    public RunnableAsJobsPoolThread(int maxSize) {
        jobsCreationSemaphore = new Semaphore(maxSize);
        this.setDaemon(true);
        this.start();
    }

    private final Object stopThreadsLock = new Object();
    private int stopThreads = 0;

    public void pushStopThreads() {
        synchronized (stopThreadsLock) {
            stopThreads += 1;
        }
    }

    public void popStopThreads() {
        synchronized (stopThreadsLock) {
            stopThreads -= 1;
            Assert.isTrue(stopThreads >= 0);
            if (stopThreads == 0) {
                stopThreadsLock.notifyAll();
            }
        }
    }

    /**
     * We'll stay here until the end of times (or at least until the vm finishes)
     */
    @Override
    public void run() {
        while (true) {

            //until we've gotten a release we'll stay here.
            canRunSemaphore.acquire();

            //get the runnable to run.
            Tuple<Runnable, String> execute = null;
            int size;
            synchronized (lockRunnables) {
                size = runnables.size();
                if (size > 0) {
                    execute = runnables.remove(0);
                    size--;
                }
            }

            int local = 0;
            while (true) {
                synchronized (stopThreadsLock) {
                    local = stopThreads;
                }
                if (local == 0) {
                    break;
                } else {
                    synchronized (stopThreadsLock) {
                        try {
                            stopThreadsLock.wait(200);
                        } catch (InterruptedException e) {
                            Log.log(e);
                        }
                    }
                }
            }

            if (execute != null) {
                //this will make certain that only X jobs are running.
                jobsCreationSemaphore.acquire();
                final Runnable[] runnable = new Runnable[] { execute.o1 };
                String name = execute.o2;
                execute = null;

                if (size > 1) {
                    name += " (" + size + " scheduled)";
                }

                Job workbenchJob = new Job(name) {

                    @Override
                    public IStatus run(IProgressMonitor monitor) {
                        Runnable r;
                        try {
                            r = runnable[0];
                            if (r instanceof IRunnableWithMonitor) {
                                ((IRunnableWithMonitor) r).setMonitor(monitor);
                            }
                            runnable[0] = null;//make sure it'll be available for garbage collection ASAP.
                            r.run();
                        } catch (RuntimeException e) {
                            if (CorePlugin.getDefault() != null) {
                                //Only log if eclipse still didn't shutdown.
                                Log.log(e);
                            }
                        } finally {
                            r = null; //make sure it'll be available for garbage collection ASAP.
                            jobsCreationSemaphore.release();
                        }
                        return Status.OK_STATUS;
                    }

                };
                //                workbenchJob.setSystem(true);
                //                workbenchJob.setPriority(Job.BUILD);
                workbenchJob.setPriority(Job.INTERACTIVE);
                workbenchJob.schedule();
            }

        }
    }

    public void scheduleToRun(final IRunnableWithMonitor runnable, final String name) {
        synchronized (lockRunnables) {
            runnables.add(new Tuple<Runnable, String>(runnable, name));
        }
        canRunSemaphore.release();
    }

    /**
     * Meant to be used in tests!
     */
    public void waitToFinishCurrent() {
        final Object lock = new Object();

        IRunnableWithMonitor runnable = new IRunnableWithMonitor() {

            @Override
            public void run() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void setMonitor(IProgressMonitor monitor) {
            }
        };
        //I.e.: we'll schedule a job to wait until all the currently scheduled jobs are run.
        scheduleToRun(runnable, "Wait to run all currently scheduled jobs");
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static RunnableAsJobsPoolThread singleton;

    /**
     * @return a singleton to be shared across multiple clases. Note that this class
     * may still have locally created instances (so, its constructor is not private as
     * is usual for singletons).
     */
    public synchronized static RunnableAsJobsPoolThread getSingleton() {
        if (singleton == null) {
            //if a problem happens getting the number of processors (although it shouldn't happen), use 6
            int maxSize = 6;

            try {
                int availableProcessors = Runtime.getRuntime().availableProcessors();
                if (availableProcessors <= 1) {
                    maxSize = 3;

                } else {
                    //note that we create more threads than processes because some are very likely to 
                    //be disk-bound processes (but with a logarithmic function, because we don't want 
                    //to add up too fast as the number of processors increase because of the amount of memory
                    //it'd consume).
                    //
                    //The progression we get with this formula is below.
                    //
                    //2: 4
                    //3: 6
                    //4: 8
                    //5: 10
                    //6: 11
                    //7: 13
                    //8: 14
                    //9: 16
                    //10: 17
                    //11: 18
                    //12: 19
                    //13: 21
                    //14: 22
                    //15: 23
                    //16: 24
                    //17: 25
                    //18: 27
                    //19: 28
                    maxSize = (int) (availableProcessors + Math.round(MathUtils.log(availableProcessors, 1.4)));
                }
            } catch (Throwable e) {
            }

            singleton = new RunnableAsJobsPoolThread(maxSize);
        }
        return singleton;
    }

}
