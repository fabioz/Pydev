/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.concurrency;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.log.Log;


/**
 * Helper class to ensure that only 1 job will be running.
 *
 * Clients should call addJob(job) to schedule the job and the job itself must call removeJob(job)
 * when finished (on a try..finally).
 */
public class SingleJobRunningPool {

    /**
     * Lock to access runningJobs.
     */
    private static Object jobsLock = new Object();

    /**
     * The 1st job in this list is always running!
     */
    private List<Job> runningJobs = new ArrayList<Job>();

    /**
     * Adds a job. Schedules it to be run if it's the 1st added job.
     *
     * Clients are responsible for calling removeJob(job) when the job finishes.
     */
    public void addJob(Job job) {
        synchronized (jobsLock) {
            runningJobs.add(job);
            if (runningJobs.size() == 1) {
                //no other will run it when finished!
                job.schedule(100);
            }
        }
    }

    /**
     * Removes the job from the running jobs and starts a new one (if one is available).
     */
    public void removeJob(Job job) {
        synchronized (jobsLock) {
            if (runningJobs.size() == 0) {
                Log.log("Something bad happened: trying to remove a job when no running job is available!! This should never happen.");
                return;
            }
            Job removed = runningJobs.remove(0);
            if (removed != job) {
                Log.log("Something bad happened: the removed one should always be in position 0!!! This should never happen.");
                //We got into a really bad state, just start over (and just to be explicit, 
                //this is a programming error and we're just trying to keep things running 
                //by going to a known state).
                runningJobs.clear();
                return;
            }
            //Start the next job (if one is available).
            if (runningJobs.size() > 0) {
                runningJobs.get(0).schedule(100);
            }
        }
    }
}
