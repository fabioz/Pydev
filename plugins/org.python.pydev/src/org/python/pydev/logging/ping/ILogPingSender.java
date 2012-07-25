/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.logging.ping;

public interface ILogPingSender {

    /**
     * @param pingString the string that should be posted.
     * @return true if it was properly sent and false otherwise.
     */
    public boolean sendPing(String pingString);
}
