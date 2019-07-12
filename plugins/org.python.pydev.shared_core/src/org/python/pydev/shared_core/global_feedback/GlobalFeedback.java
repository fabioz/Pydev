package org.python.pydev.shared_core.global_feedback;

import java.io.IOException;

import org.python.pydev.shared_core.callbacks.ListenerList;

/**
 * Provides a way for clients to be independently notified of the slowest operations in PyDev.
 *
 * @author Fabio Zadrozny
 */
public class GlobalFeedback {

    public final static ListenerList<IGlobalFeedbackListener> GLOBAL_LISTENERS = new ListenerList<>(
            IGlobalFeedbackListener.class);

    public static class GlobalFeedbackReporter implements AutoCloseable {

        private long lastReport;
        private IGlobalFeedbackListener[] listeners;

        public GlobalFeedbackReporter(String message) {
            this.lastReport = System.currentTimeMillis();
            this.listeners = GLOBAL_LISTENERS.getListeners();
            for (IGlobalFeedbackListener listener : listeners) {
                listener.start(message);
            }
        }

        @Override
        public void close() throws IOException {
            for (IGlobalFeedbackListener listener : listeners) {
                listener.end();
            }
        }

        public void progress(String name) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > lastReport + 200) {
                lastReport = currentTimeMillis;
                for (IGlobalFeedbackListener listener : listeners) {
                    listener.progress(name);
                }
            }
        }
    }

    public static GlobalFeedbackReporter start(String message) {
        return new GlobalFeedbackReporter(message);
    }

}
