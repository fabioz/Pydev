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
import org.python.pydev.shared_core.io.FileUtils;

// Class which is friends with PyUnitView
public class PyUnitViewTestsHolder {

    public static int MAX_RUNS_TO_KEEP = 20;

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

            while (files.size() > MAX_RUNS_TO_KEEP) {
                // Erase the files for the runs that we don't want to keep (and restore the remaining ones).
                Iterator<Entry<Integer, File>> it = files.entrySet().iterator();
                Entry<Integer, File> entry = it.next();
                it.remove();
                entry.getValue().delete();
            }

            int i = 0;
            Set<Entry<Integer, File>> entrySet = files.entrySet();
            for (Entry<Integer, File> entry : entrySet) {
                File file = entry.getValue();
                try {
                    PyUnitTestRun testRunRestored = PyUnitTestRun.fromXML(FileUtils.getFileContents(file));
                    if (i != entry.getKey()) {
                        // Restart the numbering on the disk (otherwise that number would grow forever as we
                        // always start numbering from the last maximum number + 1).
                        entry.getValue().renameTo(new File(workspaceMetadataFile, "test_run_" + i + ".xml"));
                    }
                    testRunRestored.savedDiskIndex = i;
                    DummyPyUnitServer pyUnitServer = new DummyPyUnitServer(testRunRestored.getPyUnitLaunch());

                    final PyUnitViewServerListener serverListener = new PyUnitViewServerListener(pyUnitServer,
                            testRunRestored);
                    addServerListener(serverListener);

                } catch (Exception e) {
                    Log.log(e);
                }
                i += 1;
            }
        } catch (Exception e) {
            Log.log(e);
        }
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
            List<PyUnitViewServerListener> lst;
            synchronized (lockServerListeners) {
                lst = new ArrayList<>(serverListeners);
            }
            synchronized (saveTestsRunStateLock) {
                File workspaceMetadataFile = getPyUnitTestsDir();
                int i = 0;
                for (PyUnitViewServerListener pyUnitViewServerListener : lst) {
                    PyUnitTestRun testRun = pyUnitViewServerListener.getTestRun();
                    if (testRun.savedDiskIndex != null && testRun.savedDiskIndex >= i) {
                        i = testRun.savedDiskIndex + 1;
                    }
                }

                for (PyUnitViewServerListener pyUnitViewServerListener : lst) {
                    PyUnitTestRun testRun = pyUnitViewServerListener.getTestRun();
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
