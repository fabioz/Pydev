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

import java.io.IOException;
import java.io.InputStream;

import org.python.pydev.shared_core.log.Log;

/**
 * A class mostly for debugging purposes. Prints whatever comes out of the passed input stream.
 * 
 * @note: things are printed char-by-char and totally unbuffered (so, things may become slower,
 * but should not be a problem only for debugging purposes)
 */
public class ThreadStreamReaderPrinter extends Thread {

    private InputStream is;

    public ThreadStreamReaderPrinter(InputStream is) {
        setName("ThreadStreamReaderPrinter");
        this.is = is;
    }

    @Override
    public void run() {
        try {
            int c;
            while ((c = is.read()) != -1) {
                System.out.print((char) c);
            }
        } catch (IOException ioe) {
            Log.log(ioe);
        }
    }
}
