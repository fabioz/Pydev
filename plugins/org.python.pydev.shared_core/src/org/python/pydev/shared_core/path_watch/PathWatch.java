/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.path_watch;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.callbacks.ListenerList;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * @author fabioz
 *
 * Service to watch filesystem changes at a given path. Works with the default watch service from JDK 1.7.
 *
 * Multiple events are stacked and reported as soon as it happens (from a non-main thread).
 *
 * Note that if a directory being watched is removed, it should notify that the given path was removed
 * (and will remove all the listeners for the path afterwards).
 */
public class PathWatch implements IPathWatch {

    /**
     * The service that'll give us notifications.
     */
    private WatchService watchService;

    /**
     * If != null, logs will be added to this buffer.
     */
    public FastStringBuffer log;

    /**
     * The path being watched and the stacker object that'll stack many requests into one.
     *
     * The stacker object contains the actual key in the watchService (although it may be none if the key
     * becomes invalid).
     */
    private Map<Path, EventsStackerRunnable> pathToStacker = Collections
            .synchronizedMap(new HashMap<Path, EventsStackerRunnable>());

    private Map<WatchKey, Path> keyToPath = Collections
            .synchronizedMap(new HashMap<WatchKey, Path>());

    private final PollThread pollThread;
    private final Object lock = new Object();

    private volatile boolean disposed = false;

    private FileFilter fileFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return true; //by default accepts everything.
        }
    };

    private FileFilter dirsFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return true; //by default accepts everything.
        }
    };

    private boolean registeredTracker;

    public PathWatch() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Log.log("Error starting watch service", e);
        }
        pollThread = new PollThread();
        pollThread.setDaemon(true);
        pollThread.setPriority(Thread.MIN_PRIORITY + 1); //Just a bit above minimum.
        pollThread.start();
    }

    private class PollThread extends Thread {

        @Override
        public void run() {
            for (;;) {
                try {
                    if (disposed) {
                        return;
                    }
                    if (watchService == null) {
                        Log.log("Error: watchService is null. Unable to track file changes.");
                        return;
                    }
                    if (log != null) {
                        log.append("Wating (watchService.take)\n");
                    }
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
                        // System.out.println("watch service closed, terminating.");
                        break;
                    }

                    List<WatchEvent<?>> list;
                    Path watchedPath;
                    EventsStackerRunnable stacker;

                    synchronized (lock) {
                        watchedPath = keyToPath.get(signalledKey);
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

                        // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                        // key to be reported again by the watch service.
                        if (new File(watchedPath.toString()).exists()) {
                            signalledKey.reset();
                        }

                        for (WatchEvent<?> e : list) {
                            Path context = (Path) e.context();
                            Path resolve = watchedPath.resolve(context);
                            File file = new File(resolve.toString());
                            Kind<?> kind = e.kind();
                            if (log != null) {
                                log.append("Event: ").appendObject(kind).append(" file: ").appendObject(file)
                                        .append('\n');
                            }

                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                if (!file.exists()) {
                                    //It may be that it became invalid...
                                    keyToPath.remove(signalledKey);
                                    stacker.key = null;
                                    stacker.removed(file);
                                } else {
                                }
                                //On an overflow, wait a bit and signal that all files being watched were removed,
                                //do a list and say that the current files were added again.
                                stacker.overflow(file);

                            } else {
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                        || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    stacker.added(file);

                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    stacker.removed(file);

                                    //Only available on jpath watch
                                    //} else if (kind == ExtendedWatchEventKind.KEY_INVALID) {
                                    //    //Invalidated means it was removed... (so, no need to reschedule to listen again)
                                    //    keyToPath.remove(signalledKey);
                                    //    stacker.key = null;
                                    //    stacker.removed(file);
                                    //    pathToStacker.remove(watchedPath);
                                }
                            }
                        }

                        try {
                            stacker.run();
                        } catch (Exception e1) {
                            Log.log(e1);
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_core.path_watch.IPathWatch#stopTrack(java.io.File, org.python.pydev.shared_core.path_watch.IFilesystemChangesListener)
     */
    @Override
    public void stopTrack(File path, IFilesystemChangesListener listener) {
        if (disposed) {
            return;
        }
        Assert.isNotNull(path);
        Assert.isNotNull(listener);

        Path watchedPath = Paths.get(FileUtils.getFileAbsolutePath(path));

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
                    keyToPath.remove(stacker.key);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_core.path_watch.IPathWatch#hasTracker(java.io.File, org.python.pydev.shared_core.path_watch.IFilesystemChangesListener)
     */
    @Override
    public boolean hasTracker(File path, IFilesystemChangesListener listener) {
        if (disposed) {
            return false;
        }
        Assert.isNotNull(path);
        Assert.isNotNull(listener);

        Path watchedPath = Paths.get(FileUtils.getFileAbsolutePath(path));

        if (log != null) {
            log.append("Has Tracker: ").appendObject(path).append("Listener: ").appendObject(listener).append('\n');
        }

        synchronized (lock) {
            EventsStackerRunnable stacker = pathToStacker.get(watchedPath);

            if (stacker != null && stacker.list != null) {
                ListenerList<IFilesystemChangesListener> list = stacker.list;
                IFilesystemChangesListener[] listeners = list.getListeners();
                for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                    if (list.equals(iFilesystemChangesListener)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_core.path_watch.IPathWatch#dispose()
     */
    @Override
    public void dispose() {
        disposed = true;
        try {
            synchronized (lock) {
                pathToStacker.clear();
                keyToPath.clear();
                try {
                    if (watchService != null) {
                        watchService.close();
                    }
                } catch (IOException e) {
                    Log.log(e);
                }
                pollThread.interrupt();
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_core.path_watch.IPathWatch#track(java.io.File, org.python.pydev.shared_core.path_watch.IFilesystemChangesListener)
     */
    @Override
    public void track(File path, IFilesystemChangesListener listener) {
        if (disposed) {
            return;
        }
        registeredTracker = true;
        Assert.isNotNull(path);
        Assert.isNotNull(listener);

        if (!path.exists()) {
            Log.logInfo("Unable to track file that does not exist: " + path);
            return;
        }
        Path watchedPath = Paths.get(FileUtils.getFileAbsolutePath(path));

        synchronized (lock) {
            EventsStackerRunnable stacker = pathToStacker.get(watchedPath);
            if (stacker != null) {
                //already being tracked -- or already in invalid list ;)
                stacker.list.add(listener);

                return;
            }

            if (log != null) {
                log.append("Track: ").appendObject(path).append(" Listener: ").appendObject(listener).append('\n');
            }
            boolean add = true;
            WatchKey key = null;
            try {
                if (watchService != null) {
                    key = watchedPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.OVERFLOW
                    //, ExtendedWatchEventKind.KEY_INVALID
                    );
                } else {
                    Log.log("watchService is null. Unable to track: " + path);
                }
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
                            IFilesystemChangesListener.class), path, fileFilter, dirsFilter);
                    pathToStacker.put(watchedPath, stacker);
                }
                stacker.list.add(listener);

                if (key != null) {
                    keyToPath.put(key, watchedPath);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.python.pydev.shared_core.path_watch.IPathWatch#setDirectoryFileFilter(java.io.FileFilter, java.io.FileFilter)
     */
    @Override
    public void setDirectoryFileFilter(FileFilter fileFilter, FileFilter dirsFilter) {
        if (registeredTracker) {
            throw new AssertionError("After registering a tracker, the file filter can no longer be changed.");
        }
        this.fileFilter = fileFilter;
        this.dirsFilter = dirsFilter;
    }

}
