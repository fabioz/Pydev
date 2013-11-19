package org.python.pydev.shared_core.utils;

public class ThreadPriorityHelper {

    private Thread thisThread;
    private int initialPriority;

    public ThreadPriorityHelper(Thread thread) {
        thisThread = thread;
        if (thread != null) {
            initialPriority = thread.getPriority();
        }
    }

    public void setMinPriority() {
        if (thisThread != null) {
            thisThread.setPriority(Thread.MIN_PRIORITY);
        }
    }

    public void restoreInitialPriority() {
        if (thisThread != null) {
            thisThread.setPriority(initialPriority);
        }
    }
}
