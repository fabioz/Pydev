/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.path_watch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;

import org.python.pydev.core.ListenerList;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 *
 */
public class PathWatchTest extends TestCase {

    private File baseDir;

    @Override
    protected void setUp() throws Exception {
        PathWatch.log = new FastStringBuffer(1000);
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("pathwatchtest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    protected void tearDown() throws Exception {
        System.out.println(PathWatch.log);
        PathWatch.log = null;
        FileUtils.deleteDirectoryTree(baseDir);
    }

    public void testEventsStackerRunnable() throws Exception {
        PathWatch.log.append("\n\n");
        PathWatch.log.appendN('-', 50);
        PathWatch.log.append("testEventsStackerRunnable\n");
        WatchKey key = new WatchKey() {

            @Override
            public boolean reset() {
                return false;
            }

            @Override
            public List<WatchEvent<?>> pollEvents() {
                return null;
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
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

        EventsStackerRunnable stack = new EventsStackerRunnable(key, Paths.get(FileUtils.getFileAbsolutePath(baseDir)),
                list);

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
        // This test passes if run on its own (not even with the other test in the
        // same file)
        PathWatch.log.append("\n\n");
        PathWatch.log.appendN('-', 50);
        PathWatch.log.append("testPathWatch\n");

        final PathWatch pathWatch = PathWatch.get();
        baseDir.mkdir();

        final List<Tuple<String, File>> changes = Collections.synchronizedList(new ArrayList<Tuple<String, File>>());
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
            FileUtils.writeStrToFile("FILE1", new File(baseDir, "f" + i + ".txt"));
        }

        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                Tuple<String, File>[] array = createChangesArray(changes);

                HashSet<Tuple<String, File>> set = new HashSet<>(Arrays.asList(array));
                Set<String> filesChanged = new HashSet<>();
                for (Tuple<String, File> tuple : array) {
                    if (tuple.o1.equals("added")) {
                        filesChanged.add(FileUtils.getFileAbsolutePath(tuple.o2));
                    }
                }
                if (set.size() == 5) {
                    return null;
                }
                return changes.toString();
            }
        });
        changes.clear();

        PathWatch.log.append("\n--- Will delete base dir files ---\n");
        File[] files = baseDir.listFiles();
        if (files != null) {

            for (int i = 0; i < files.length; ++i) {
                File f = files[i];
                f.delete();
            }
        }
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {

                int foundRemovals = 0;
                Tuple<String, File>[] array = createChangesArray(changes);

                for (Tuple<String, File> tuple : array) {
                    if (tuple.o1.equals("removed")) {
                        foundRemovals += 1;
                    }
                }
                if (foundRemovals == 5) {
                    return null;
                }
                return changes.toString();
            }
        });

        changes.clear();

        PathWatch.log.append("--- Will delete base dir ---\n");
        assertTrue(baseDir.delete());

        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {

                Tuple<String, File>[] array = createChangesArray(changes);

                if (array.length == 1) {
                    for (Tuple<String, File> tuple : array) {
                        assertEquals("removed", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });
        changes.clear();

        PathWatch.log.append("\n--- Will create base dir ---");
        baseDir.mkdir();
        pathWatch.track(baseDir, listener);
        pathWatch.track(baseDir, listener2);

        PathWatch.log.append("\n--- Will delete base dir--- \n");

        assertTrue(baseDir.delete());
        waitUntilCondition(new ICallback<String, Object>() {

            public String call(Object arg) {
                Tuple<String, File>[] array = createChangesArray(changes);
                if (array.length == 2) { //2 listeners
                    for (Tuple<String, File> tuple : array) {
                        assertEquals("removed", tuple.o1);
                        assertEquals(baseDir, tuple.o2);
                    }
                    return null;
                }
                return changes.toString();
            }
        });

        changes.clear();

        pathWatch.stopTrack(baseDir, listener); //Shouldn't be listening anymore, but just to check if there's some error
        //listener2 not removed (but not thre anymore)

        baseDir.mkdir();
        try {
            synchronized (this) {
                wait(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(0, changes.size());
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

    @SuppressWarnings("unchecked")
    private Tuple<String, File>[] createChangesArray(final List<Tuple<String, File>> changes) {
        Tuple<String, File>[] array = changes.toArray(new Tuple[changes.size()]);
        return array;
    }
}
