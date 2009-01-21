package org.python.pydev.ui;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.plugin.PydevPlugin;


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
    public void run() {
        try {
            String line;
            while (null != (line = bReader.readLine())) {
                lastLine = line;
                // DebugPlugin.log(line);
            }
            bReader.close();
        } catch (IOException ioe) {
            PydevPlugin.log(IStatus.ERROR, "Error in stream consumer", ioe);
        }
    }
    /**
     * @return last line obtained, can be null
     */
    public String getLastLine() {
        return lastLine;
    }

}