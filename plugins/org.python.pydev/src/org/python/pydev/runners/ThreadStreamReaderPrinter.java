/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/08/2005
 */
package org.python.pydev.runners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.PydevPlugin;

public class ThreadStreamReaderPrinter extends Thread {
    private static final boolean DEBUG = false;
    InputStream is;
    FastStringBuffer contents;

    
    public ThreadStreamReaderPrinter(InputStream is) {
        contents = new FastStringBuffer();
        setName("ThreadStreamReaderPrinter");
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            int c;
            while ((c = in.read()) != -1) {
                if(DEBUG){
                    contents.append((char) c);
                }
            }
            if(DEBUG){
                System.out.print(contents);
                contents = new FastStringBuffer();
            }
        } catch (IOException ioe) {
            PydevPlugin.log(ioe);
        }
    }
}
