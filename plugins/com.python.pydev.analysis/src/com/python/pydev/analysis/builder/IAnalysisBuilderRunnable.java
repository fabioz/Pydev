/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import org.python.pydev.core.concurrency.IRunnableWithMonitor;

public interface IAnalysisBuilderRunnable extends IRunnableWithMonitor {

    public static final int ANALYSIS_CAUSE_BUILDER = 1;
    public static final int ANALYSIS_CAUSE_PARSER = 2;

    public static final int FULL_MODULE = 1;
    public static final int DEFINITIONS_MODULE = 2;

    /**
     * ANALYSIS_CAUSE_BUILDER
     * ANALYSIS_CAUSE_PARSER
     */
    int getAnalysisCause();

    /**
     * Forces the current analysis to stop
     */
    void stopAnalysis();

    /**
     * @return whether the analysis should be forced.
     * 
     * It's used to check if the only the context insensitive information should be done (and the actual analysis
     * is not needed).
     * 
     * boolean onlyRecreateCtxInsensitiveInfo = !forceAnalysis && 
     *      analysisCause == ANALYSIS_CAUSE_BUILDER && 
     *      PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor();
     */
    boolean getForceAnalysis();

    boolean getRunFinished();

    String getModuleName();

    String getAnalysisCauseStr();

    long getDocumentTime();

    long getResourceModificationStamp();
}
