/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package org.python.pydev.ast.analysis.messages;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IToken;

public interface IMessage {

    /**
     * @see org.eclipse.core.resources.IMarker#SEVERITY_ERROR
     * @see org.eclipse.core.resources.IMarker#SEVERITY_WARNING
     * @see org.eclipse.core.resources.IMarker#SEVERITY_INFO
     * @return this message severity.
     */
    int getSeverity();

    /**
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_DUPLICATED_SIGNATURE
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_NO_SELF
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_REIMPORT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNDEFINED_VARIABLE
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNRESOLVED_IMPORT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNUSED_IMPORT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNUSED_VARIABLE
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNUSED_PARAMETER
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_UNUSED_WILD_IMPORT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_USED_WILD_IMPORT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_NO_EFFECT_STMT
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_INDENTATION_PROBLEM
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_PEP8
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_ARGUMENTS_MISATCH
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#TYPE_INVALID_ENCODING
     * 
     * @return this message type
     */
    int getType();

    /**
     * @return the starting line of the error (starting at 1)
     */
    int getStartLine(IDocument doc);

    /**
     * @param doc 
     * @return the starting col of the error (starting at 1)
     */
    int getStartCol(IDocument doc);

    /**
     * @return the ending line of the error. may be -1 if we are unable to find the end of the token (starting at 1)
     */
    int getEndLine(IDocument doc);

    /**
     * @return the ending col of the error. may be -1 if we are unable to find the end of the token (starting at 1)
     */
    int getEndCol(IDocument doc);

    /**
     * @return the message that should be presented to the user.
     */
    String getMessage();

    /**
     * @return additional info to be added to the marker that will be created by this message. It might be
     * useful for making actions based on the analysis info
     */
    List<String> getAdditionalInfo();

    /**
     * Adds some additional info to the message
     * @param info this is the additional info to add
     */
    void addAdditionalInfo(String info);

    /**
     * @return the message that should be presented to the user in a short way (may be used for abbreviations).
     */
    Object getShortMessage();

    /**
     * @return the generator token for the message
     */
    IToken getGenerator();

}
