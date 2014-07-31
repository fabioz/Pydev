package org.python.pydev.core.concurrency;

import org.python.pydev.shared_core.callbacks.ICallback0;

public class ConditionEvent {

    private final Object lock = new Object();
    private boolean set = false;
    private final ICallback0<Boolean> markSetIfConditionReturnedTrue;
    private final long timeout;

    /**
     *
     * @param timeout only used if markSetIfConditionReturnedTrue != null
     */
    public ConditionEvent(ICallback0<Boolean> markSetIfConditionReturnedTrue, long timeout) {
        this.markSetIfConditionReturnedTrue = markSetIfConditionReturnedTrue;
        this.timeout = timeout;
    }

    public void set() {
        synchronized (lock) {
            set = true;
            lock.notifyAll();
        }
    }

    public void unset() {
        synchronized (lock) {
            set = false;
        }
    }

    public void waitForSet() {
        if (markSetIfConditionReturnedTrue != null) {

            synchronized (lock) {
                while (!set) {
                    try {
                        lock.wait(timeout);
                    } catch (InterruptedException e) {
                        //ok
                    }
                    if (this.markSetIfConditionReturnedTrue.call()) {
                        set = true;
                    }
                }
            }
        } else {
            synchronized (lock) {
                while (!set) {
                    try {
                        lock.wait(); //no timeout
                    } catch (InterruptedException e) {
                        //ok
                    }
                }
            }
        }
    }

}
