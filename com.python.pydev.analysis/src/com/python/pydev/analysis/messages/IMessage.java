/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.revisited.IToken;

public interface IMessage {
    
    /**
     * @see org.eclipse.core.resources.IMarker#SEVERITY_ERROR
     * @see org.eclipse.core.resources.IMarker#SEVERITY_WARNING
     * @see org.eclipse.core.resources.IMarker#SEVERITY_INFO
     * @return this message severity.
     */
    int getSeverity();
    
    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_DUPLICATED_SIGNATURE
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_NO_SELF
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_REIMPORT
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNDEFINED_VARIABLE
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNRESOLVED_IMPORT
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNUSED_IMPORT
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNUSED_VARIABLE
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNUSED_WILD_IMPORT
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_USED_WILD_IMPORT
     * 
     * @return this message type
     */
    int getType();

    /**
     * @return the starting line of the error
     */
    int getStartLine(IDocument doc);
    
    /**
     * @param doc 
     * @return the starting col of the error
     */
    int getStartCol(IDocument doc);

    /**
     * @return the ending line of the error. may be -1 if we are unable to find the end of the token
     */
    int getEndLine(IDocument doc);

    /**
     * @return the ending col of the error. may be -1 if we are unable to find the end of the token
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
