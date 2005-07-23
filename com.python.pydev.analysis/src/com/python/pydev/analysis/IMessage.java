/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public interface IMessage {
    int TYPE_ERROR = 1;
    int TYPE_WARNING = 2;
    
    /**
     * @return this message type.
     */
    int getType();
    
    
    
    
    int SUB_UNUSED_IMPORT = 0;
    int SUB_UNUSED_VARIABLE = 1;
    /**
     * @return this message sub type
     */
    int getSubType();

    /**
     * @return the starting line of the error
     */
    int getStartLine();
    
    /**
     * @return the starting col of the error
     */
    int getStartCol();

    /**
     * @return the ending line of the error. may be -1 if we are unable to find the end of the token
     */
    int getEndLine();

    /**
     * @return the ending col of the error. may be -1 if we are unable to find the end of the token
     */
    int getEndCol();
    
    /**
     * @return the message that should be presented to the user.
     */
    String getMessage();
    
    /**
     * @return the message that should be presented to the user in a short way (may be used for abbreviations).
     */
    String getShortMessage();
    
    /**
     * @return the generator token for the message
     */
    SourceToken getGenerator();
}
