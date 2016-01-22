/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

/**
 * This is a marker interface to identify hover participants for which specialized behavior is implemented.
 * 
 * For example, hover participants that implement this interface will provide hover info for Strings and comments.
 * 
 */
public interface IPyHoverParticipant2 extends IPyHoverParticipant {

    /**
     * Checks if the specified content type is supported
     * 
     * @param contentType the content type
     * @return true if the specified content type is supported
     */
    public boolean isContentTypeSupported(String contentType);

}
