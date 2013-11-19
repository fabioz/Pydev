/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.path_watch;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.shared_core.callbacks.ListenerList;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.OrderedMap;

/**
 * This object will stack many ADD/REMOVE changes into a single change. It also deals with OVERFLOW changes, which
 * mean that too many changes occurred and thus can't be properly mapped to the actual events. In this case,
 * a notification that the base path was removed and then added again is issued (listener clients must take care
 * of properly dealing with this notification, as no events of added/removed children will be issued in this case).
 *
 * @author fabioz
 */
public class EventsStackerRunnable implements Runnable {

    public final static int ADDED = 0;
    public final static int REMOVED = 1;

    /**
     * As we only get notifications in directories from the files beneath it, or files within a folder
     * within it, we should hear 2 levels to get modifications.
     */
    public final static int LEVELS_TO_GET_MODIFIED_TIME = 2;

    /**
     * May be null!
     */
    /*default*/volatile WatchKey key;

    public final ListenerList<IFilesystemChangesListener> list;
    public final Path watchedPath;

    /**
     * Lock for dealing with fileToEvent and overflow.
     */
    private final Object lock = new Object();

    /**
     * The file mapping to the last event recorded in it.
     */
    private Map<File, Integer> fileToEvent = new OrderedMap<File, Integer>();

    /**
     * The directory being watched, where the overflow occurred.
     */
    private volatile File overflow = null;

    /**
     * The file related to the watchedPath we're listening.
     */
    private final File file;

    /**
     * This is the time of the last modified file we're interested in in the directory we're watching.
     *
     * If not a directory, it's not considered.
     */
    private final Map<File, Long> internalDirToLastModifiedTime = new HashMap<File, Long>();

    /**
     * Identifies whether we're watching a dir or not.
     */
    private final boolean isDir;

    /**
     * The file filter we're dealing with.
     */
    private final FileFilter fileFilter;

    /**
     * The filter for directories.
     */
    private final FileFilter dirFilter;

    private static final Long DIRECTORY_WITH_NOTHING_INTERESTING = 0L;

    private volatile boolean initializationFinished = false;

    private final Object lockInitialization = new Object();

