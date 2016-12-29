/**
 * Copyright (c) 2016 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

// Class which is friends with PyUnitView
public class PyUnitViewTestsHolder {

    public static int MAX_RUNS_TO_KEEP = 20;

    private static PyUnitTestRun lastPinned;
    private static PyUnitTestRun currentPinned;
    private static PyUnitTestRun currentSelected;

    public static PyUnitTestRun getLastPinned() {
        return lastPinned;
    }

    public static PyUnitTestRun getCurrentPinned() {
        return currentPinned;
    }

    public static void setCurrentPinned(PyUnitTestRun pin) {
        if (pin != null) {
            PyUnitViewTestsHolder.lastPinned = pin;
        }
        PyUnitViewTestsHolder.currentPinned = pin;
        onPinSelected.call(pin);
    }

    public static void setCurrentTest(PyUnitTestRun result) {
        currentSelected = result;
    }

    public static PyUnitTestRun getCurrentTest() {
        return currentSelected;
    }

    public static final CallbackWithListeners<PyUnitTestRun> onPinSelected = new CallbackWithListeners<>();

    /*default*/ static final Object lockServerListeners = new Object();
    /*default*/ static final LinkedList<PyUnitViewServerListener> serverListeners = new LinkedList<PyUnitViewServerListener>();

    /*default*/ static class DummyPyUnitServer implements IPyUnitServer {

        private IPyUnitLaunch launch;

        public DummyPyUnitServer(IPyUnitLaunch launch) {
            this.launch = launch;
        }

        @Override
        public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {

        }

        @Override
        public IPyUnitLaunch getPyUnitLaunch() {
            return this.launch;
        }
    };

    /*default*/ static Job saveDiskIndexJob = new Job("Save PyUnit test runs") {

        {
            setPriority(Job.BUILD);
            setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            saveTestsRunState(false);
            return Status.OK_STATUS;
        }
    };

    public static void restoreTestsRunState() {
        try {
            File workspaceMetadataFile = getPyUnitTestsDir();
            SortedMap<Integer, File> files = new TreeMap<>();

            // Load existing data on test runs.
            final int i0 = "test_run_".length();
            for (File f : workspaceMetadataFile.listFiles()) {
                String name = f.getName();

                if (name.endsWith(".xml") && name.startsWith("test_run_")) {
                    String val0 = name.substring(i0, name.length() - 4);
                    try {
                        int i = Integer.parseInt(val0);
                        files.put(i, f);
                    } catch (NumberFormatException e) {
                        //ignore
                    }
                }
            }

            // Get the content on the launches pinned.
            String pinInfoContents;
            try {
                pinInfoContents = FileUtils
                        .getFileContents(new File(workspaceMetadataFile, "test_run_pin_info.txt"));
            } catch (Exception e1) {
                pinInfoContents = null;
            }
            if (pinInfoContents == null) {
                pinInfoContents = "";
            }
            List<String> split = StringUtils.splitKeepEmpty(pinInfoContents, '|');
            int currentPin = -1;
            int currentRun = -1;
            int lastPin = -1;
            if (split.size() > 0) {
                try {
                    currentPin = Integer.parseInt(split.get(0));
                } catch (NumberFormatException e) {
                    //ignore
                }
            }
            if (split.size() > 1) {
                try {
                    lastPin = Integer.parseInt(split.get(1));
                } catch (NumberFormatException e) {
                    //ignore
                }
            }
            if (split.size() > 2) {
                try {
                    currentRun = Integer.parseInt(split.get(2));
                } catch (NumberFormatException e) {
                    //ignore
                }
            }

            // Erase the files that we don't want to keep (but take care for keeping the pinned ones).
            int i = 0;
            while (files.size() > MAX_RUNS_TO_KEEP) {
                // Erase the files for the runs that we don't want to keep (and restore the remaining ones).
                Iterator<Entry<Integer, File>> it = files.entrySet().iterator();
                Entry<Integer, File> entry = it.next();
                // We need to prevent from deleting the pinned contents.
                if (entry.getKey() == currentPin) {
                    try {
                        currentPinned = PyUnitTestRun.fromXML(FileUtils.getFileContents(entry.getValue()));
                        setSavedDiskIndex(currentPinned, workspaceMetadataFile, i, entry);
                        i += 1;
                        if (currentPin == lastPin) {
                            lastPinned = currentPinned;
                        }
                        if (currentRun == currentPin) {
                            currentSelected = currentPinned;
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                } else if (entry.getKey() == lastPin) {
                    try {
                        lastPinned = PyUnitTestRun.fromXML(FileUtils.getFileContents(entry.getValue()));
                        setSavedDiskIndex(lastPinned, workspaceMetadataFile, i, entry);

                        if (currentRun == lastPin) {
                            currentSelected = lastPinned;
                        }

                        i += 1;
                    } catch (Exception e) {
                        Log.log(e);
                    }

                } else if (entry.getKey() == currentRun) {
                    try {
                        currentSelected = PyUnitTestRun.fromXML(FileUtils.getFileContents(entry.getValue()));
                        setSavedDiskIndex(currentSelected, workspaceMetadataFile, i, entry);
                        i += 1;
                    } catch (Exception e) {
                        Log.log(e);
                    }
                } else {
                    entry.getValue().delete();
                }
                it.remove();
            }

            Set<Entry<Integer, File>> entrySet = files.entrySet();
            for (Entry<Integer, File> entry : entrySet) {
                File file = entry.getValue();
                try {
                    String fileContents = FileUtils.getFileContents(file);
                    try {
                        PyUnitTestRun testRunRestored = PyUnitTestRun.fromXML(fileContents);
                        setSavedDiskIndex(testRunRestored, workspaceMetadataFile, i, entry);
                        i += 1;

                        // If the pinned files are current files, we have to restore them too.
                        if (entry.getKey() == currentPin) {
                            currentPinned = testRunRestored;
                        }
                        if (entry.getKey() == lastPin) {
                            lastPinned = testRunRestored;
                        }
                        DummyPyUnitServer pyUnitServer = new DummyPyUnitServer(testRunRestored.getPyUnitLaunch());
                        final PyUnitViewServerListener serverListener = new PyUnitViewServerListener(pyUnitServer,
                                testRunRestored);
                        addServerListener(serverListener);
                    } catch (Exception e) {
                        Log.log("Error with contents: " + fileContents, e);
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    private static void setSavedDiskIndex(PyUnitTestRun testRun, File workspaceMetadataFile, int i,
            Entry<Integer, File> entry) {
        if (i != entry.getKey()) {
            // Restart the numbering on the disk (otherwise that number would grow forever as we
            // always start numbering from the last maximum number + 1).
            entry.getValue().renameTo(new File(workspaceMetadataFile, "test_run_" + i + ".xml"));
        }
        testRun.savedDiskIndex = i;
    }

    /**
     * Adds a server listener to the static list of available server listeners. This is needed so that we start
     * to listen to it when the view is restored later on (if it's still not visible).
     */
    protected static void addServerListener(PyUnitViewServerListener serverListener) {
        synchronized (lockServerListeners) {

            if (serverListeners.size() + 1 > MAX_RUNS_TO_KEEP) {
                serverListeners.removeFirst();
            }
            serverListeners.add(serverListener);
        }
    }

    public final static Object saveTestsRunStateLock = new Object();

    /**
     * @param forceWriteUnfinished if true, we'll write even the unfinished test runs.
     */
    public static void saveTestsRunState(boolean forceWriteUnfinished) {
        try {
            List<PyUnitTestRun> lst;
            synchronized (lockServerListeners) {
                lst = new ArrayList<>(serverListeners.size() + 2);
                for (PyUnitViewServerListener pyUnitViewServerListener : serverListeners) {
                    lst.add(pyUnitViewServerListener.getTestRun());
                }

            }
            synchronized (saveTestsRunStateLock) {
                File workspaceMetadataFile = getPyUnitTestsDir();
                int i = 0;

                PyUnitTestRun currPin = currentPinned;
                PyUnitTestRun currRun = currentSelected;
                PyUnitTestRun lastPin = lastPinned;
                boolean foundCurrPin = false;
                boolean foundCurrRun = false;
                boolean foundLast = false;

                for (PyUnitTestRun testRun : lst) {
                    if (testRun == currPin) {
                        foundCurrPin = true;
                    }
                    if (testRun == currRun) {
                        foundCurrRun = true;
                    }
                    if (testRun == lastPin) {
                        foundLast = true;
                    }
                    if (testRun.savedDiskIndex != null && testRun.savedDiskIndex >= i) {
                        i = testRun.savedDiskIndex + 1;
                    }
                }

                if (!foundCurrPin && currPin != null) {
                    lst.add(currPin);
                    if (currPin.savedDiskIndex != null && currPin.savedDiskIndex >= i) {
                        i = currPin.savedDiskIndex + 1;
                    }
                }
                if (!foundLast && lastPin != null) {
                    lst.add(lastPin);
                    if (lastPin.savedDiskIndex != null && lastPin.savedDiskIndex >= i) {
                        i = lastPin.savedDiskIndex + 1;
                    }
                }
                if (!foundCurrRun && currRun != null) {
                    lst.add(currRun);
                    if (currRun.savedDiskIndex != null && currRun.savedDiskIndex >= i) {
                        i = currRun.savedDiskIndex + 1;
                    }
                }

                for (PyUnitTestRun testRun : lst) {
                    // We want to write only the deltas, so, skip it if it's already saved
                    if (testRun.savedDiskIndex != null) {
                        continue;
                    }
                    if (!forceWriteUnfinished) {
                        if (!testRun.getFinished()) {
                            continue;
                        }
                    }
                    String xml = testRun.toXML();
                    File f = new File(workspaceMetadataFile,
                            "test_run_" + i + ".xml");
                    FileUtils.writeBytesToFile(xml.getBytes(StandardCharsets.UTF_8), f);
                    testRun.savedDiskIndex = i;
                    i += 1;
                }

                FastStringBuffer buf = new FastStringBuffer();
                if (currPin != null && currPin.savedDiskIndex != null) {
                    buf.append(currPin.savedDiskIndex);
                }
                buf.append('|');
                if (lastPin != null && lastPin.savedDiskIndex != null) {
                    buf.append(lastPin.savedDiskIndex);
                }
                buf.append('|');
                if (currRun != null && currRun.savedDiskIndex != null) {
                    buf.append(currRun.savedDiskIndex);
                }
                FileUtils.writeBytesToFile(buf.getBytes(), new File(workspaceMetadataFile, "test_run_pin_info.txt"));
            }

        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @return the directory to save the files
     */
    private static File getPyUnitTestsDir() {
        File workspaceMetadataFile = PydevPlugin.getWorkspaceMetadataFile("pyunit_tests");
        if (!workspaceMetadataFile.exists()) {
            workspaceMetadataFile.mkdirs();
        }
        return workspaceMetadataFile;
    }

}
