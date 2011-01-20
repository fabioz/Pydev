/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 4, 2004
 */
package org.python.pydev.debug.model.remote;

/**
 * All commands are executed asynchronously.
 * 
 * This interface, if specified, is called when command completes.
 */
public interface ICommandResponseListener {
    public void commandComplete(AbstractDebuggerCommand cmd);
}
