/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.path_watch;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.python.pydev.core.ListenerList;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * @author fabioz
 *
 * Service to watch filesystem changes at a given path. Works with JPathWatch.
 *
 * Multiple events are stacked and reported as soon as it happens (from a non-main thread).
 *
 * Note that if a directory being watched is removed, it should notify that the given path was removed
 * (and will remove all the listeners for the path afterwards).
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
    private Map<Path, EventsStackerRunnable> pathToStacker = Collections
            .synchronizedMap(new HashMap<Path, EventsStackerRunnable>());
    private Map<WatchKey, Path> keyToPath = Collections
            .synchronizedMap(new HashMap<WatchKey, Path>());

    private final PollThread pollThread;
    private final Object lock = new Object();

    private PathWatch() {
        watchService = FileSystems.getDefault().newWatchService();
        pollThread = new PollThread();
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private class PollThread extends Thread {

        @Override
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
                            log.append("Event: ").appendObject(e).append('\n');
                        }

                        if (kind == StandardWatchEventKind.OVERFLOW) {
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
                            if (kind == StandardWatchEventKind.ENTRY_CREATE
                                    || kind == StandardWatchEventKind.ENTRY_MODIFY) {
                                stacker.added(file);

                            } else if (kind == StandardWatchEventKind.ENTRY_DELETE) {
                                stacker.removed(file);

                            } else if (kind == ExtendedWatchEventKind.KEY_INVALID) {
                                //Invalidated means it was removed... (so, no need to reschedule to listen again)
                                keyToPath.remove(signalledKey);
                                stacker.key = null;
                                stacker.removed(file);
                                pathToStacker.remove(watchedPath);
                            }
                        }
                    }

                    stacker.run();
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

    /**
     * A listener will start tracking changes at the given path.
     */
    public void track(File path, IFilesystemChangesListener listener) {
        Assert.isNotNull(path);
        Assert.isNotNull(listener);

        if (!path.exists()) {
            Log.log("Unable to track file that does not exist: " + path);
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
                    keyToPath.put(key, watchedPath);
                }
            }
        }
    }

}
