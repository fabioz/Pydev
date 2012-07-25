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
import name.pachler.nio.file.WatchEvent.Kind;
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
import org.python.pydev.core.structure.FastStringBuffer;

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
     * The service that'll give us notifications.
     */
    private WatchService watchService;

    /**
     * If != null, logs will be added to this buffer.
     */
    public static FastStringBuffer log;

    /**
     * The path being watched and the stacker object that'll stack many requests into one.
     * 
     * The stacker object contains the actual key in the watchService (although it may be none if the key
     * becomes invalid).
     */
    private Map<Path, EventsStackerRunnable> pathToStacker = new HashMap<Path, EventsStackerRunnable>();
    private final Object keyToPathLock = new Object();
    private Map<WatchKey, Path> keyToPath = new HashMap<WatchKey, Path>();

    private final Object invalidPathsLock = new Object();
    private volatile Set<EventsStackerRunnable> invalidPaths = new HashSet<EventsStackerRunnable>();

    /*default*/Set<EventsStackerRunnable> getInvalidPaths() {
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
                if (log != null) {
                    log.append('.');
                }

                Set<EventsStackerRunnable> remove = new HashSet<EventsStackerRunnable>();
                for (Iterator<EventsStackerRunnable> it = invalidPaths.iterator(); it.hasNext();) {
                    EventsStackerRunnable r = it.next();
                    IFilesystemChangesListener[] listeners = r.list.getListeners();
                    if (listeners.length == 0) {
                        if (log != null) {
                            log.append("Removing stacker from invalid list (because it has no listeners): ")
                                    .appendObject(r).append('\n');
                        }
                        remove.add(r); //remove last iterated (no longer watched)
                    } else {
                        File f = new File(r.watchedPath.toString());
                        if (f.exists()) {
                            for (IFilesystemChangesListener listener : listeners) {
                                listener.added(f);
                            }

                            try {
                                WatchKey key = r.watchedPath.register(watchService,
                                        StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE,
                                        StandardWatchEventKind.ENTRY_MODIFY, StandardWatchEventKind.OVERFLOW,
                                        ExtendedWatchEventKind.KEY_INVALID);
                                //only add to be removed if it was successful...
                                r.key = key;
                                synchronized (keyToPathLock) {
                                    keyToPath.put(key, r.watchedPath);
                                }

                                if (log != null) {
                                    log.append("Removing stacker from invalid list because it became valid again: ")
                                            .appendObject(r).append('\n');
                                }

                                remove.add(r); //remove last iterated (valid again)
                            } catch (UnsupportedOperationException uox) {
                                Log.log(uox);

                            } catch (IOException iox) {
                                //Ignore: it may not exist now, but may start existing later on...
                                if (log != null) {
                                    log.append("IOException when trying to make valid: " + r.watchedPath);
                                }

                            } catch (Throwable e) {
                                Log.log(e);
                            }

                        }
                    }
                }

                invalidPaths.removeAll(remove);
                //Re-add the ones not removed...
                int size = invalidPaths.size();
                if (log != null) {
                    if (size < 0) {
                        //This could happen when access to invalidPaths is not properly synched with invalidPathsLock!
                        log.append("\nBUG BUG BUG: Size: ").append(size).append('\n');
                    }
                }
                if (size > 0) {
                    this.schedule(RECHECK_INVALID_PATHS_EACH);
                    if (log != null) {
                        log.append("!");
                    }
                } else {
                    if (log != null) {
                        log.append("NOT rescheduling; size=").append(size).append(";invalidPaths=")
                                .appendObject(invalidPaths).append('\n');
                    }
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
                    if (log != null) {
                        log.append("Interrupted\n");
                    }
                    continue;
                } catch (ClosedWatchServiceException cwse) {
                    // other thread closed watch service
                    System.out.println("watch service closed, terminating.");
                    break;
                }

                List<WatchEvent<?>> list;
                Path watchedPath;
                EventsStackerRunnable stacker;

                synchronized (lock) {
                    synchronized (keyToPathLock) {
                        watchedPath = keyToPath.get(signalledKey);
                    }
                    if (watchedPath == null) {
                        continue;
                    }

                    // get list of events from key
                    list = signalledKey.pollEvents();

                    stacker = pathToStacker.get(watchedPath);
                    if (stacker == null) {
                        //if the stacker does not exist, go on without rescheduling the key!
                        if (log != null) {
                            log.append("Stacker for: ").appendObject(watchedPath).append("is null\n");
                        }
                        continue;
                    }

                    runnables.add(stacker);

                    for (WatchEvent<?> e : list) {
                        Path context = (Path) e.context();
                        Path resolve = watchedPath.resolve(context);
                        File file = new File(resolve.toString());
                        Kind<?> kind = e.kind();
                        if (log != null) {
                            log.append("Event: ").appendObject(e).append('\n');
                        }
                        if (kind == StandardWatchEventKind.OVERFLOW) {
                            if (!file.exists()) {
                                //It may be that it became invalid...
                                synchronized (keyToPathLock) {
                                    keyToPath.remove(signalledKey);
                                }
                                stacker.key = null;
                                addInvalidPath(stacker);
                                stacker.removed(file);
                            } else {
                                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                                // key to be reported again by the watch service.
                                signalledKey.reset();
                                if (log != null) {
                                    log.append("Key reset to hear changes");
                                }
                            }
                            //On an overflow, wait a bit and signal that all files being watched were removed,
                            //do a list and say that the current files were added again.
                            stacker.overflow(file);

                        } else {
                            if (kind == StandardWatchEventKind.ENTRY_CREATE
                                    || kind == StandardWatchEventKind.ENTRY_MODIFY) {
                                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                                // key to be reported again by the watch service.
                                signalledKey.reset();
                                if (log != null) {
                                    log.append("Key reset to hear changes");
                                }

                                stacker.added(file);

                            } else if (kind == StandardWatchEventKind.ENTRY_DELETE) {
                                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                                // key to be reported again by the watch service.
                                signalledKey.reset();
                                if (log != null) {
                                    log.append("Key reset to hear changes");
                                }
                                stacker.removed(file);

                            } else if (kind == ExtendedWatchEventKind.KEY_INVALID) {
                                //Invalidated means it was removed... (so, no need to reschedule to listen again)
                                synchronized (keyToPathLock) {
                                    keyToPath.remove(signalledKey);
                                }
                                stacker.key = null;
                                addInvalidPath(stacker);
                                stacker.removed(file);
                            }
                        }
                    }
                }
                if (runnables.size() > 0) {
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

        if (log != null) {
            log.append("STOP Track: ").appendObject(path).append("Listener: ").appendObject(listener).append('\n');
        }

        synchronized (lock) {
            EventsStackerRunnable stacker = pathToStacker.get(watchedPath);

            if (stacker != null && stacker.list != null) {
                ListenerList<IFilesystemChangesListener> list = stacker.list;
                list.remove(listener);
                if (list.getListeners().length == 0) {
                    pathToStacker.remove(watchedPath);
                    synchronized (keyToPathLock) {
                        keyToPath.remove(stacker.key);
                    }
                    synchronized (invalidPathsLock) {
                        if (log != null) {
                            log.append("Remove from invalid paths (no listeners): ").appendObject(stacker).append('\n');
                        }
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
            if (stacker != null) {
                //already being tracked -- or already in invalid list ;)
                stacker.list.add(listener);

                return;
            }

            if (log != null) {
                log.append("Track: ").appendObject(path).append("Listener: ").appendObject(listener).append('\n');
            }
            boolean add = true;
            WatchKey key = null;
            try {
                key = watchedPath.register(watchService, StandardWatchEventKind.ENTRY_CREATE,
                        StandardWatchEventKind.ENTRY_DELETE, StandardWatchEventKind.ENTRY_MODIFY,
                        StandardWatchEventKind.OVERFLOW, ExtendedWatchEventKind.KEY_INVALID);
            } catch (UnsupportedOperationException uox) {
                if (log != null) {
                    log.append("UnsupportedOperationException: ").appendObject(uox).append('\n');
                }
                add = false;
                Log.log(uox);

            } catch (IOException iox) {
                //Ignore: it may not exist now, but may start existing later on...

            } catch (Throwable e) {
                if (log != null) {
                    log.append("Throwable: ").appendObject(e).append('\n');
                }
                add = false;
                Log.log(e);
            }

            if (add) {
                if (stacker == null) {
                    stacker = new EventsStackerRunnable(key, watchedPath, new ListenerList<IFilesystemChangesListener>(
                            IFilesystemChangesListener.class));
                    pathToStacker.put(watchedPath, stacker);
                }
                stacker.list.add(listener);

                if (key != null) {
                    synchronized (keyToPathLock) {
                        keyToPath.put(key, watchedPath);
                    }
                } else {
                    //Will go to our poll service to start tracking when it becomes valid...
                    addInvalidPath(stacker);
                }
            }
        }
    }

    private void addInvalidPath(EventsStackerRunnable stacker) {
        if (log != null) {
            log.append("addInvalidPath: ").appendObject(stacker).append('\n');
        }

        synchronized (invalidPathsLock) {
            invalidPaths.add(stacker);
        }
        invalidPathsRestorer.schedule(RECHECK_INVALID_PATHS_EACH);
    }
}
