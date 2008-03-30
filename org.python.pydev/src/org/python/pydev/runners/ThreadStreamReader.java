/*
 * Created on 03/08/2005
 */
package org.python.pydev.runners;

import java.io.InputStream;
import java.io.InputStreamReader;

public class ThreadStreamReader extends Thread {
    InputStream is;
    public StringBuffer contents;

    public ThreadStreamReader(InputStream is) {
    	this.setName("ThreadStreamReader");
    	this.setDaemon(true);
        contents = new StringBuffer();
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader in = new InputStreamReader(is);
            int c;
            while ((c = in.read()) != -1) {
                contents.append((char) c);
            }
        } catch (Exception e) {
            //that's ok
            e.printStackTrace();
        }
    }

    /**
     * @return the contents that were obtained from this instance since it was started or since
     * the last call to this method.
     */
    public String getAndClearContents() {
        StringBuffer oldContents = contents;
        contents = new StringBuffer();
        return oldContents.toString();
    }
}
