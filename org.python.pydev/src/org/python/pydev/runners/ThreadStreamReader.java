/*
 * Created on 03/08/2005
 */
package org.python.pydev.runners;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ThreadStreamReader extends Thread {
    InputStream is;
    public StringBuffer contents;

    public ThreadStreamReader(InputStream is) {
        contents = new StringBuffer();
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader in = new BufferedReader(isr);
            int c;
            while ((c = in.read()) != -1) {
                contents.append((char) c);
            }
        } catch (Exception e) {
            //that's ok
            e.printStackTrace();
        }
    }
}
