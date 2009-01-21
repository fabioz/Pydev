/*
 * Created on 03/08/2005
 */
package org.python.pydev.runners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.python.pydev.core.structure.FastStringBuffer;

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
            ioe.printStackTrace();
        }
    }
}
