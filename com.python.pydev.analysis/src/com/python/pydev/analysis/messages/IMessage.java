/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.messages;

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
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNUSED_IMPORT
     * @see com.python.pydev.analysis.IAnalysisPreferences#TYPE_UNUSED_VARIABLE
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
     * @return the message that should be presented to the user in a short way (may be used for abbreviations).
     */
    Object getShortMessage();
    
    /**
     * @return the generator token for the message
     */
    IToken getGenerator();
}
