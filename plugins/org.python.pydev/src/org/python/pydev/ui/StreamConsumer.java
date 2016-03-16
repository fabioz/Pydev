/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;


/**
 * Used to receive output from processes.
 * 
 * Copied from Ant code, and used by InterpreterEditor
 */
public class StreamConsumer extends Thread {
    BufferedReader bReader;
    private String lastLine;

    public StreamConsumer(InputStream inputStream) {
        super();
        setName("StreamConsumer");
        setDaemon(true);
        bReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run() {
        try {
            String line;
            while (null != (line = bReader.readLine())) {
                lastLine = line;
                // DebugPlugin.log(line);
            }
            bReader.close();
        } catch (IOException ioe) {
            Log.log(IStatus.ERROR, "Error in stream consumer", ioe);
        }
    }

    /**
     * @return last line obtained, can be null
     */
    public String getLastLine() {
        return lastLine;
    }

}