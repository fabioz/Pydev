package org.python.pydev.shared_interactive_console.console.ui.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

/*
 * StreamReader spawns two threads to continually call the blocking readline
 * on both the stdout and stderr streams coming from the child process.
 * These streams are serialized into a single buffer which can then be consumed
 * in a single process. 
 * 
 * Possible ideas: generalise this to merge any number of streams of any number
 * of types to serialise events.
 */
public class StreamReader implements Runnable {

    private class ThreadedStreamReader extends Thread {
        private final StreamType type;
        private final BufferedReader rdr;
        private final BlockingQueue<StreamMessage> q;

        ThreadedStreamReader(StreamType type, InputStream stream, BlockingQueue<StreamMessage> q) {
            this.type = type;
            this.rdr = new BufferedReader(new InputStreamReader(stream));
            this.q = q;
        }

        @Override
        public void run() {
            /* This line separator can in theory be document dependent and hence
             * should use TextUtilities.getDefaultLineDelimiter(doc); 
             * however for our purposes, the console is on the local system.
             */
            String endl = System.getProperty("line.separator");
            while (true) {
                try {
                    StreamMessage m = new StreamMessage(type, "");
                    m.message = rdr.readLine();

                    if (m.message != null) {
                        m.message += endl;
                    }

                    q.put(m);

                    if (m.message == null) {
                        return;
                    }

                } catch (IOException e) {
                    return;
                } catch (InterruptedException e) {
                    return;
                }

            }
        }
    }

    private final ThreadedStreamReader out;
    private final ThreadedStreamReader err;

    public StreamReader(InputStream out, InputStream err, BlockingQueue<StreamMessage> q) {
        this.out = new ThreadedStreamReader(StreamType.STDOUT, out, q);
        this.err = new ThreadedStreamReader(StreamType.STDERR, err, q);
    }

    public void run() {
        out.start();
        err.start();
    }
}
