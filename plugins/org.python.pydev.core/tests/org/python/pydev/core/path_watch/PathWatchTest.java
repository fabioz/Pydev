/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.path_watch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;

import org.python.pydev.core.ListenerList;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * @author fabioz
 *
 */
public class PathWatchTest extends TestCase {

    private File baseDir;

    @Override
    protected void setUp() throws Exception {
        PathWatch.log = new FastStringBuffer(1000);
        baseDir = new File(REF.getFileAbsolutePath(new File("pathwatchtest.temporary_dir")));
        try {
            REF.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    protected void tearDown() throws Exception {
        System.out.println(PathWatch.log);
        PathWatch.log = null;
        REF.deleteDirectoryTree(baseDir);
    }

    public void testEventsStackerRunnable() throws Exception {
        PathWatch.log.append("\n\n");
        PathWatch.log.appendN('-', 50);
        PathWatch.log.append("testEventsStackerRunnable\n");
        WatchKey key = new WatchKey() {

            public boolean reset() {
                return false;
            }

            public List<WatchEvent<?>> pollEvents() {
                return null;
            }

            public boolean isValid() {
                return true;
            }

            public void cancel() {
            }
        };
        final List<Tuple<String, File>> changes = new ArrayList<Tuple<String, File>>();
        ListenerList<IFilesystemChangesListener> list = new ListenerList<IFilesystemChangesListener>(
                IFilesystemChangesListener.class);
        list.add(new IFilesystemChangesListener() {

            public void removed(File file) {
                changes.add(new Tuple<String, File>("removed", file));
            }

            public void added(File file) {
                changes.add(new Tuple<String, File>("added", file));
            }
        });

        EventsStackerRunnable stack = new EventsStackerRunnable(key, Paths.get(REF.getFileAbsolutePath(baseDir)), list);

        stack.run();
        assertEquals(0, changes.size());

        stack.added(new File(baseDir, "f1.txt"));
        stack.removed(new File(baseDir, "f1.txt"));
        stack.run();

        assertEquals(1, changes.size());
        assertEquals("removed", changes.get(0).o1);
        changes.clear();

        stack.added(new File(baseDir, "f1.txt"));
        stack.run();

        assertEquals(1, changes.size());
        assertEquals("added", changes.get(0).o1);
        changes.clear();

        stack.added(new File(baseDir, "f1.txt"));
        stack.removed(new File(baseDir, "f1.txt"));
        stack.overflow(baseDir);
        stack.added(new File(baseDir, "f1.txt"));
        stack.removed(new File(baseDir, "f1.txt"));

        stack.run();

        assertEquals(1, changes.size());
        assertEquals("removed", changes.get(0).o1);
        changes.clear();

        stack.run();
        assertEquals(0, changes.size());

        stack.overflow(baseDir);
        baseDir.mkdir();
        stack.run();
        assertEquals(2, changes.size());
        assertEquals("removed", changes.get(0).o1);
        assertEquals("added", changes.get(1).o1);
        changes.clear();
    }

    public void testPathWatch() throws Exception {
        PathWatch.log.append("\n\n");
        PathWatch.log.appendN('-', 50);
        PathWatch.log.append("testPathWatch\n");

        final PathWatch pathWatch = PathWatch.get();
        baseDir.mkdir();

        PathWatch.TIME_BEFORE_NOTIFY = 0;
        PathWatch.RECHECK_INVALID_PATHS_EACH = 0;
        final List<Tuple<String, File>> changes = new ArrayList<Tuple<String, File>>();
        IFilesystemChangesListener listener = new IFilesystemChangesListener() {

            public void removed(File file) {
                changes.add(new Tuple<String, File>("removed", file));
            }

            public void added(File file) {
                changes.add(new Tuple<String, File>("added", file));
            }
        };
        IFilesystemChangesListener listener2 = new IFilesystemChangesListener() {

            public void removed(File file) {
                changes.add(new Tuple<String, File>("removed", file));
            }

            public void added(File file) {
                changes.add(new Tuple<String, File>("added", file));
            }
        };

        pathWatch.track(baseDir, listener);

        for (int i = 0; i < 5; i++) {
            REF.writeStrToFile("FILE1", new File(baseDir, "f" + i + ".txt"));
        }

        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {

                HashSet<Tuple<String, File>> set = new HashSet<Tuple<String, File>>(changes);
                if (set.size() == 5) {
                    for (Tuple<String, File> tuple : set) {
                        assertEquals("added", tuple.o1);
                    }
                    return null;
                }
                return changes.toString();
            }
        });
        changes.clear();

        File[] files = baseDir.listFiles();
        if (files != null) {

            for (int i = 0; i < files.length; ++i) {
                File f = files[i];
                f.delete();
            }
        }
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {

                if (changes.size() == 5) {
                    for (Tuple<String, File> tuple : changes) {
                        assertEquals("removed", tuple.o1);
                    }
                    return null;
                }
                return changes.toString();
            }
        });

        changes.clear();

        assertTrue(baseDir.delete());
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {

                if (changes.size() == 1) {
                    for (Tuple<String, File> tuple : changes) {
                        assertEquals("removed", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });
        changes.clear();
        pathWatch.track(baseDir, listener2);

        baseDir.mkdir();
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                if (changes.size() == 2) {
                    for (Tuple<String, File> tuple : changes) {
                        assertEquals("added", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });

        changes.clear();

        PathWatch.log.append("testPathWatch: deleteBaseDir\n");

        assertTrue(baseDir.delete());
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                if (changes.size() == 2) {
                    for (Tuple<String, File> tuple : changes) {
                        assertEquals("removed", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });

        changes.clear();
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                Set<EventsStackerRunnable> invalidPaths = pathWatch.getInvalidPaths();
                if (invalidPaths.size() == 1) {
                    return null;
                }
                return invalidPaths.toString();
            }
        });
        pathWatch.stopTrack(baseDir, listener);
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                Set<EventsStackerRunnable> invalidPaths = pathWatch.getInvalidPaths();
                if (invalidPaths.size() == 1) {
                    return null;
                }
                return invalidPaths.toString();
            }
        });
        pathWatch.stopTrack(baseDir, listener2);
        assertEquals(0, pathWatch.getInvalidPaths().size());

        baseDir.mkdir();
        try {
            synchronized (this) {
                wait(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(0, changes.size());

        assertTrue(baseDir.delete());
        changes.clear();

        pathWatch.track(baseDir, listener);
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                Set<EventsStackerRunnable> invalidPaths = pathWatch.getInvalidPaths();
                if (invalidPaths.size() == 1) {
                    return null;
                }
                return invalidPaths.toString();
            }
        });
        baseDir.mkdir();
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                if (changes.size() == 1) {
                    for (Tuple<String, File> tuple : changes) {
                        assertEquals("added", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });
        assertEquals(0, pathWatch.getInvalidPaths().size());

        changes.clear();
    }

    private void waitUntilCondition(ICallback<String, Object> call) {
        long currentTimeMillis = System.currentTimeMillis();
        String msg = null;
        while (System.currentTimeMillis() < currentTimeMillis + 2000) { //at most 2 seconds
            msg = call.call(null);
            if (msg == null) {
                return;
            }
            synchronized (this) {
                try {
                    wait(25);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        fail("Condition not satisfied in 2 seconds." + msg + "\nLog:" + PathWatch.log.toString());
    }
}
