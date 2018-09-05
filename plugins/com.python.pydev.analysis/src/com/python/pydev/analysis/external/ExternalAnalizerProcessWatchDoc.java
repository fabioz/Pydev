package com.python.pydev.analysis.external;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.shared_core.io.ThreadStreamReader;

public final class ExternalAnalizerProcessWatchDoc extends Thread {
    private final IExternalCodeAnalysisStream out;
    private final Process process;
    private final IProgressMonitor monitor;
    private final IExternalAnalyzer externalAnalyzer;

    public ExternalAnalizerProcessWatchDoc(IExternalCodeAnalysisStream out, IProgressMonitor monitor, Process process,
            IExternalAnalyzer externalAnalyzer) {
        this.setDaemon(true);
        this.out = out;
        this.process = process;
        this.monitor = monitor;
        this.externalAnalyzer = externalAnalyzer;
    }

    @Override
    public void run() {
        //No need to synchronize as we'll waitFor() the process before getting the contents.
        ThreadStreamReader std = new ThreadStreamReader(process.getInputStream(), false, null);
        ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream(), false, null);

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