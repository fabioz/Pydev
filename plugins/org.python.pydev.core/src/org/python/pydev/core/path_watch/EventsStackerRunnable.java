/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.path_watch;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import name.pachler.nio.file.Path;
import name.pachler.nio.file.WatchKey;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.ListenerList;
import org.python.pydev.core.OrderedMap;
import org.python.pydev.core.structure.FastStringBuffer;

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
     * Creates the events stacker based on the key, path and listeners related (the contents of the listeners may 
     * change later on, but the actual key and path may not change).
     */
    public EventsStackerRunnable(WatchKey key, Path watchedPath, ListenerList<IFilesystemChangesListener> list) {
        Assert.isNotNull(list);
        Assert.isNotNull(watchedPath);
        this.list = list;
        this.key = key; //the key may be null!
        this.watchedPath = watchedPath;
    }

    /**
     * When run, it'll notify clients about the events that were stacked as it makes sense (i.e.: if multiple additions
     * or removals of a file were issued, only the last one will actually be seen by clients).
     */
    public void run() {
        Map<File, Integer> currentFileToEvent;
        File currentOverflow;
        synchronized (lock) {
            currentFileToEvent = fileToEvent;
            fileToEvent = new OrderedMap<File, Integer>();
            currentOverflow = overflow;
            overflow = null;
        }

        IFilesystemChangesListener[] listeners = list.getListeners();
        if (listeners.length > 0) {
            if (currentOverflow != null) {
                for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                    //Say that the dir was removed...
                    File watched = new File(watchedPath.toString());
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

                switch (value) {
                    case ADDED:
                        for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                            iFilesystemChangesListener.added(currKey);
                        }
                        break;

                    case REMOVED:
                        for (IFilesystemChangesListener iFilesystemChangesListener : listeners) {
                            iFilesystemChangesListener.removed(currKey);
                        }
                        break;
                }
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