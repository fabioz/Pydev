package com.python.pydev.analysis.external;

public interface IExternalAnalyzer {

    void afterRunProcess(String output, String errors, IExternalCodeAnalysisStream out);

    /**
     * Waits for the PyLint processing to finish (note that canceling the monitor should also
     * stop the analysis/kill the related process).
     */
    void join();

}