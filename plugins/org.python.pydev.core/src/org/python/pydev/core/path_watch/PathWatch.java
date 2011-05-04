/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.path_watch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.ListenerList;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;

/**
 * @author fabioz
 *
 * Service to watch filesystem changes at a given path. Works with JPathWatch.
 * 
 * Multiple events are stacked and reported from time to time.
 * 
 * When a key that is tracked is removed from the filesystem, it enters in a poll job (invalidPathsRestorer)
 * which will notify when it's recreated (note that it's only (re)scheduled if there is some available invalid path).
 */
public class PathWatch {

    /**
     * The service that'll give us notifications
     */
    private WatchService watchService;
    
    /**
     * The path being watched and the stacker object that'll stack many requests into one.
     * 
     * The stacker object contains the actual key in the watchService (although it may be none if the key
     * becomes invalid).
     */
    private Map<Path, EventsStackerRunnable> pathToStacker = new HashMap<Path, EventsStackerRunnable>();
    private Map<WatchKey, Path> keyToPath = new HashMap<WatchKey, Path>();
    
    private final Object invalidPathsLock = new Object();
    private volatile Set<EventsStackerRunnable> invalidPaths = new HashSet<EventsStackerRunnable>();
    /*default*/ Set<EventsStackerRunnable> getInvalidPaths() {
        synchronized (invalidPathsLock) {
            //Always return a copy!
            return new HashSet<EventsStackerRunnable>(invalidPaths);
        }
    }
    
    private final PollThread pollThread;
    private final Object lock = new Object();
    
