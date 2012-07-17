/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

public interface ILogPing {

    /**
     * Whether to force the log in development mode
     */
    boolean FORCE_SEND_WHEN_IN_DEV_MODE = false;

    void addPingOpenEditor();

    void addPingStartPlugin();

    /**
     * Sends the contents to the ping server. If all went ok, clears the memory and disk-contents.
     */
    void send();

    /**
     * Clears in-memory contents and flushes buffered contents to file
     */
    void stop();

}