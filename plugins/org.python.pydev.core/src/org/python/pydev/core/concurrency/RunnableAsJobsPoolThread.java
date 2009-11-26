package org.python.pydev.core.concurrency;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.Tuple;

/**
 * This is a pool where we can register runnables to run -- and it'll let only X runnables run at the same time.
 * 
 * The runnables will be run as eclipse jobs. 
 */
public class RunnableAsJobsPoolThread extends Thread{
    /**
     * 
     * We cannot have more than XX jobs scheduled at any time. 
     */
    private Semaphore jobsCreationSemaphore;
    
    /**
     * Semaphore to run: only let it go if there's a release() acknowledging something happened.
     */
    private Semaphore canRunSemaphore = new Semaphore(0);

    /**
     * List of runnables and their names to run.
     */
    private List<Tuple<Runnable, String>> runnables = new ArrayList<Tuple<Runnable, String>>();
    
    /**
     * Lock to access the runnables field.
     */
    private Object lockRunnables = new Object();
    
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

    
    /**
     * We'll stay here until the end of times (or at least until the vm finishes)
     */
    @Override
    public void run() {
        while(true){
            
            //until we've gotten a release we'll stay here.
            canRunSemaphore.acquire();
            
            //get the runnable to run.
            Tuple<Runnable, String> execute = null;
            synchronized(lockRunnables){
                if(runnables.size() > 0){
                    execute = runnables.remove(0);
                }
            }
            
            if(execute != null){
                //this will make certain that only X jobs are running.
                jobsCreationSemaphore.acquire();
                final Runnable runnable = execute.o1;
                final String name = execute.o2;
                
                Job workbenchJob = new Job(name) {
                
                    @Override
                    public IStatus run(IProgressMonitor monitor) {
                        try{
                            runnable.run();
                        }finally{
                            jobsCreationSemaphore.release();
                        }
                        return Status.OK_STATUS;
                    }
                
                };
                workbenchJob.setSystem(true);
                workbenchJob.setPriority(Job.BUILD);
                workbenchJob.schedule();
            }
            
        }
    }

    public void scheduleToRun(final Runnable runnable, final String name){
        synchronized(lockRunnables){
            runnables.add(new Tuple<Runnable, String>(runnable, name));
        }
        canRunSemaphore.release();
    }
}
