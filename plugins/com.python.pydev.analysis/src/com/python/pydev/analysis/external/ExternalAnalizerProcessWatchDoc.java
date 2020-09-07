package com.python.pydev.analysis.external;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.ThreadStreamReader;

public final class ExternalAnalizerProcessWatchDoc extends Thread {
    private final IExternalCodeAnalysisStream out;
    private final IProgressMonitor monitor;
    private final IExternalAnalyzer externalAnalyzer;
    private final ICallback0<Process> launchProcessCallback;
    private final boolean useProjectLock;
    private final IProject project;

    public ExternalAnalizerProcessWatchDoc(IExternalCodeAnalysisStream out, IProgressMonitor monitor,
            IExternalAnalyzer externalAnalyzer, ICallback0<Process> launchProcessCallback, IProject project,
            boolean useProjectLock) {
        this.setDaemon(true);
        this.out = out;
        this.monitor = monitor;
        this.externalAnalyzer = externalAnalyzer;
        this.launchProcessCallback = launchProcessCallback;
        this.project = project;
        this.useProjectLock = useProjectLock;
        if (useProjectLock) {
            Assert.isNotNull(project);
        }
    }

    private static Map<IProject, Semaphore> projectToSemaphore = new HashMap<>();

    private static Semaphore getProjectSemaphore(final IProject project) {
        synchronized (projectToSemaphore) {
            Semaphore semaphore = projectToSemaphore.get(project);
            if (semaphore != null) {
                return semaphore;
            } else {
                semaphore = new Semaphore(1);
                projectToSemaphore.put(project, semaphore);
                return semaphore;
            }
        }
    }

    @Override
    public void run() {
        Semaphore semaphore = null;
        if (useProjectLock) {
            semaphore = getProjectSemaphore(project);
            try {
                semaphore.acquire();
            } catch (InterruptedException e1) {
                Log.log(e1); // Bail out if interrupted.
                return;
            }
        }
        ThreadStreamReader std;
        ThreadStreamReader err;
        try {
            Process process;
            try {
                process = launchProcessCallback.call();
            } catch (Exception e1) {
                Log.log(e1);
                return;
            }
            //No need to synchronize as we'll waitFor() the process before getting the contents.
            std = new ThreadStreamReader(process.getInputStream(), false, null);
            err = new ThreadStreamReader(process.getErrorStream(), false, null);
            std.start();
            err.start();
            while (process.isAlive()) {
                if (monitor.isCanceled()) {
                    std.stopGettingOutput();
                    err.stopGettingOutput();
                    return;
                }
                synchronized (this) {
                    try {
                        this.wait(20);
                    } catch (InterruptedException e) {
                        // Just proceed to another check.
                    }
                }
            }
        } finally {
            if (semaphore != null) {
                semaphore.release();
            }
        }

        if (monitor.isCanceled()) {
            std.stopGettingOutput();
            err.stopGettingOutput();
            return;
        }
        // Wait for the other threads to finish getting the output
        try {
            std.join();
        } catch (InterruptedException e) {
        }
        try {
            err.join();
        } catch (InterruptedException e) {
        }
        if (monitor.isCanceled()) {
            std.stopGettingOutput();
            err.stopGettingOutput();
            return;
        }
        String output = std.getAndClearContents();
        String errors = err.getAndClearContents();
        this.externalAnalyzer.afterRunProcess(output, errors, out);
    }
}