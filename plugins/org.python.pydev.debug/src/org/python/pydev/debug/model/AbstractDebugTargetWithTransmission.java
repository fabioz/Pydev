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
import org.eclipse.ui.console.IOConsole;
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

    private volatile boolean waitingForInput = false;

    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    public void setWaitingForInput(boolean waitingForInput) {
        this.waitingForInput = waitingForInput;
    }

    protected void addProcessConsole(IOConsole c) {
        // What we'd like to do is not put in the input stream the contents we received
        // in the console UNLESS we're waiting for input (but unfortunately, it seems there's
        // no API for that).
        //
        // This means that if the user writes something (to do an evaluation) and later
        // does a raw_input('say something:\n'), the raw_input will get the contents that
        // the user wrote for the evaluation and not the contents it'd write now.
        // As we now have a separate input console, this shouldn't be so troublesome, as
        // we control things better when the user writes to the PromptOverlay console, but
        // if he writes to the other console, things may misbehave.
    }

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