    private volatile List<Runnable> runnables = new ArrayList<Runnable>();
    private final Job jobRunRunnables = new Job("PathWatch notifier") {
        
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            //Clients will actually be notified in this job.
            List<Runnable> curr = runnables;
            runnables = new ArrayList<Runnable>();
            for (Runnable runnable : curr) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            return Status.OK_STATUS;
        }
    };
    
    
    public static int RECHECK_INVALID_PATHS_EACH = 4000;
    private final Job invalidPathsRestorer = new Job("Invalid paths restorer") {
        
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            synchronized (invalidPathsLock) {
                for(Iterator<EventsStackerRunnable> it=invalidPaths.iterator();it.hasNext();){
                    EventsStackerRunnable r = it.next();
                    IFilesystemChangesListener[] listeners = r.list.getListeners();
                    if(listeners.length == 0){
                        it.remove(); //remove last iterated (no longer watched)
                    }else{
                        File f = new File(r.watchedPath.toString());
                        if(f.exists()){
                            for (IFilesystemChangesListener listener : listeners) {
                                listener.added(f);
                            }
                            
                            try {
                                WatchKey key = r.watchedPath.register(
                                        watchService, 
                                        StandardWatchEventKind.ENTRY_CREATE, 
                                        StandardWatchEventKind.ENTRY_DELETE,
                                        StandardWatchEventKind.ENTRY_MODIFY,
                                        StandardWatchEventKind.OVERFLOW,
                                        ExtendedWatchEventKind.KEY_INVALID
                                );
                                //only add to be removed if it was successful...
                                r.key = key;
                                keyToPath.put(key, r.watchedPath);
                                it.remove(); //remove last iterated (valid again)
                            } catch (UnsupportedOperationException uox) {
                                Log.log(uox);
                                
                            } catch (IOException iox) {
                                //Ignore: it may not exist now, but may start existing later on...
                                
                            } catch (Throwable e) {
                                Log.log(e);
                            }
                            
                        }
                    }
                }
                //Re-add the ones not removed...
                if(invalidPaths.size() > 0){
                    this.schedule(RECHECK_INVALID_PATHS_EACH);
                }
            }
            return Status.OK_STATUS;
        }
    };
    
    /**
     * After receiving a change, it'll only be notified after this time elapses (in millis).
     * This means that while we have a change it may be that what's reported actually changes
     * (i.e.: if a file is added and removed, only the removal will be recorded).
     */
    public static int TIME_BEFORE_NOTIFY = 250;

    private PathWatch() {
        watchService = FileSystems.getDefault().newWatchService();
        pollThread = new PollThread();
        pollThread.start();
    }
    
    private class PollThread extends Thread {
        
        public void run() {
            
            for (;;) {
                // take() will block until a file has been created/deleted
                WatchKey signalledKey;
                try {
                    signalledKey = watchService.take();
                } catch (InterruptedException ix) {
                    // we'll ignore being interrupted
                    continue;
                } catch (ClosedWatchServiceException cwse) {
                    // other thread closed watch service
                    System.out.println("watch service closed, terminating.");
                    break;
                }
                
                IFilesystemChangesListener[] listeners = null;
                List<WatchEvent<?>> list;
                Path watchedPath;
                EventsStackerRunnable stacker;
                
                synchronized (lock) {
                    watchedPath = keyToPath.get(signalledKey);
                    if(watchedPath == null){
                        continue;
                    }
                    
                    // get list of events from key
                    list = signalledKey.pollEvents();
                    
                    stacker = pathToStacker.get(watchedPath);
                    if(stacker.list != null){
                        listeners = stacker.list.getListeners();
                    }
                }
                if(listeners != null && listeners.length > 0){
                    // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                    // key to be reported again by the watch service (but don't do that if there are no more
                    // listeners).
                    signalledKey.reset();
                    runnables.add(stacker);
                    
                    for (WatchEvent<?> e : list) {
                        Path context = (Path) e.context();
                        Path resolve = watchedPath.resolve(context);
                        File file = new File(resolve.toString());
                        if (e.kind() == StandardWatchEventKind.OVERFLOW) {
                            //On an overflow, wait a bit and signal that all files being watched were removed,
                            //do a list and say that the current files were added again.
                            stacker.overflow(file);
                            if(!file.exists()){
                                //It may be that it became invalid...
                                keyToPath.remove(signalledKey);
                                stacker.key = null;
                                addInvalidPath(stacker);
                                stacker.removed(file);
                            }

                        }else{
                            if (e.kind() == StandardWatchEventKind.ENTRY_CREATE || 
                                    e.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                                stacker.added(file);
                                
                            } else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                                stacker.removed(file);
                                
                            }else if(e.kind() == ExtendedWatchEventKind.KEY_INVALID){
                                //Invalidated means it was removed...
                                keyToPath.remove(signalledKey);
                                stacker.key = null;
                                addInvalidPath(stacker);
                                stacker.removed(file);
                            }
                        }
                        
                    }
                    jobRunRunnables.schedule(TIME_BEFORE_NOTIFY);
                }
            }
        }
    }

    private static PathWatch singleton = null;

    public static PathWatch get() {
        if (singleton == null) {
            singleton = new PathWatch();
        }
        return singleton;
    }

    public void stopTrack(File path, IFilesystemChangesListener listener) {
        Assert.isNotNull(path);
        Assert.isNotNull(listener);
        
        Path watchedPath = Paths.get(REF.getFileAbsolutePath(path));
        
        synchronized (lock) {
            EventsStackerRunnable stacker = pathToStacker.get(watchedPath);
            
            if(stacker != null && stacker.list != null){
                ListenerList<IFilesystemChangesListener> list = stacker.list;
                list.remove(listener);
                if(list.getListeners().length == 0){
                    pathToStacker.remove(watchedPath);
                    keyToPath.remove(stacker.key);
                    synchronized (invalidPaths) {
                        invalidPaths.remove(stacker);
                    }
                }
            }
        }
    }
    
    
    /**
     * A listener will start tracking changes at the given path.
     */
    public void track(File path, IFilesystemChangesListener listener) {
        Assert.isNotNull(path);
        Assert.isNotNull(listener);
        
        Path watchedPath = Paths.get(REF.getFileAbsolutePath(path));
        
        synchronized (lock) {
            EventsStackerRunnable stacker = pathToStacker.get(watchedPath);
            if(stacker != null){
                //already being tracked -- or already in invalid list ;)
                stacker.list.add(listener);

                return;
            }
            
            boolean add = true;
            WatchKey key = null;
            try {
                key = watchedPath.register(
                        watchService, 
                        StandardWatchEventKind.ENTRY_CREATE, 
                        StandardWatchEventKind.ENTRY_DELETE,
                        StandardWatchEventKind.ENTRY_MODIFY,
                        StandardWatchEventKind.OVERFLOW,
                        ExtendedWatchEventKind.KEY_INVALID
                );
            } catch (UnsupportedOperationException uox) {
                add = false;
                Log.log(uox);
                
            } catch (IOException iox) {
                //Ignore: it may not exist now, but may start existing later on...
                
            } catch (Throwable e) {
                add = false;
                Log.log(e);
            }
    
            if(add){
                if(stacker == null){
                    stacker = new EventsStackerRunnable(
                            key, watchedPath, new ListenerList<IFilesystemChangesListener>(IFilesystemChangesListener.class));
                    pathToStacker.put(watchedPath, stacker);
                }
                stacker.list.add(listener);
                
                if(key != null){
                    keyToPath.put(key, watchedPath);
                }else{
                    //Will go to our poll service to start tracking when it becomes valid...
                    addInvalidPath(stacker);
                }
            }
        }
    }

    private void addInvalidPath(EventsStackerRunnable stacker) {
        synchronized (invalidPathsLock) {
            invalidPaths.add(stacker);
        }
        invalidPathsRestorer.schedule(RECHECK_INVALID_PATHS_EACH);
    }
}

