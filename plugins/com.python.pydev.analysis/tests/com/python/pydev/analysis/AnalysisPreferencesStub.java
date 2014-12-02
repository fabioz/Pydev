/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;

public final class AnalysisPreferencesStub extends AbstractAnalysisPreferences {
    public int severityForUnusedImport;
    public int severityForUnusedVariable;
    public int severityForUndefinedVariable;
    public int severityForDuplicatedSignature;
    public int severityForReimport;
    public int severityForUnresolvedImport;
    public int severityForNoSelf;
    public int severityForUnusedWildImport;
    public int severityForUndefinedImportVariable;
    public int severityForUnusedParameter;
    public int severityForNoEffectStmt;
    public int severityForIndentationProblem;
    public int severityForInvalidModuleName;
    public int severityForAssignmentToBuiltInSymbol;
    public int severityForArgumentsMismatch;

    public AnalysisPreferencesStub() {
        severityForUnusedImport = IMarker.SEVERITY_WARNING;
        severityForUnusedVariable = IMarker.SEVERITY_WARNING;
        severityForUndefinedVariable = IMarker.SEVERITY_ERROR;
        severityForDuplicatedSignature = IMarker.SEVERITY_ERROR;
        severityForReimport = IMarker.SEVERITY_WARNING;
        severityForUnresolvedImport = IMarker.SEVERITY_ERROR;
        severityForNoSelf = IMarker.SEVERITY_ERROR;
        severityForUnusedWildImport = IMarker.SEVERITY_WARNING;
        severityForUndefinedImportVariable = IMarker.SEVERITY_WARNING;
        severityForUnusedParameter = IMarker.SEVERITY_WARNING;
        severityForNoEffectStmt = IMarker.SEVERITY_WARNING;
        severityForIndentationProblem = IMarker.SEVERITY_WARNING;
        severityForInvalidModuleName = IMarker.SEVERITY_WARNING;
        severityForAssignmentToBuiltInSymbol = IMarker.SEVERITY_WARNING;
        severityForArgumentsMismatch = IMarker.SEVERITY_INFO;
    }

    public int getSeverityForType(int type) {
        switch (type) {
            case TYPE_UNUSED_IMPORT:
                return severityForUnusedImport;

            case TYPE_UNUSED_VARIABLE:
                return severityForUnusedVariable;

            case TYPE_UNDEFINED_VARIABLE:
                return severityForUndefinedVariable;

            case TYPE_DUPLICATED_SIGNATURE:
                return severityForDuplicatedSignature;

            case TYPE_REIMPORT:
                return severityForReimport;

            case TYPE_UNRESOLVED_IMPORT:
                return severityForUnresolvedImport;

            case TYPE_NO_SELF:
                return severityForNoSelf;

            case TYPE_UNUSED_WILD_IMPORT:
                return severityForUnusedWildImport;

            case TYPE_UNDEFINED_IMPORT_VARIABLE:
                return severityForUndefinedImportVariable;

            case TYPE_UNUSED_PARAMETER:
                return severityForUnusedParameter;

            case TYPE_NO_EFFECT_STMT:
                return severityForNoEffectStmt;

            case TYPE_PEP8:
                return IMarker.SEVERITY_INFO;

            case TYPE_ARGUMENTS_MISATCH:
                return severityForArgumentsMismatch;

            case TYPE_INDENTATION_PROBLEM:
                return severityForIndentationProblem;

            case TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL:
                return severityForAssignmentToBuiltInSymbol;
        }
        throw new RuntimeException("unable to get severity for type " + type);
    }

    public boolean makeCodeAnalysis() {
        return true;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public Set<String> getNamesIgnoredByUnusedVariable() {
        Set<String> names = new HashSet<String>();
        names.add("dummy");
        return names;
    }

    public Set<String> getModuleNamePatternsToBeIgnored() {
        Set<String> names = new HashSet<String>();
        names.add("__init__");
        return names;
    }

    public Set<String> getTokensAlwaysInGlobals() {
        Set<String> names = new HashSet<String>();
        names.add("considerGlobal");
        return names;
    }

    public int getWhenAnalyze() {
        return IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE;
    }

    @Override
    public IAdaptable getProjectAdaptable() {
        return null;
    }
}