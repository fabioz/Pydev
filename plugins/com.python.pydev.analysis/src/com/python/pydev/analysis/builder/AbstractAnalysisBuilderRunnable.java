/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;


/**
 * Abstract class for the builder runnables.
 * 
 * @author Fabio
 */
public abstract class AbstractAnalysisBuilderRunnable implements IAnalysisBuilderRunnable {

    // -------------------------------------------------------------------------------------------- ATTRIBUTES
    protected IProgressMonitor monitorSetExternally;

    //from IRunnableWithMonitor
    @Override
    public void setMonitor(IProgressMonitor monitor) {
        monitorSetExternally = monitor;
    }

    final protected IProgressMonitor internalCancelMonitor = new NullProgressMonitor() {
        @Override
        public final boolean isCanceled() {
            if (super.isCanceled()) {
                return true;
            }
            IProgressMonitor ext = AbstractAnalysisBuilderRunnable.this.monitorSetExternally;
            if (ext != null && ext.isCanceled()) {
                return true;
            }
            return false;
        };
    };

    final protected String moduleName;
    final protected boolean isFullBuild;
    final protected boolean forceAnalysis;
    final protected int analysisCause;
    final protected KeyForAnalysisRunnable key;
    final private Object lock = new Object();

    protected IPythonNature nature;
    protected volatile boolean runFinished = false;
    private IAnalysisBuilderRunnable oldAnalysisBuilderThread;
    private long documentTime;
    private long resourceModificationStamp;

    // ---------------------------------------------------------------------------------------- END ATTRIBUTES

    public AbstractAnalysisBuilderRunnable(boolean isFullBuild, String moduleName, boolean forceAnalysis,
            int analysisCause, IAnalysisBuilderRunnable oldAnalysisBuilderThread, IPythonNature nature,
            long documentTime, KeyForAnalysisRunnable key, long resourceModificationStamp) {
        this.isFullBuild = isFullBuild;
        this.moduleName = moduleName;
        this.forceAnalysis = forceAnalysis;
        this.analysisCause = analysisCause;
        this.oldAnalysisBuilderThread = oldAnalysisBuilderThread;
        this.nature = nature;
        this.documentTime = documentTime;
        this.key = key;
        this.resourceModificationStamp = resourceModificationStamp;
    }

    @Override
    public long getDocumentTime() {
        return documentTime;
    }

    @Override
    public long getResourceModificationStamp() {
        return resourceModificationStamp;
    }

    @Override
    public int getAnalysisCause() {
        return analysisCause;
    }

    @Override
    public boolean getForceAnalysis() {
        return forceAnalysis;
    }

    @Override
    public synchronized boolean getRunFinished() {
        return runFinished;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getAnalysisCauseStr() {
        String analysisCauseStr;
        if (analysisCause == ANALYSIS_CAUSE_BUILDER) {
            analysisCauseStr = "Builder";
        } else if (analysisCause == ANALYSIS_CAUSE_PARSER) {
            analysisCauseStr = "Parser";
        } else {
            analysisCauseStr = "Unknown?";
        }
        return analysisCauseStr;
    }

    protected void logOperationCancelled() {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(this, "OperationCanceledException: cancelled by new runnable -- " + moduleName
                    + ". Cancelled was from: " + getAnalysisCauseStr());
        }
    }

    /**
     * The run for this runnable will only start if there's no other runnable active for the same module.
     * 
     * This method will do that and call doAnalysis() if it hasn't been cancelled itself.
     */
    @Override
    public void run() {
        try {
            try {
                if (oldAnalysisBuilderThread != null) {
                    if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                        Log.toLogFile(this, "Waiting for other to be finished...");
                    }

                    //just to make sure that the analysis of the existing runnable had a request for stopping already
                    oldAnalysisBuilderThread.stopAnalysis();

                    int attempts = 0;
                    while (!oldAnalysisBuilderThread.getRunFinished()) {
                        attempts += 1;
                        synchronized (lock) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                //ignore
                            }
                        }
                    }
                    if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                        Log.toLogFile(this, "Starting analysis after attempts: " + attempts);
                    }
                }
                //that's all we need it for... we can already dispose of it.
                this.oldAnalysisBuilderThread = null;

                if (!internalCancelMonitor.isCanceled()) {
                    doAnalysis();
                } else {
                    logOperationCancelled();
                }
            } catch (NoClassDefFoundError e) {
                //ignore, plugin finished and thread still active
            }

        } catch (Exception e) {
            Log.log(e);
        } finally {
            try {
                AnalysisBuilderRunnableFactory.removeFromThreads(key, this);
            } catch (Throwable e) {
                Log.log(e);
            } finally {
                runFinished = true;
            }

            dispose();
        }
    }

    protected void dispose() {
        this.nature = null;
        this.oldAnalysisBuilderThread = null;
    }

    /**
     * This method should be overridden to actually make the action that this analysis triggered.
     */
    protected abstract void doAnalysis();

    /**
     * Stops the analysis whenever it gets a chance to do so.
     */
    @Override
    public synchronized void stopAnalysis() {
        this.internalCancelMonitor.setCanceled(true);
    }

    private final static OperationCanceledException operationCanceledException = new OperationCanceledException();

    /**
     * Checks if the analysis in this runnable should be stopped (raises an OperationCanceledException if it should be stopped)
     */
    protected void checkStop() {
        if (this.internalCancelMonitor.isCanceled()) {
            throw operationCanceledException;
        }
    }

}