    /**
     * Creates the events stacker based on the key, path and listeners related (the contents of the listeners may
     * change later on, but the actual key and path may not change).
     * @param fileFilter
     * @param path
     */
    public EventsStackerRunnable(WatchKey key, Path watchedPath, ListenerList<IFilesystemChangesListener> list,
            final File file, final FileFilter fileFilter, final FileFilter dirFilter) {
        Assert.isNotNull(list);
        Assert.isNotNull(watchedPath);
        this.list = list;
        this.key = key; //the key may be null!
        this.watchedPath = watchedPath;
        this.file = file;
        isDir = file.isDirectory();
        this.fileFilter = fileFilter;
        this.dirFilter = dirFilter;
        if (isDir) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        File[] listFiles = file.listFiles();
                        if (listFiles != null) {
                            for (File f : listFiles) {
                                if (f.isDirectory()) {
                                    if (dirFilter.accept(f)) {
                                        long lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(f,
                                                fileFilter, dirFilter, LEVELS_TO_GET_MODIFIED_TIME);
                                        if (lastModifiedTimeFromDir != 0) {
                                            internalDirToLastModifiedTime.put(
                                                    f, lastModifiedTimeFromDir);
                                        } else {
                                            internalDirToLastModifiedTime.put(
                                                    f, DIRECTORY_WITH_NOTHING_INTERESTING);
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        initializationFinished = true;
                    }

                };
            }.start();
        } else {
            initializationFinished = true;
        }
    }

    /**
     * When run, it'll notify clients about the events that were stacked as it makes sense (i.e.: if multiple additions
     * or removals of a file were issued, only the last one will actually be seen by clients).
     */
    public void run() {
        while (!initializationFinished) {
            synchronized (lockInitialization) {
                try {
                    lockInitialization.wait(100);
                } catch (InterruptedException e) {

                }
            }
        }
        Map<File, Integer> currentFileToEvent;
        File currentOverflow;
        boolean dirExists = true;
        synchronized (lock) {
            currentFileToEvent = fileToEvent;
            fileToEvent = new OrderedMap<File, Integer>();
            currentOverflow = overflow;
            overflow = null;
        }

        IFilesystemChangesListener[] listeners = list.getListeners();
        if (listeners.length == 0) {
            return;
        }

        if (isDir) {
            dirExists = file.exists();
            if (!dirExists) {
                //Special case if we were watching a directory and it no longer exists...
                for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                    try {
                        iFilesystemChangesListener.removed(file);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }

                //Directory no longer exists: just bail out!
                return;
            }
        }

        if (currentOverflow != null) {
            for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                //Say that the dir was removed...
                File watched = file;
                iFilesystemChangesListener.removed(watched);
                if (watched.exists()) {
                    //And later added again (without notifying about inner contents!!)
                    iFilesystemChangesListener.added(watched);
                }
            }
            return;
        }
        Set<Entry<File, Integer>> entrySet = currentFileToEvent.entrySet();
        for (Entry<File, Integer> entry : entrySet) {
            Integer value = entry.getValue();
            File currKey = entry.getKey();
            if (isDir) {
                Long lastModifiedTime = internalDirToLastModifiedTime.get(currKey);
                if (currKey.isDirectory()) {
                    long newLast = FileUtils
                            .getLastModifiedTimeFromDir(currKey, fileFilter, dirFilter, LEVELS_TO_GET_MODIFIED_TIME);
                    if (lastModifiedTime != null && newLast == lastModifiedTime) {
                        continue; //nothing interesting changed, just go on...
                    }
                    if (newLast == 0 && lastModifiedTime == null) {
                        //nothing interesting added either...
                        //Note: register as seen but with nothing interesting.
                        internalDirToLastModifiedTime.put(currKey, DIRECTORY_WITH_NOTHING_INTERESTING);
                        continue;
                    }
                    if (newLast == 0 && lastModifiedTime != null) {
                        //interesting content was removed (notify about it).
                        internalDirToLastModifiedTime.put(currKey, DIRECTORY_WITH_NOTHING_INTERESTING);
                    } else {

                        //interesting content changed
                        internalDirToLastModifiedTime.put(currKey, newLast);
                    }
                } else {
                    if (lastModifiedTime != null) {
                        if (value == ADDED && !currKey.exists()) {
                            //we have an add notification from addition of a directory that no longer exists.
                            //Don't notify: just wait for the remove notification.
                            continue;
                        }
                        //The internal directory was removed.
                        internalDirToLastModifiedTime.remove(currKey);
                        if (lastModifiedTime == DIRECTORY_WITH_NOTHING_INTERESTING) {
                            continue;
                        }
                    } else {
                        //Ok, it's really a file inside a directory: let's check if it's interesting...
                        if (!fileFilter.accept(currKey)) {
                            continue;
                        }
                    }
                }
            }

            switch (value) {
                case ADDED:
                    for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                        try {
                            iFilesystemChangesListener.added(currKey);
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                    break;

                case REMOVED:
                    for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                        try {
                            iFilesystemChangesListener.removed(currKey);
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * On overflow we'll clear all the other events and just send the overflow (any subsequent event is
     * ignored until the overflow is signaled).
     */
    public void overflow(File file) {
        synchronized (lock) {
            this.overflow = file;
            fileToEvent.clear();
        }
    }

    public void added(File file) {
        synchronized (lock) {
            if (overflow == null) {
                fileToEvent.put(file, ADDED);
            }
        }
    }

    public void removed(File file) {
        synchronized (lock) {
            if (overflow == null) {
                fileToEvent.put(file, REMOVED);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new FastStringBuffer().append("EventsStackerRunnable(key=").appendObject(this.key)
                .append(";watchedPath=").appendObject(this.watchedPath).append(";overflow=")
                .appendObject(this.overflow).append(";fileToEvent=").appendObject(this.fileToEvent)
                .append(";listeners=").appendObject(this.list.getListeners()).append(")").toString();
    }

}