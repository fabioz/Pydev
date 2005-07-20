/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

public interface IMessage {
    int ERROR = 1;
    int WARNING = 2;
    
    /**
     * @return this message type.
     */
    int getType();
    
    
    int UNUSED_IMPORT = 0;
    /**
     * @return this message sub type
     */
    int getSubType();
    
    /**
     * @return the message that should be presented to the user.
     */
    String getMessage();
}
