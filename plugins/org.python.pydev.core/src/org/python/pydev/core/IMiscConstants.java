package org.python.pydev.core;

/**
 * This file is intended for constants that are shared
 * between plugins, to avoid otherwise unneeded dependencies
 * particularly cicrular ones, or spurious extension points.
 * 
 * @author Jeremy J Carroll
 *
 */
public interface IMiscConstants {

    String PYDEV_ANALYSIS_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";
    String PYDEV_ANALYSIS_TYPE = "PYDEV_TYPE";
    String ANALYSIS_PARSER_OBSERVER_FORCE = "AnalysisParserObserver:force";
    int TYPE_UNUSED_IMPORT = 1;
}
