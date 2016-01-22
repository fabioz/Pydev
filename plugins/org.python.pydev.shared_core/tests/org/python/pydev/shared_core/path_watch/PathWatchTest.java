/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.path_watch;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ListenerList;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.testutils.TestUtils;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class PathWatchTest extends TestCase {

    private final class TrackChangesListener implements IFilesystemChangesListener {
        @Override
        public void removed(File file) {
            notifyChange();
        }

        @Override
        public void added(File file) {
            notifyChange();
        }

    }

    private final Object lockToSynchWait = new Object();
    private final Object lockToChange = new Object();
    private volatile boolean changeHappened = false;

    private void notifyChange() {
        synchronized (lockToChange) {
            changeHappened = true;
        }
        synchronized (lockToSynchWait) {
            lockToSynchWait.notifyAll();
        }
    }

    private boolean getChangeHappened() {
        synchronized (lockToChange) {
            return changeHappened;
        }
    }

    private File baseDir;
    private PathWatch pathWatch;

    @Override
    protected void setUp() throws Exception {
        pathWatch = new PathWatch();
        pathWatch.log = new FastStringBuffer(1000);
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("pathwatchtest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    protected void tearDown() throws Exception {
        //System.out.println(PathWatch.log);
        pathWatch.log = null;
        pathWatch.dispose();
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    public void testEventsStackerRunnable() throws Exception {
        pathWatch.log.append("\n\n");
        pathWatch.log.appendN('-', 50);
        pathWatch.log.append("testEventsStackerRunnable\n");
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

            @Override
            public Watchable watchable() {
                throw new RuntimeException("not implemented");
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
                list, baseDir, acceptAllFilter, acceptAllFilter);

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

    private FileFilter pyFilesFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".py");
        }
    };
    private FileFilter acceptAllFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return true;
        }
    };

    public void testPathWatchDirs() throws Exception {
        baseDir.mkdir();
        pathWatch.track(baseDir, new TrackChangesListener());
        File dir = new File(baseDir, "dir");
        dir.mkdir();
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Not expecting any addition when a directory is added inside a directory unless that directory "
                    + "changed its time with interesting content.");
        }
        dir.delete();
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Should not report removal when nothing interesting changed.");
        }
    }

    public void testPathWatchDirs2() throws Exception {
        baseDir.mkdir();
        pathWatch.setDirectoryFileFilter(pyFilesFilter, acceptAllFilter);
        pathWatch.track(baseDir, new TrackChangesListener());
        File dir = new File(baseDir, "dir");
        dir.mkdir();
        File f = new File(dir, "t.txt");
        FileUtils.writeStrToFile("test", f);
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Not expecting any addition when a directory is added inside a directory unless that directory "
                    + "changed its time with interesting content.");
        }
        dir.delete();
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Should not report removal when nothing interesting changed.");
        }
    }

    public void testPathWatchDirs3() throws Exception {
        baseDir.mkdir();
        pathWatch.setDirectoryFileFilter(pyFilesFilter, acceptAllFilter);
        pathWatch.track(baseDir, new TrackChangesListener());
        File f = new File(baseDir, "t.txt");
        FileUtils.writeStrToFile("test", f);
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Not expecting any addition when a directory is added inside a directory unless that directory "
                    + "changed its time with interesting content.");
        }
        f = new File(baseDir, "t.py");
        FileUtils.writeStrToFile("test", f);
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        assertTrue(getChangeHappened());
    }

    public void testPathWatchDirs4() throws Exception {
        baseDir.mkdir();
        pathWatch.setDirectoryFileFilter(pyFilesFilter, acceptAllFilter);
        pathWatch.track(baseDir, new TrackChangesListener());

        File dir = new File(baseDir, "dir");
        pathWatch.log.append("Creating :").appendObject(dir).append('\n');
        dir.mkdir();
        File f = new File(dir, "t.txt");
        pathWatch.log.append("Creating :").appendObject(f).append('\n');
        FileUtils.writeStrToFile("test", f);
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        if (getChangeHappened()) {
            fail("Not expecting any addition when a directory is added inside a directory unless that directory "
                    + "changed its time with interesting content.");
        }
        f = new File(dir, "t.py");
        pathWatch.log.append("Creating :").appendObject(f).append('\n');
        FileUtils.writeStrToFile("test", f);
        waitUntilCondition(new ICallback<String, Object>() {

            @Override
            public String call(Object arg) {
                if (getChangeHappened()) {
                    return null;
                }
                return "No change detected. \nLog:\n" + pathWatch.log;
            }
        });
    }

    public void testPathWatch() throws Exception {
        // This test passes if run on its own (not even with the other test in the
        // same file)
        pathWatch.log.append("\n\n");
        pathWatch.log.appendN('-', 50);
        pathWatch.log.append("testPathWatch\n");

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

        pathWatch.log.append("--- Will delete base dir files ---\n");
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

        pathWatch.log.append("--- Will delete base dir ---\n");
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

        pathWatch.log.append("--- Will create base dir ---");
        baseDir.mkdir();
        pathWatch.track(baseDir, listener);
        pathWatch.track(baseDir, listener2);

        pathWatch.log.append("--- Will delete base dir--- \n");

        assertTrue(baseDir.delete());

        //JPathWatch did notify us (through an extension) that a tracked directory was removed (i.e.: ExtendedWatchEventKind.KEY_INVALID).
        // Java 1.7 doesn't, so the test below no longer works.
        //
        //waitUntilCondition(new ICallback<String, Object>() {
        //
        //    public String call(Object arg) {
        //        Tuple<String, File>[] array = createChangesArray(changes);
        //        if (array.length == 2) { //2 listeners
        //            for (Tuple<String, File> tuple : array) {
        //                assertEquals("removed", tuple.o1);
        //                assertEquals(baseDir, tuple.o2);
        //            }
        //            return null;
        //        }
        //        return changes.toString();
        //    }
        //});
        //
        //changes.clear();
        //
        //pathWatch.stopTrack(baseDir, listener); //Shouldn't be listening anymore, but just to check if there's some error
        //listener2 not removed (but not there anymore)

        baseDir.mkdir();
        synchronized (lockToSynchWait) {
            lockToSynchWait.wait(300);
        }
        assertEquals(0, changes.size());
    }

    private void waitUntilCondition(ICallback<String, Object> call) {
        try {
            TestUtils.waitUntilCondition(call);
        } catch (AssertionError e1) {
            fail("\nLog:" + pathWatch.log.toString() + "\n----------\n" + e1.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Tuple<String, File>[] createChangesArray(final List<Tuple<String, File>> changes) {
        Tuple<String, File>[] array = changes.toArray(new Tuple[changes.size()]);
        return array;
    }
}
