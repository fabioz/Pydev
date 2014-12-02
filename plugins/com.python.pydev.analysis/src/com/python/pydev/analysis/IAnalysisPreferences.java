/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IMiscConstants;

public interface IAnalysisPreferences {

    public static final int TYPE_UNUSED_IMPORT = IMiscConstants.TYPE_UNUSED_IMPORT;
    public static final int TYPE_UNUSED_VARIABLE = 2;
    public static final int TYPE_UNDEFINED_VARIABLE = 3;
    public static final int TYPE_DUPLICATED_SIGNATURE = 4;
    public static final int TYPE_REIMPORT = 5;
    public static final int TYPE_UNRESOLVED_IMPORT = 6;
    public static final int TYPE_NO_SELF = 7;
    public static final int TYPE_UNUSED_WILD_IMPORT = 8;
    public static final int TYPE_UNDEFINED_IMPORT_VARIABLE = 9;
    public static final int TYPE_UNUSED_PARAMETER = 10;
    public static final int TYPE_NO_EFFECT_STMT = 11;
    public static final int TYPE_INDENTATION_PROBLEM = 12;
    public static final int TYPE_UNDEFINED_VARIABLE_IN_SELF = 13; //Generated on demand by the tdd actions
    public static final int TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL = 14;
    public static final int TYPE_PEP8 = 15;
    public static final int TYPE_ARGUMENTS_MISATCH = 16;

    public static final String MSG_TO_IGNORE_TYPE_UNUSED_IMPORT = "@UnusedImport";
    public static final String MSG_TO_IGNORE_TYPE_UNUSED_WILD_IMPORT = "@UnusedWildImport";
    public static final String MSG_TO_IGNORE_TYPE_UNUSED_VARIABLE = "@UnusedVariable";
    public static final String MSG_TO_IGNORE_TYPE_UNDEFINED_VARIABLE = "@UndefinedVariable";
    public static final String MSG_TO_IGNORE_TYPE_DUPLICATED_SIGNATURE = "@DuplicatedSignature";
    public static final String MSG_TO_IGNORE_TYPE_REIMPORT = "@Reimport";
    public static final String MSG_TO_IGNORE_TYPE_UNRESOLVED_IMPORT = "@UnresolvedImport";
    public static final String MSG_TO_IGNORE_TYPE_NO_SELF = "@NoSelf";
    public static final String MSG_TO_IGNORE_TYPE_UNDEFINED_IMPORT_VARIABLE = "@UndefinedVariable";
    public static final String MSG_TO_IGNORE_TYPE_UNUSED_PARAMETER = "@UnusedVariable";
    public static final String MSG_TO_IGNORE_TYPE_NO_EFFECT_STMT = "@NoEffect";
    public static final String MSG_TO_IGNORE_TYPE_INDENTATION_PROBLEM = "@IndentOk";
    public static final String MSG_TO_IGNORE_TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL = "@ReservedAssignment";
    public static final String MSG_TO_IGNORE_TYPE_PEP8 = "@IgnorePep8";
    public static final String MSG_TO_IGNORE_TYPE_ARGUMENTS_MISATCH = "@ArgumentMismatch";

    /**
     * Used to define if the analysis should happen only on save
     */
    public static final int ANALYZE_ON_SAVE = 1;

    /**
     * Used to define if the analysis should happen on any successful parse
     */
    public static final int ANALYZE_ON_SUCCESFUL_PARSE = 2;

    /**
     * @see #ANALYZE_ON_SAVE
     * @see #ANALYZE_ON_SUCCESFUL_PARSE
     * 
     * @return the even that should trigger the analysis 
     */
    int getWhenAnalyze();

    /**
     * @see org.eclipse.core.resources.IMarker#SEVERITY_ERROR
     * @see org.eclipse.core.resources.IMarker#SEVERITY_WARNING
     * @see org.eclipse.core.resources.IMarker#SEVERITY_INFO
     * 
     * @return this message severity.
     */
    int getSeverityForType(int type);

    /**
     * @return whether we should do code analysis
     */
    boolean makeCodeAnalysis();

    /**
     * @return a set with the names that should be ignored when reporting unused variables
     * (this are names 'starting with' that should be ignored)
     * 
     * e.g.: if dummy is in the set, ignore names starting with 'dummy' that will be reported as unused variables
     */
    Set<String> getNamesIgnoredByUnusedVariable();

    /**
     * @return a set with the names of the modules where unused imports should not be reported
     */
    Set<String> getModuleNamePatternsToBeIgnored();

    /**
     * @return a set with the names of the tokens that should be considered in the globals at all times
     */
    Set<String> getTokensAlwaysInGlobals();

    /**
     * @return the message that should be in a line so that a warning of a given type is ignored. 
     * I.e.: @UnusedImport
     */
    String getRequiredMessageToIgnore(int type);

    IAdaptable getProjectAdaptable();
}
