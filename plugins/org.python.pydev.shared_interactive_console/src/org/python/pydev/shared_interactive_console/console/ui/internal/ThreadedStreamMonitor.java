package org.python.pydev.shared_interactive_console.console.ui.internal;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugPlugin;

public class ThreadedStreamMonitor extends Thread implements IStreamMonitor {
    private final BlockingQueue<StreamMessage> q;
    private ListenerList listeners = new ListenerList();

    public ThreadedStreamMonitor(BlockingQueue<StreamMessage> q) {
        this.q = q;
    }

    public void addListener(IStreamListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IStreamListener listener) {
        listeners.remove(listener);
    }

    public void run() {
        ContentNotifier notifier = new ContentNotifier();

        while (true) {
            StreamMessage stdout = new StreamMessage(StreamType.STDOUT, "");
            StreamMessage stderr = new StreamMessage(StreamType.STDERR, "");

            ArrayList<StreamMessage> msgs = new ArrayList<StreamMessage>();

            try {
                // drainTo isn't blocking, so we take first.
                msgs.add(q.take());
                q.drainTo(msgs);
                StringBuilder stdoutBuilder = new StringBuilder();
                StringBuilder stderrBuilder = new StringBuilder();

                for (StreamMessage msg : msgs ) {
                    if (msg.message == null) {
                        break;
                    }

                    if (msg.type == StreamType.STDOUT) {
                        stdoutBuilder.append(msg.message);
                    } else {
                        stderrBuilder.append(msg.message);
                    }
                }

                stdout.message = stdoutBuilder.toString();
                if (stdout.message.length() > 0) {
                    notifier.onStream(stdout);
                }

                stderr.message = stderrBuilder.toString();
                if (stderr.message.length() > 0) {
                    notifier.onStream(stderr);
                }

                if (msgs.get(msgs.size()-1) == null) 
                    return;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ContentNotifier implements ISafeRunnable {

        private IStreamListener listener;
        private StreamMessage msg;

        public void handleException(Throwable exception) {
            DebugPlugin.log(exception);
        }

        public void run() throws Exception {
            listener.onStream(msg);
        }

        public void onStream(StreamMessage msg) {
            this.msg = msg;
            Object[] copiedListeners = listeners.getListeners();
            for (int i = 0; i < copiedListeners.length; i++) {
                listener = (IStreamListener) copiedListeners[i];
                SafeRunner.run(this);
            }
            this.listener = null;
            this.msg = null;
        }
    }
}
