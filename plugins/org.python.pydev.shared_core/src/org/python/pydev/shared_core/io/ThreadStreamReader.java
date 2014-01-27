/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/08/2005
 */
package org.python.pydev.shared_core.io;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

public final class ThreadStreamReader extends Thread {

    /**
     * Input stream read.
     */
    private final InputStream is;

    /**
     * Buffer with the contents gotten.
     */
    private final FastStringBuffer contents;

    /**
     * Access to the buffer should be synchronized.
     */
    private final Object lock = new Object();

    /**
     * Whether the read should be synchronized.
     */
    private final boolean synchronize;

    /**
     * Keeps the next unique identifier.
     */
    private static int next = 0;

    /**
     * Get a unique identifier for this thread. 
     */
    private static synchronized int next() {
        next++;
        return next;
    }

    private final String encoding;

    private boolean stopGettingOutput = false;

    public ThreadStreamReader(InputStream is) {
        this(is, true); //default is synchronize.
    }

    public ThreadStreamReader(InputStream is, boolean synchronize) {
        this(is, synchronize, null);
    }

    public ThreadStreamReader(InputStream is, boolean synchronize, String encoding) {
        this.setName("ThreadStreamReader: " + next());
        this.setDaemon(true);
        this.encoding = encoding;
        contents = new FastStringBuffer();
        this.is = is;
        this.synchronize = synchronize;
    }

    @Override
    public void run() {
        try {
            InputStreamReader in;
            if (encoding != null) {
                in = new InputStreamReader(is, encoding);

            } else {
                in = new InputStreamReader(is);
            }
            int c;

            //small buffer because we may want to see contents as it's being written.
            //(still better than char by char).
            char[] buf = new char[80];

            if (synchronize) {
                while ((c = in.read(buf)) != -1 && !stopGettingOutput) {
                    synchronized (lock) {
                        contents.append(buf, 0, c);
                    }
                }
            } else {
                while ((c = in.read(buf)) != -1 && !stopGettingOutput) {
                    contents.append(buf, 0, c);
                }
            }
        } catch (Exception e) {
            //that's ok
        }
    }

    /**
     * @return the contents that were obtained from this instance since it was started or since
     * the last call to this method.
     */
    public String getAndClearContents() {
        synchronized (lock) {
            String string = contents.toString();
            contents.clear();
            return string;
        }
    }

    public String getContents() {
        synchronized (lock) {
            return contents.toString();
        }
    }

    public void stopGettingOutput() {
        try {
            synchronized (lock) {
                this.stopGettingOutput = true;
                this.interrupt();
                contents.clear();
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void clearContents() {
        synchronized (lock) {
            contents.clear();
        }
    }
}
