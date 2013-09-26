/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.core;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * This interface can be used to listen to what the user writes in the console when debugging.
 *
 */
public interface IConsoleInputListener {

    /**
     * This method is called whenever a new line is written in the console while debugging.
     * 
     * @param lineReceived this is the line that was written.
     * @param target this is the target of the debug
     */
    void newLineReceived(String lineReceived, AbstractDebugTarget target);

    /**
     * This method is called when there is a paste action in the console.
     * 
     * @param text this is the text that was pasted
     * @param target this is the target of the debug
     */
    void pasteReceived(String text, AbstractDebugTarget target);

}
