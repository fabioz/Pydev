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
    public static final int TYPE_UNUSED_VARIABLE = IMiscConstants.TYPE_UNUSED_VARIABLE;
    public static final int TYPE_UNDEFINED_VARIABLE = IMiscConstants.TYPE_UNDEFINED_VARIABLE;
    public static final int TYPE_DUPLICATED_SIGNATURE = IMiscConstants.TYPE_DUPLICATED_SIGNATURE;
    public static final int TYPE_REIMPORT = IMiscConstants.TYPE_REIMPORT;
    public static final int TYPE_UNRESOLVED_IMPORT = IMiscConstants.TYPE_UNRESOLVED_IMPORT;
    public static final int TYPE_NO_SELF = IMiscConstants.TYPE_NO_SELF;
    public static final int TYPE_UNUSED_WILD_IMPORT = IMiscConstants.TYPE_UNUSED_WILD_IMPORT;
    public static final int TYPE_UNDEFINED_IMPORT_VARIABLE = IMiscConstants.TYPE_UNDEFINED_IMPORT_VARIABLE;
    public static final int TYPE_UNUSED_PARAMETER = IMiscConstants.TYPE_UNUSED_PARAMETER;
    public static final int TYPE_NO_EFFECT_STMT = IMiscConstants.TYPE_NO_EFFECT_STMT;
    public static final int TYPE_INDENTATION_PROBLEM = IMiscConstants.TYPE_INDENTATION_PROBLEM;
    public static final int TYPE_UNDEFINED_VARIABLE_IN_SELF = IMiscConstants.TYPE_UNDEFINED_VARIABLE_IN_SELF;
    public static final int TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL = IMiscConstants.TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL;
    public static final int TYPE_PEP8 = IMiscConstants.TYPE_PEP8;
    public static final int TYPE_ARGUMENTS_MISATCH = IMiscConstants.TYPE_ARGUMENTS_MISATCH;
    public static final int TYPE_FSTRING_SYNTAX_ERROR = IMiscConstants.TYPE_FSTRING_SYNTAX_ERROR;

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
