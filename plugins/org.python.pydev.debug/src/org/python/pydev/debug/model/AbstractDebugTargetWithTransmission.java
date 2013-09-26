/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.core.runtime.PlatformObject;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.DebuggerReader;
import org.python.pydev.debug.model.remote.DebuggerWriter;

public class AbstractDebugTargetWithTransmission extends PlatformObject {

    /**
     * connection socket
     */
    protected Socket socket;

    /**
     * reading thread
     */
    protected DebuggerReader reader;

    /**
     * writing thread
     */
    protected DebuggerWriter writer;

    /**
     * sequence seed for command numbers
     */
    protected int sequence = -1;

    /**
     * @return next available debugger command sequence number
     */
    public int getNextSequence() {
        sequence += 2;
        return sequence;
    }

    public void addToResponseQueue(AbstractDebuggerCommand cmd) {
        if (reader != null) {
            reader.addToResponseQueue(cmd);
        }
    }

    public void postCommand(AbstractDebuggerCommand cmd) {
        if (writer != null) {
            writer.postCommand(cmd);
        }
    }

    public void startTransmission(Socket socket2) throws IOException {
        this.socket = socket2;
        //socket = connector.getSocket();
        this.reader = new DebuggerReader(socket, this);
        this.writer = new DebuggerWriter(socket);
        Thread t = new Thread(reader, "pydevd.reader");
        t.start();
        t = new Thread(writer, "pydevd.writer");
        t.start();
    }

}
